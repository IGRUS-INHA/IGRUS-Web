#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { IgrusWebV2Stack } from '../lib/igrus-web-v2-stack';

const app = new cdk.App();

// 운영과 동일 계정/리전 (Vpc/HostedZone fromLookup 에 명시적 env 필수)
new IgrusWebV2Stack(app, 'IgrusWebV2Stack', {
  stackName: 'IgrusWebV2Stack',
  env: {
    account: '218736972976',
    region: 'ap-northeast-2',
  },
});
