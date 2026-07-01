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

/** 한 앱 환경(prod/staging) 구성 설정 */
interface AppEnvConfig {
  idPrefix: string;
  /** 기존 Secrets Manager 시크릿 이름 (그대로 재사용) */
  existingSecretName: string;
  springProfile: string;
  // ECS
  serviceName: string;
  family: string;
  containerName: string;
  image: string;
  logGroupName: string;
  attachDefaultSgToService: boolean; // prod=true, staging=false (운영 현황 그대로)
  desiredCount: number; // prod=1(상시 가동), staging=0(비용 절감 위해 중지)
  // RDS (기존 DB 스냅샷에서 복원 → 데이터 그대로 복제)
  rdsId: string;
  rdsVersion: rds.MysqlEngineVersion;
  rdsSnapshotIdentifier: string;
  rdsDbName: string; // SPRING_DATASOURCE_URL 구성용 (스냅샷 내 DB명)
  rdsPubliclyAccessible: boolean;
  rdsBackupDays: number;
}

interface SharedCtx {
  vpc: ec2.IVpc;
  cluster: ecs.ICluster;
  ecsSg: ec2.ISecurityGroup;
  rdsSg: ec2.ISecurityGroup;
  defaultSg: ec2.ISecurityGroup;
  taskRole: iam.IRole;
  executionRole: iam.IRole;
}

