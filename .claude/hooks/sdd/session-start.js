#!/usr/bin/env node
/**
 * SessionStart Hook: SDD 상태 대시보드
 * 세션 시작 시 현재 SDD 상태를 표시
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

  const projectDir = getProjectDir(input);
  const currentBranch = getCurrentBranch(projectDir);
  const specsDir = path.join(projectDir, 'specs');
  const featureBranch = isFeatureBranch(currentBranch);
  const featureDir = featureBranch ? path.join(specsDir, currentBranch) : null;

  // SDD 상태 수집
  let specExists = false, planExists = false, tasksExists = false;
  let specStatus = '없음', planStatus = '없음', tasksStatus = '없음';

  if (featureDir && fs.existsSync(featureDir)) {
    const specFile = path.join(featureDir, 'spec.md');
    const planFile = path.join(featureDir, 'plan.md');
    const tasksFile = path.join(featureDir, 'tasks.md');

    if (fs.existsSync(specFile)) {
      specExists = true;
      const content = fs.readFileSync(specFile, 'utf8');
      specStatus = content.includes('NEEDS CLARIFICATION') ? '명확화 필요' : '완료';
    }

    if (fs.existsSync(planFile)) {
      planExists = true;
      const content = fs.readFileSync(planFile, 'utf8');
      planStatus = content.includes('NEEDS CLARIFICATION') ? '명확화 필요' : '완료';
    }

    if (fs.existsSync(tasksFile)) {
      tasksExists = true;
      const content = fs.readFileSync(tasksFile, 'utf8');
      const pendingCount = (content.match(/- \[ \]/g) || []).length;
      const completedCount = (content.match(/- \[[xX]\]/g) || []).length;

      if (pendingCount === 0 && completedCount > 0) {
        tasksStatus = '모두 완료';
      } else if (completedCount > 0) {
        tasksStatus = `진행중 (${completedCount}/${pendingCount + completedCount})`;
      } else if (pendingCount > 0) {
        tasksStatus = `시작 전 (${pendingCount}개)`;
      } else {
        tasksStatus = '작업 없음';
      }
    }
  }

  // 대시보드 생성
  let dashboard = `
========================================
   SDD (Spec-Driven Development) 상태
========================================

현재 브랜치: ${currentBranch}

`;

  if (featureBranch) {
    dashboard += `문서 상태:
  spec.md  : ${specStatus} ${specExists ? '[v]' : '[ ]'}
  plan.md  : ${planStatus} ${planExists ? '[v]' : '[ ]'}
  tasks.md : ${tasksStatus} ${tasksExists ? '[v]' : '[ ]'}

`;

    if (!specExists) {
      dashboard += `[!] spec.md가 없습니다.
    /speckit.specify 명령어로 스펙을 먼저 작성하세요.

`;
    } else if (!planExists) {
      dashboard += `[!] plan.md가 없습니다.
    /speckit.plan 명령어로 구현 계획을 작성하세요.

`;
    } else if (!tasksExists) {
      dashboard += `[!] tasks.md가 없습니다.
    /speckit.tasks 명령어로 작업 목록을 생성하세요.

`;
    } else {
      dashboard += `[v] SDD 문서가 모두 준비되었습니다.
    /speckit.implement 로 구현을 시작하세요.

`;
    }
  } else {
    // 최근 specs 폴더 확인
    let latestSpec = null;
    if (fs.existsSync(specsDir)) {
      try {
        const dirs = fs.readdirSync(specsDir, { withFileTypes: true })
          .filter(d => d.isDirectory() && /^\d{3}-/.test(d.name))
          .map(d => d.name)
          .sort()
          .reverse();
        if (dirs.length > 0) latestSpec = dirs[0];
      } catch (e) {}
    }

    if (latestSpec) {
      dashboard += `최근 작업 스펙: ${latestSpec}

`;
    }

    dashboard += `[i] Feature 브랜치가 아닙니다.
    새 기능 개발은 /speckit.specify 명령어로 시작하세요.

`;
  }

  dashboard += '========================================';

  console.log(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'SessionStart',
      additionalContext: dashboard
    }
  }));
  process.exit(0);
}
