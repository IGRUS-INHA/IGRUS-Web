#!/usr/bin/env node
/**
 * UserPromptSubmit Hook: 기능 구현 요청 감지
 * 구현 요청 시 spec.md 존재 여부 확인
 */

const fs = require('fs');
const path = require('path');
const { getCurrentBranch, isFeatureBranch, getProjectDir } = require('../lib/git-utils');

// stdin에서 JSON 입력 읽기 (타임아웃 포함)
const STDIN_TIMEOUT_MS = 3000;
let inputData = '';
let mainCalled = false;

process.stdin.setEncoding('utf8');

const timeoutId = setTimeout(() => {
  if (!mainCalled) {
    mainCalled = true;
    main(inputData);
  }
}, STDIN_TIMEOUT_MS);

process.stdin.on('readable', () => {
  let chunk;
  while ((chunk = process.stdin.read()) !== null) {
    inputData += chunk;
  }
});

process.stdin.on('end', () => {
  clearTimeout(timeoutId);
  if (!mainCalled) {
    mainCalled = true;
    main(inputData);
  }
});

process.stdin.on('error', () => {
  clearTimeout(timeoutId);
  if (!mainCalled) {
    mainCalled = true;
    process.exit(0);
  }
});

function main(inputJson) {
  let input = {};
  try {
    input = JSON.parse(inputJson || '{}');
  } catch (e) {
    process.exit(0);
  }

  const prompt = input.prompt || '';
  const projectDir = getProjectDir(input);
  const promptLower = prompt.toLowerCase();

  // 구현 관련 키워드 패턴
  const implementationPatterns = [
    '구현해', '만들어', '작성해', '추가해', '개발해', '코딩해',
    'implement', 'create', 'build', 'develop', 'add feature', 'write code', 'code the'
  ];

  // Speckit 명령어 패턴 (스킵)
  const speckitPatterns = [
    '/speckit', 'speckit.specify', 'speckit.plan', 'speckit.tasks',
    'speckit.implement', 'speckit.clarify', 'speckit.analyze'
  ];

  // Speckit 명령어인 경우 스킵
  if (speckitPatterns.some(p => promptLower.includes(p.toLowerCase()))) {
    process.exit(0);
  }

  // 구현 요청이 아니면 스킵
  if (!implementationPatterns.some(p => promptLower.includes(p.toLowerCase()))) {
    process.exit(0);
  }

  // 현재 브랜치와 문서 상태 확인
  const currentBranch = getCurrentBranch(projectDir);
  const specsDir = path.join(projectDir, 'specs');
  let specExists = false, planExists = false, tasksExists = false;

  if (isFeatureBranch(currentBranch)) {
    const featureDir = path.join(specsDir, currentBranch);
    specExists = fs.existsSync(path.join(featureDir, 'spec.md'));
    planExists = fs.existsSync(path.join(featureDir, 'plan.md'));
    tasksExists = fs.existsSync(path.join(featureDir, 'tasks.md'));
  }

  // 문서가 없는 경우 알림
  if (!specExists || !planExists || !tasksExists) {
    const missingDocs = [];
    if (!specExists) missingDocs.push('spec.md');
    if (!planExists) missingDocs.push('plan.md');
    if (!tasksExists) missingDocs.push('tasks.md');

    console.log(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'UserPromptSubmit',
        additionalContext: `
[SDD 알림] 기능 구현 요청이 감지되었습니다.

현재 누락된 문서: ${missingDocs.join(', ')}

IGRUS-Web 프로젝트는 Spec-Driven Development(SDD) 방식을 따릅니다.
기능 구현 전에 다음 문서를 먼저 작성하는 것을 권장합니다:

1. spec.md - /speckit.specify 명령어로 생성
2. plan.md - /speckit.plan 명령어로 생성
3. tasks.md - /speckit.tasks 명령어로 생성

구현을 진행하시겠습니까? 아니면 문서를 먼저 작성하시겠습니까?
`
      }
    }));
  }

  process.exit(0);
}
