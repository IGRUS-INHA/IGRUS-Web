import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as route53targets from 'aws-cdk-lib/aws-route53-targets';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';

const SSL_POLICY = 'ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09' as elbv2.SslPolicy;

/**
 * EC2+Caddy 전환 단계 (근거: docs/infra/ec2-caddy-migration-rationale.md,
 * 절차: docs/infra/ec2-migration-runbook.md)
 *
 * 1 — 병행 프로비저닝: 기존 Fargate+ALB 유지 + EC2(t3.small)+Caddy 추가.
 *     ec2.igrus.co.kr 로 EC2 경로 검증. api.igrus.co.kr 은 아직 ALB.
 * 2 — cutover: api.igrus.co.kr A레코드 → EC2 EIP, prod Fargate desiredCount 0.
 *     ALB/Fargate 는 롤백 경로로 유지 (플래그를 1로 되돌리면 원복).
 * 2.5 — cleanup: 미사용 레거시 제거 — ALB(+IPv4 4개)/ECS/bastion/staging RDS(스냅샷 보존).
 *       라이브 경로(EC2→prod RDS) 무변경 = 무중단.
 * 3 — prod RDS t3.micro → t4g.micro (ARM −10%, 짧은 재부팅 다운타임 수반).
 */
const MIGRATION_PHASE: 1 | 2 | 2.5 | 3 = 2.5;
const CUTOVER = MIGRATION_PHASE >= 2; // api 도메인이 EC2 를 향하고 Fargate 는 중지
const LEGACY = MIGRATION_PHASE <= 2; // Fargate/ALB/bastion/staging 자원 유지 여부

const ECR_IMAGE_REPO = '218736972976.dkr.ecr.ap-northeast-2.amazonaws.com/igrus/web/spring';
/** EC2 최초 부팅 시 기동할 이미지 태그 (이후 배포는 CD 가 igrus-deploy 로 갱신) */
const INITIAL_IMAGE_TAG = 'v1.1.8';

/** play-igrus Go 백엔드 (play-api.igrus.co.kr) — 같은 EC2 에 컨테이너 추가 */
const PLAY_ECR_IMAGE_REPO = '218736972976.dkr.ecr.ap-northeast-2.amazonaws.com/igrus/play/server';
const PLAY_INITIAL_IMAGE_TAG = 'play-v0.1.0';

/** 한 앱 환경(prod/staging)의 ECS 구성 설정 */
interface AppEnvConfig {
  idPrefix: string;
  /** 기존 Secrets Manager 시크릿 (그대로 재사용) */
  appSecret: secretsmanager.ISecret;
  springProfile: string;
  serviceName: string;
  family: string;
  containerName: string;
  image: string;
  logGroupName: string;
  attachDefaultSgToService: boolean; // prod=true, staging=false (운영 현황 그대로)
  desiredCount: number;
  rdsDbName: string; // SPRING_DATASOURCE_URL 구성용
  db: rds.IDatabaseInstance;
}

/** RDS 인스턴스 구성 설정 (기존 DB 스냅샷에서 복원 → 데이터 그대로 복제) */
interface DbConfig {
  idPrefix: string;
  rdsId: string;
  rdsVersion: rds.MysqlEngineVersion;
  rdsSnapshotIdentifier: string;
  rdsPubliclyAccessible: boolean;
  rdsBackupDays: number;
  instanceType: ec2.InstanceType;
}

interface SharedCtx {
  vpc: ec2.IVpc;
  cluster: ecs.ICluster;
  ecsSg: ec2.ISecurityGroup;
  defaultSg: ec2.ISecurityGroup;
  taskRole: iam.IRole;
  executionRole: iam.IRole;
}

/**
 * IGRUS-Web 인프라 스택.
 *
 * - v2: 운영 인프라를 "토씨 하나 같게" 복제한 Fargate+ALB 구조 (MIGRATION_PHASE<=2 유지)
 * - v3(EC2+Caddy): 단일 t3.small 에 Docker(동일 ECR 이미지)+Caddy(자동 TLS) —
 *   상시 저부하 워크로드에 맞춰 Fargate/ALB/IPv4 고정비 제거 (월 ~$175 → ~$47)
 *
 * account 218736972976 / ap-northeast-2 / default VPC.
 */