/**
 * IGRUS-Web 운영 + 스테이징 인프라를 "토씨 하나 같게" 복제하는 스택.
 *
 * - 수동 생성 자원 이름에만 `-v2` 접미사
 * - 접속 도메인만 신규: clone.igrus.co.kr (운영) / staging-clone.igrus.co.kr (스테이징)
 * - 시크릿은 기존 Secrets Manager(igrus/web/server/prod, .../staging)를 그대로 재사용
 * - 앱 코드 불변. DB만 v2로 격리: v2 RDS 자동생성 자격증명을 ECS 가 앱 datasource 에 주입
 *
 * 기준 스냅샷 2026-06-29 / account 218736972976 / ap-northeast-2 / default VPC.
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

    const rdsSg = new ec2.SecurityGroup(this, 'RdsSg', {
      vpc,
      securityGroupName: 'IGRUS-Web-MySQL-RDS-SG-v2',
      description: 'IGRUS Web MySQL RDS Security Group',
      allowAllOutbound: true,
    });
    rdsSg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(3306));
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
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AmazonECSTaskExecutionRolePolicy'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AWSSecretsManagerClientReadOnlyAccess'),
      ],
    });

    // ── ECS 클러스터 (FARGATE / FARGATE_SPOT) ──
    const cluster = new ecs.Cluster(this, 'Cluster', {
      clusterName: 'IGRUS-WEB-ECS-Cluster-v2',
      vpc,
      enableFargateCapacityProviders: true,
    });

    const ctx: SharedCtx = { vpc, cluster, ecsSg, rdsSg, defaultSg, taskRole, executionRole };

    // ── 운영(prod) 앱 환경 ──
    const prod = this.addAppEnvironment(
      {
        idPrefix: 'Prod',
        existingSecretName: 'igrus/web/server/prod',
        springProfile: 'prod',
        serviceName: 'igrus-web-server-ecs-service-v2',
        family: 'igrus-web-server-task-def-v2',
        containerName: 'IGRUS-WEB-SPRING-SERVER',
        image: '218736972976.dkr.ecr.ap-northeast-2.amazonaws.com/igrus/web/spring:v1.1.8',
        logGroupName: '/ecs/igrus-web-server-task-def-v2',
        attachDefaultSgToService: true,
        desiredCount: 1,
        rdsId: 'igrus-web-mysql-rds-v2',
        rdsVersion: rds.MysqlEngineVersion.of('8.0.44', '8.0'),
        rdsSnapshotIdentifier: 'igrus-web-mysql-rds-v2seed-20260629',
        rdsDbName: 'igrus_web',
        rdsPubliclyAccessible: false,
        rdsBackupDays: 3,
      },
      ctx,
    );

    // ── 스테이징(staging) 앱 환경 ──
    const staging = this.addAppEnvironment(
      {
        idPrefix: 'Staging',
        existingSecretName: 'igrus/web/server/staging',
        springProfile: 'staging',
        serviceName: 'igrus-web-server-staging-task-def-service-v2',
        family: 'igrus-web-server-staging-task-def-v2',
        containerName: 'IGRUS-WEB-SPRING-STAGING-SERVER',
        image:
          '218736972976.dkr.ecr.ap-northeast-2.amazonaws.com/igrus/web/staging/spring:6b34009e2deac8c65c6d31f4d62c34a07343a8f7',
        logGroupName: '/ecs/igrus-web-server-staging-task-def-v2',
        attachDefaultSgToService: false,
        desiredCount: 0, // staging 중지(비용 절감). 필요 시 1로 올려 재배포
        rdsId: 'igrus-web-staging-mysql-rds-v2',
        rdsVersion: rds.MysqlEngineVersion.of('8.4.7', '8.4'),
        rdsSnapshotIdentifier: 'igrus-web-staging-mysql-rds-v2seed-20260629',
        rdsDbName: 'igrus_web_staging',
        rdsPubliclyAccessible: false, // 보안상 private (실 PII 복원이라 운영 public 설정에서 이것만 변경)
        rdsBackupDays: 1,
      },
      ctx,
    );

    // ── S3 file-storage 버킷 ×2 (AES256 + public 전체 차단) ──
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
        prod.service.loadBalancerTarget({
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
        staging.service.loadBalancerTarget({
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
    // cutover: 실제 운영 도메인을 v2 ALB 로 (기존 콘솔 레코드를 코드 관리로 전환)
    new route53.ARecord(this, 'ApiAlias', {
      zone,
      recordName: 'api',
      target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
    });
    new route53.ARecord(this, 'StagingApiAlias', {
      zone,
      recordName: 'staging-api',
      target: route53.RecordTarget.fromAlias(new route53targets.LoadBalancerTarget(alb)),
    });

    // project.igrus.co.kr → Vercel(igrus-project). ALB/백엔드와 무관한 프론트 프로젝트지만
    // igrus.co.kr 존 DNS 를 한 곳(IaC)에서 관리하기 위해 코드화. (기존 CLI 레코드는 deleteExisting 으로 흡수)
    new route53.CnameRecord(this, 'ProjectVercelCname', {
      zone,
      recordName: 'project',
      domainName: 'cname.vercel-dns.com',
      ttl: cdk.Duration.seconds(300),
      deleteExisting: true,
    });

    // ── SSM Bastion EC2 (운영 IGRUS-Web-RDS-SSM-EC2 복제) ──
    const ssmRole = new iam.Role(this, 'SsmRole', {
      roleName: 'EC2_SSM_ROLE-v2',
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore')],
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

  /** 한 앱 환경(RDS + log + taskdef + service)을 생성. 시크릿은 기존 것을 참조 */
  private addAppEnvironment(
    cfg: AppEnvConfig,
    ctx: SharedCtx,
  ): { service: ecs.FargateService; db: rds.IDatabaseInstance } {
    // 기존 Secrets Manager 시크릿 참조 (앱이 spring.config.import 로 그대로 읽음)
    const appSecret = secretsmanager.Secret.fromSecretNameV2(
      this,
      `${cfg.idPrefix}AppSecret`,
      cfg.existingSecretName,
    );

    const logGroup = new logs.LogGroup(this, `${cfg.idPrefix}LogGroup`, {
      logGroupName: cfg.logGroupName,
      retention: logs.RetentionDays.INFINITE,
    });

    // v2 RDS: 기존 DB 스냅샷에서 복원 → 데이터 그대로 복제.
    // credentials 미지정 → 스냅샷의 마스터 자격증명(=기존 시크릿의 datasource 값)을 그대로 유지
    const db = new rds.DatabaseInstanceFromSnapshot(this, `${cfg.idPrefix}Rds`, {
      snapshotIdentifier: cfg.rdsSnapshotIdentifier,
      instanceIdentifier: cfg.rdsId,
      engine: rds.DatabaseInstanceEngine.mysql({ version: cfg.rdsVersion }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      // 명시 안 하면 CDK 기본값 100GiB 로 부풀려짐 → 원본(v1)과 동일하게 20GB 고정
      allocatedStorage: 20,
      vpc: ctx.vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      publiclyAccessible: cfg.rdsPubliclyAccessible,
      storageType: rds.StorageType.GP2,
      multiAz: false,
      securityGroups: [ctx.rdsSg, ctx.defaultSg],
      backupRetention: cdk.Duration.days(cfg.rdsBackupDays),
      removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
    });

    const taskDef = new ecs.FargateTaskDefinition(this, `${cfg.idPrefix}TaskDef`, {
      family: cfg.family,
      cpu: 1024,
      memoryLimitMiB: 2048,
      taskRole: ctx.taskRole,
      executionRole: ctx.executionRole,
    });
    appSecret.grantRead(ctx.taskRole); // 앱이 런타임에 기존 시크릿 읽기

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
        SPRING_DATASOURCE_URL: `jdbc:mysql://${db.dbInstanceEndpointAddress}:3306/${cfg.rdsDbName}`,
        // S3 버킷명은 기존 시크릿(app.storage.s3.bucket-name)에서 직접 v2 버킷으로 지정됨
      },
    });

    const serviceSgs = cfg.attachDefaultSgToService ? [ctx.ecsSg, ctx.defaultSg] : [ctx.ecsSg];

    const service = new ecs.FargateService(this, `${cfg.idPrefix}Service`, {
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

    return { service, db };
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