export class IgrusWebV2Stack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);

    // ── 기존 default VPC / default SG / Route53 존 참조 ──
    const vpc = ec2.Vpc.fromLookup(this, 'DefaultVpc', { isDefault: true });
    const defaultSg = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'DefaultSg',
      'sg-05c163b511ab48477',
      { mutable: false },
    );
    const zone = route53.HostedZone.fromLookup(this, 'Zone', { domainName: 'igrus.co.kr' });

    // ── RDS 보안그룹 (이름/규칙 운영과 동일, 이름만 -v2) ──
    const rdsSg = new ec2.SecurityGroup(this, 'RdsSg', {
      vpc,
      securityGroupName: 'IGRUS-Web-MySQL-RDS-SG-v2',
      description: 'IGRUS Web MySQL RDS Security Group',
      allowAllOutbound: true,
    });
    rdsSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(3306));

    // ── prod 시크릿 + prod RDS (아키텍처와 무관하게 상시 유지) ──
    const prodSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'ProdAppSecret',
      'igrus/web/server/prod',
    );
    const prodDb = this.addDatabase(
      {
        idPrefix: 'Prod',
        rdsId: 'igrus-web-mysql-rds-v2',
        rdsVersion: rds.MysqlEngineVersion.of('8.0.44', '8.0'),
        rdsSnapshotIdentifier: 'igrus-web-mysql-rds-v2seed-20260629',
        rdsPubliclyAccessible: false,
        rdsBackupDays: 3,
        // cleanup 단계에서 x86(t3) → ARM(t4g) 전환: 동급 스펙 −10%, 짧은 재부팅 1회
        instanceType:
          MIGRATION_PHASE >= 3
            ? ec2.InstanceType.of(ec2.InstanceClass.T4G, ec2.InstanceSize.MICRO)
            : ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      },
      vpc,
      [rdsSg, defaultSg],
    );

    // ══════════════════════════════════════════════════════════════════
    // EC2 + Caddy 앱 호스트 (v3 아키텍처 본체)
    // ══════════════════════════════════════════════════════════════════
    const appSg = new ec2.SecurityGroup(this, 'AppEc2Sg', {
      vpc,
      securityGroupName: 'IGRUS-Web-App-EC2-SG',
      description: 'IGRUS Web App EC2 (Caddy 80/443)',
      allowAllOutbound: true,
    });
    [80, 443].forEach((p) => appSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(p)));
    rdsSg.addIngressRule(appSg, ec2.Port.tcp(3306));

    // 앱 컨테이너가 인스턴스 프로파일로 Secrets Manager / S3 접근 (기존 taskRole 과 동등 권한)
    // + SSM(원격 관리·배포·RDS 터널 = bastion 대체) + ECR pull
    const appRole = new iam.Role(this, 'AppEc2Role', {
      roleName: 'IGRUS-Web-App-EC2-ROLE',
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEC2ContainerRegistryReadOnly'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess'),
      ],
    });
    prodSecret.grantRead(appRole);

    // ── play-igrus 시크릿 (DB DSN — 값 생성은 수동, 런북 참조) ──
    // ECR repo(igrus/play/server)는 spring repo 와 동일하게 CDK 밖에서 생성·URL 참조 (런북 참조)
    const playSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'PlayAppSecret',
      'igrus/play/server/prod',
    );
    playSecret.grantRead(appRole);

    const userData = ec2.UserData.forLinux();
    userData.addCommands(
      `set -euxo pipefail

# ── swap 5GB — RAM 2GB 하드 OOM 방지 안전망 (rationale §4, 상시 의존 아님) ──
fallocate -l 5G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
sysctl -w vm.swappiness=10
echo 'vm.swappiness=10' >> /etc/sysctl.conf

# ── Docker + compose v2 + ECR credential helper (인스턴스 롤로 무기한 pull) ──
dnf install -y docker amazon-ecr-credential-helper
systemctl enable --now docker
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64 \\
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

mkdir -p /opt/igrus/.docker
cat > /opt/igrus/.docker/config.json <<'DOCKERCFG'
{ "credsStore": "ecr-login" }
DOCKERCFG

# ── Caddy: 자동 Let's Encrypt TLS + 리버스 프록시 (ALB+ACM 대체) ──
# api.igrus.co.kr 인증서는 DNS cutover 전까지 발급 실패(백오프 재시도)가 정상.
# cutover 직후 'docker compose restart caddy' 로 즉시 재발급 (런북 참조).
cat > /opt/igrus/Caddyfile <<'CADDYFILE'
api.igrus.co.kr, ec2.igrus.co.kr {
	encode gzip
	reverse_proxy app:8080
}

play.igrus.co.kr {
	encode gzip
	reverse_proxy play-api:8080
}
CADDYFILE

cat > /opt/igrus/.env <<'ENVFILE'
IMAGE_TAG=${INITIAL_IMAGE_TAG}
PLAY_IMAGE_TAG=${PLAY_INITIAL_IMAGE_TAG}
ENVFILE

cat > /opt/igrus/docker-compose.yml <<'COMPOSE'
services:
  caddy:
    image: caddy:2.10
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /opt/igrus/Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    logging:
      driver: json-file
      options: { max-size: "10m", max-file: "3" }
  app:
    image: ${ECR_IMAGE_REPO}:\${IMAGE_TAG}
    restart: unless-stopped
    environment:
      SPRING_ACTIVE_PROFILE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://${prodDb.dbInstanceEndpointAddress}:3306/igrus_web
      JAVA_TOOL_OPTIONS: -Xmx1g
    logging:
      driver: json-file
      options: { max-size: "10m", max-file: "3" }
  play-api:
    image: ${PLAY_ECR_IMAGE_REPO}:\${PLAY_IMAGE_TAG}
    restart: unless-stopped
    environment:
      # 토큰 introspection — 같은 compose 네트워크의 app 컨테이너 직결 (TLS 왕복 절약)
      IGRUS_API: http://app:8080
      PLAY_SECRET_NAME: igrus/play/server/prod
      S3_BUCKET: igrus-web-file-storage-bucket-v2
      AWS_REGION: ap-northeast-2
    logging:
      driver: json-file
      options: { max-size: "10m", max-file: "3" }
volumes:
  caddy_data:
  caddy_config:
COMPOSE

# ── 배포 스크립트 (GitHub Actions 가 SSM RunCommand 로 호출) ──
cat > /usr/local/bin/igrus-deploy <<'DEPLOY'
#!/bin/bash
set -euo pipefail
TAG="\${1:?usage: igrus-deploy <image-tag> [service=app]}"
SVC="\${2:-app}"
export DOCKER_CONFIG=/opt/igrus/.docker
cd /opt/igrus
case "\$SVC" in
  app)      sed -i "s|^IMAGE_TAG=.*|IMAGE_TAG=\${TAG}|" .env ;;
  play-api) sed -i "s|^PLAY_IMAGE_TAG=.*|PLAY_IMAGE_TAG=\${TAG}|" .env ;;
  *) echo "unknown service: \$SVC" >&2; exit 1 ;;
esac
docker compose pull "\$SVC"
docker compose up -d "\$SVC"
docker image prune -f
DEPLOY
chmod +x /usr/local/bin/igrus-deploy

export DOCKER_CONFIG=/opt/igrus/.docker
cd /opt/igrus
docker compose up -d`,
    );

    const appInstance = new ec2.Instance(this, 'AppEc2', {
      instanceName: 'IGRUS-Web-App-EC2',
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({ cachedInContext: true }),
      securityGroup: appSg,
      role: appRole,
      userData,
      // user-data 는 부팅 1회 실행 → 수정 시 인스턴스 교체로 재현 (EIP/DNS 는 유지됨)
      userDataCausesReplacement: true,
      blockDevices: [
        {
          deviceName: '/dev/xvda',
          // 30GB: OS ~3 + swap 5 + 이미지/로그 누적 헤드룸 (디스크 full 장애 방지)
          volume: ec2.BlockDeviceVolume.ebs(30, {
            volumeType: ec2.EbsDeviceVolumeType.GP3,
            encrypted: true,
          }),
        },
      ],
    });

    // 컨테이너(브리지 네트워크 = 홉 +1)가 IMDSv2 로 인스턴스 롤 자격증명을 읽도록 hop limit 2
    const imdsLt = new ec2.CfnLaunchTemplate(this, 'AppEc2ImdsLt', {
      launchTemplateData: {
        metadataOptions: { httpTokens: 'required', httpPutResponseHopLimit: 2 },
      },
    });
    const cfnAppInstance = appInstance.node.defaultChild as ec2.CfnInstance;
    cfnAppInstance.launchTemplate = {
      launchTemplateId: imdsLt.ref,
      version: imdsLt.attrLatestVersionNumber,
    };

    // 퍼블릭 IPv4 5~6개 → EIP 1개 (인스턴스 교체 시에도 IP 불변)
    const appEip = new ec2.CfnEIP(this, 'AppEip', {
      domain: 'vpc',
      tags: [{ key: 'Name', value: 'IGRUS-Web-App-EIP' }],
    });
    new ec2.CfnEIPAssociation(this, 'AppEipAssoc', {
      allocationId: appEip.attrAllocationId,
      instanceId: appInstance.instanceId,
    });

    // 검증용 도메인: cutover 전에 EC2 경로(Caddy TLS + 앱 + RDS)를 실도메인으로 점검
    new route53.ARecord(this, 'AppEc2Alias', {
      zone,
      recordName: 'ec2',
      target: route53.RecordTarget.fromIpAddresses(appEip.ref),
      ttl: cdk.Duration.seconds(60),
    });

    if (CUTOVER) {
      // cutover: 실운영 도메인을 ALB alias → EC2 EIP 로 전환 (TTL 짧게 = 빠른 롤백)
      new route53.ARecord(this, 'ApiAlias', {
        zone,
        recordName: 'api',
        target: route53.RecordTarget.fromIpAddresses(appEip.ref),
        ttl: cdk.Duration.seconds(60),
      });
    }

    // play.igrus.co.kr → 같은 EC2 (Go 컨테이너가 SPA+API 를 같은 origin 으로 서빙).
    // igrus.co.kr 서브도메인이라 www 와 refresh 쿠키(Domain=igrus.co.kr) 공유 — SSO 전제.
    new route53.ARecord(this, 'PlayAlias', {
      zone,
      recordName: 'play',
      target: route53.RecordTarget.fromIpAddresses(appEip.ref),
      ttl: cdk.Duration.seconds(60),
    });

    // ══════════════════════════════════════════════════════════════════
    // 레거시 (v2 Fargate + ALB) — cleanup(phase 3) 전까지 롤백 경로로 유지
    // ══════════════════════════════════════════════════════════════════
    if (LEGACY) {
      // ── 보안그룹 (이름/규칙 운영과 동일, 이름만 -v2) ──
      const albSg = new ec2.SecurityGroup(this, 'AlbSg', {
        vpc,
        securityGroupName: 'IGRUS-WEB-ALB-SG-v2',
        description: 'IGRUS-WEB-ALB-SG',
        allowAllOutbound: true,
      });
      [80, 443, 8080].forEach((p) => albSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(p)));

      const ecsSg = new ec2.SecurityGroup(this, 'EcsSg', {
        vpc,
        securityGroupName: 'igrus-web-server-ecs-service-sg-v2',
        description: 'igrus-web-server-ecs-service-sg',
        allowAllOutbound: true,
      });
      ecsSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8080));

      const bastionSg = new ec2.SecurityGroup(this, 'BastionSg', {
        vpc,
        securityGroupName: 'launch-wizard-1-v2',
        description: 'launch-wizard-1',
        allowAllOutbound: true,
      });
      bastionSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22));

      rdsSg.addIngressRule(ecsSg, ec2.Port.tcp(3306));
      rdsSg.addIngressRule(bastionSg, ec2.Port.tcp(3306));

      // ── IAM 역할 (운영 prod 역할을 prod/staging 공용 사용, 이름만 -v2) ──
      const taskRole = new iam.Role(this, 'TaskRole', {
        roleName: 'igrus-web-server-prod-ecs-task-role-v2',
        assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess'),
          iam.ManagedPolicy.fromAwsManagedPolicyName('AWSSecretsManagerClientReadOnlyAccess'),
        ],
      });
      const executionRole = new iam.Role(this, 'ExecutionRole', {
        roleName: 'igrus-web-server-prod-ecs-task-execution-role-v2',
        assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName(
            'service-role/AmazonECSTaskExecutionRolePolicy',
          ),
          iam.ManagedPolicy.fromAwsManagedPolicyName('AWSSecretsManagerClientReadOnlyAccess'),
        ],
      });

      // ── ECS 클러스터 (FARGATE / FARGATE_SPOT) ──
      const cluster = new ecs.Cluster(this, 'Cluster', {
        clusterName: 'IGRUS-WEB-ECS-Cluster-v2',
        vpc,
        enableFargateCapacityProviders: true,
      });

      const ctx: SharedCtx = { vpc, cluster, ecsSg, defaultSg, taskRole, executionRole };

      // ── 운영(prod) 앱 환경 ──
      const prodService = this.addEcsService(
        {
          idPrefix: 'Prod',
          appSecret: prodSecret,
          springProfile: 'prod',
          serviceName: 'igrus-web-server-ecs-service-v2',
          family: 'igrus-web-server-task-def-v2',
          containerName: 'IGRUS-WEB-SPRING-SERVER',
          image: `${ECR_IMAGE_REPO}:${INITIAL_IMAGE_TAG}`,
          logGroupName: '/ecs/igrus-web-server-task-def-v2',
          attachDefaultSgToService: true,
          desiredCount: CUTOVER ? 0 : 1, // cutover 후 중지 (롤백 시 1로 원복)
          rdsDbName: 'igrus_web',
          db: prodDb,
        },
        ctx,
      );

      // ── 스테이징(staging) 앱 환경 ──
      const stagingSecret = secretsmanager.Secret.fromSecretNameV2(
        this,
        'StagingAppSecret',
        'igrus/web/server/staging',
      );
      const stagingDb = this.addDatabase(
        {
          idPrefix: 'Staging',
          rdsId: 'igrus-web-staging-mysql-rds-v2',
          rdsVersion: rds.MysqlEngineVersion.of('8.4.7', '8.4'),
          rdsSnapshotIdentifier: 'igrus-web-staging-mysql-rds-v2seed-20260629',
          rdsPubliclyAccessible: false, // 실 PII 복원이라 private
          rdsBackupDays: 1,
          instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
        },
        vpc,
        [rdsSg, defaultSg],
      );
      const stagingService = this.addEcsService(
        {
          idPrefix: 'Staging',
          appSecret: stagingSecret,
          springProfile: 'staging',
          serviceName: 'igrus-web-server-staging-task-def-service-v2',
          family: 'igrus-web-server-staging-task-def-v2',
          containerName: 'IGRUS-WEB-SPRING-STAGING-SERVER',
          image:
            '218736972976.dkr.ecr.ap-northeast-2.amazonaws.com/igrus/web/staging/spring:6b34009e2deac8c65c6d31f4d62c34a07343a8f7',
          logGroupName: '/ecs/igrus-web-server-staging-task-def-v2',
          attachDefaultSgToService: false,
          desiredCount: 0, // staging 중지(비용 절감). 필요 시 1로 올려 재배포
          rdsDbName: 'igrus_web_staging',
          db: stagingDb,
        },
        ctx,
      );

      // ── ALB-v2 + 인증서 + 3 리스너 (운영 ALB 와 동일 구조) ──
      const alb = new elbv2.ApplicationLoadBalancer(this, 'Alb', {
        loadBalancerName: 'IGRUS-Web-ALB-v2',
        vpc,
        internetFacing: true,
        securityGroup: albSg,
        vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      });
      alb.addSecurityGroup(defaultSg);

      const cloneCert = new acm.Certificate(this, 'CloneCert', {
        domainName: 'clone.igrus.co.kr',
        validation: acm.CertificateValidation.fromDns(zone),
      });
      const stagingCloneCert = new acm.Certificate(this, 'StagingCloneCert', {
        domainName: 'staging-clone.igrus.co.kr',
        validation: acm.CertificateValidation.fromDns(zone),
      });

      // cutover 로 v2 ALB 가 실제 운영 도메인도 서비스 → 기존 운영 인증서 참조(SNI 추가)
      const apiCert = acm.Certificate.fromCertificateArn(
        this,
        'ApiCert',
        'arn:aws:acm:ap-northeast-2:218736972976:certificate/189f0561-f2d4-4cf8-821d-0a0012ce9aaa', // api.igrus.co.kr
      );
      const stagingApiCert = acm.Certificate.fromCertificateArn(
        this,
        'StagingApiCert',
        'arn:aws:acm:ap-northeast-2:218736972976:certificate/6280a8cb-0941-48b3-b6e1-1d75f4666e10', // staging-api.igrus.co.kr
      );

      const prodTg = new elbv2.ApplicationTargetGroup(this, 'ProdTg', {
        targetGroupName: 'IGRUS-Web-Spring-ECS-TG-v2',
        vpc,
        port: 8080,
        protocol: elbv2.ApplicationProtocol.HTTP,
        targetType: elbv2.TargetType.IP,
        targets: [
          prodService.loadBalancerTarget({
            containerName: 'IGRUS-WEB-SPRING-SERVER',
            containerPort: 8080,
          }),
        ],
        healthCheck: { path: '/', protocol: elbv2.Protocol.HTTP, healthyHttpCodes: '200' },
      });

      const stagingTg = new elbv2.ApplicationTargetGroup(this, 'StagingTg', {
        // AWS TG 이름 최대 32자 제한 → 'Staging' 을 'Stg' 로 축약 (원본: IGRUS-Web-Spring-Staging-ECS-TG)
        targetGroupName: 'IGRUS-Web-Spring-Stg-ECS-TG-v2',
        vpc,
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP,
        targetType: elbv2.TargetType.IP,
        targets: [
          stagingService.loadBalancerTarget({
            containerName: 'IGRUS-WEB-SPRING-STAGING-SERVER',
            containerPort: 8080,
          }),
        ],
        healthCheck: { path: '/', protocol: elbv2.Protocol.HTTP, healthyHttpCodes: '200' },
      });

      alb.addListener('Prod443', {
        port: 443,
        protocol: elbv2.ApplicationProtocol.HTTPS,
        certificates: [cloneCert, apiCert], // clone.* (default) + api.igrus.co.kr (SNI)
        sslPolicy: SSL_POLICY,
        defaultTargetGroups: [prodTg],
      });
      alb.addListener('Staging8080', {
        port: 8080,
        protocol: elbv2.ApplicationProtocol.HTTPS,
        certificates: [stagingCloneCert, stagingApiCert], // staging-clone.* + staging-api.igrus.co.kr
        sslPolicy: SSL_POLICY,
        defaultTargetGroups: [stagingTg],
      });
      alb.addListener('Staging8000', {
        port: 8000,
        protocol: elbv2.ApplicationProtocol.HTTP,
        defaultTargetGroups: [stagingTg],
      });

      // ── Route53 A레코드 (alias) ──
      new route53.ARecord(this, 'CloneAlias', {
        zone,
        recordName: 'clone',
        target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
      });
      new route53.ARecord(this, 'StagingCloneAlias', {
        zone,
        recordName: 'staging-clone',
        target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
      });
      if (!CUTOVER) {
        // cutover 전: 실제 운영 도메인은 v2 ALB 로 (기존 콘솔 레코드를 코드 관리로 전환)
        new route53.ARecord(this, 'ApiAlias', {
          zone,
          recordName: 'api',
          target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
        });
      }
      new route53.ARecord(this, 'StagingApiAlias', {
        zone,
        recordName: 'staging-api',
        target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
      });

      // ── SSM Bastion EC2 (운영 IGRUS-Web-RDS-SSM-EC2 복제) — cleanup 시 앱 EC2 가 대체 ──
      const ssmRole = new iam.Role(this, 'SsmRole', {
        roleName: 'EC2_SSM_ROLE-v2',
        assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
        ],
      });
      new ec2.Instance(this, 'Bastion', {
        instanceName: 'IGRUS-Web-RDS-SSM-EC2-v2',
        vpc,
        vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
        instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
        machineImage: ec2.MachineImage.genericLinux({ 'ap-northeast-2': 'ami-0092e0c93f74c293a' }),
        securityGroup: bastionSg,
        role: ssmRole,
        blockDevices: [
          {
            deviceName: '/dev/xvda',
            volume: ec2.BlockDeviceVolume.ebs(8, { volumeType: ec2.EbsDeviceVolumeType.GP3 }),
          },
        ],
      });

      new cdk.CfnOutput(this, 'ProdCloneUrl', { value: 'https://clone.igrus.co.kr' });
      new cdk.CfnOutput(this, 'StagingCloneUrl', { value: 'https://staging-clone.igrus.co.kr:8080' });
    }

    // ── S3 file-storage 버킷 ×2 (AES256 + public 전체 차단) — 아키텍처 무관 상시 유지 ──
    // 웹/정적 버킷(igrus-web-bucket)은 CloudFront(프론트) 전용이며 백엔드가 접근하지 않으므로
    // v2 를 만들지 않는다. 백엔드가 실제 사용하는 file-storage 버킷만 v2 로 복제한다.
    this.bucket('FileBucketProd', 'igrus-web-file-storage-bucket-v2', true, [
      'https://igrus.co.kr',
      'https://www.igrus.co.kr',
      'https://api.igrus.co.kr',
    ]);
    this.bucket('FileBucketStaging', 'igrus-web-staging-file-storage-bucket-v2', true, [
      'https://staging.igrus.co.kr',
      'https://staging-api.igrus.co.kr',
    ]);

    // project.igrus.co.kr → Vercel(igrus-project). ALB/백엔드와 무관한 프론트 프로젝트지만
    // igrus.co.kr 존 DNS 를 한 곳(IaC)에서 관리하기 위해 코드화. (기존 CLI 레코드는 deleteExisting 으로 흡수)
    new route53.CnameRecord(this, 'ProjectVercelCname', {
      zone,
      recordName: 'project',
      domainName: 'cname.vercel-dns.com',
      ttl: cdk.Duration.seconds(300),
      deleteExisting: true,
    });

    new cdk.CfnOutput(this, 'PlayUrl', { value: 'https://play.igrus.co.kr' });
    new cdk.CfnOutput(this, 'AppEc2ValidationUrl', { value: 'https://ec2.igrus.co.kr' });
    new cdk.CfnOutput(this, 'AppEc2Eip', { value: appEip.ref });
    new cdk.CfnOutput(this, 'AppEc2InstanceId', { value: appInstance.instanceId });
    new cdk.CfnOutput(this, 'AppEc2SsmSession', {
      value: `aws ssm start-session --target ${appInstance.instanceId}`,
    });
  }

  /** RDS 인스턴스 생성 (기존 DB 스냅샷에서 복원 → 데이터 그대로 복제) */
  private addDatabase(
    cfg: DbConfig,
    vpc: ec2.IVpc,
    securityGroups: ec2.ISecurityGroup[],
  ): rds.IDatabaseInstance {
    // credentials 미지정 → 스냅샷의 마스터 자격증명(=기존 시크릿의 datasource 값)을 그대로 유지
    return new rds.DatabaseInstanceFromSnapshot(this, `${cfg.idPrefix}Rds`, {
      snapshotIdentifier: cfg.rdsSnapshotIdentifier,
      instanceIdentifier: cfg.rdsId,
      engine: rds.DatabaseInstanceEngine.mysql({ version: cfg.rdsVersion }),
      instanceType: cfg.instanceType,
      // 명시 안 하면 CDK 기본값 100GiB 로 부풀려짐 → 원본(v1)과 동일하게 20GB 고정
      allocatedStorage: 20,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      publiclyAccessible: cfg.rdsPubliclyAccessible,
      storageType: rds.StorageType.GP2,
      multiAz: false,
      securityGroups,
      backupRetention: cdk.Duration.days(cfg.rdsBackupDays),
      removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
    });
  }

  /** 한 앱 환경(log + taskdef + service)을 생성. 시크릿은 기존 것을 참조 */
  private addEcsService(cfg: AppEnvConfig, ctx: SharedCtx): ecs.FargateService {
    const logGroup = new logs.LogGroup(this, `${cfg.idPrefix}LogGroup`, {
      logGroupName: cfg.logGroupName,
      retention: logs.RetentionDays.INFINITE,
    });

    const taskDef = new ecs.FargateTaskDefinition(this, `${cfg.idPrefix}TaskDef`, {
      family: cfg.family,
      cpu: 1024,
      memoryLimitMiB: 2048,
      taskRole: ctx.taskRole,
      executionRole: ctx.executionRole,
    });
    cfg.appSecret.grantRead(ctx.taskRole); // 앱이 런타임에 기존 시크릿 읽기

    taskDef.addContainer(`${cfg.idPrefix}Spring`, {
      containerName: cfg.containerName,
      image: ecs.ContainerImage.fromRegistry(cfg.image),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'ecs', logGroup }),
      portMappings: [{ containerPort: 8080, hostPort: 8080, protocol: ecs.Protocol.TCP }],
      // 앱 코드 불변. DB host 만 v2 RDS 로 오버라이드.
      // datasource 계정/비번 + jwt/mail/webhook 은 모두 기존 시크릿 값을 그대로 사용
      // (스냅샷 복원 RDS 는 마스터 비번이 기존과 동일하므로 별도 주입 불필요)
      environment: {
        SPRING_ACTIVE_PROFILE: cfg.springProfile,
        SPRING_DATASOURCE_URL: `jdbc:mysql://${cfg.db.dbInstanceEndpointAddress}:3306/${cfg.rdsDbName}`,
        // S3 버킷명은 기존 시크릿(app.storage.s3.bucket-name)에서 직접 v2 버킷으로 지정됨
      },
    });

    const serviceSgs = cfg.attachDefaultSgToService ? [ctx.ecsSg, ctx.defaultSg] : [ctx.ecsSg];

    return new ecs.FargateService(this, `${cfg.idPrefix}Service`, {
      serviceName: cfg.serviceName,
      cluster: ctx.cluster,
      taskDefinition: taskDef,
      desiredCount: cfg.desiredCount,
      assignPublicIp: true,
      securityGroups: serviceSgs,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      capacityProviderStrategies: [{ capacityProvider: 'FARGATE', weight: 1, base: 0 }],
      // 앱 기동(~60초) + ALB 헬스 수렴을 견디도록 grace 넉넉히
      healthCheckGracePeriod: cdk.Duration.seconds(240),
      // 배포 중 새 태스크가 healthy 될 때까지 기존 태스크 유지(무중단) + 실패 시 빠른 롤백
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
      circuitBreaker: { rollback: true },
    });
  }

  /** S3 버킷 헬퍼 (AES256 + public 전체 차단, 선택적 CORS) */
  private bucket(
    id: string,
    bucketName: string,
    withCors: boolean,
    corsOrigins: string[] = [],
  ): s3.Bucket {
    return new s3.Bucket(this, id, {
      bucketName,
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      cors: withCors
        ? [
            {
              allowedHeaders: ['*'],
              allowedMethods: [
                s3.HttpMethods.GET,
                s3.HttpMethods.HEAD,
                s3.HttpMethods.PUT,
                s3.HttpMethods.POST,
              ],
              allowedOrigins: corsOrigins,
              exposedHeaders: ['ETag'],
              maxAge: 3000,
            },
          ]
        : undefined,
    });
  }
}
