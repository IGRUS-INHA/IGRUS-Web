#!/usr/bin/env node
/**
 * PreToolUse Hook: 코드 수정 전 문서 확인
 * 알림만 제공 (블로킹 없음)
 */

const fs = require('fs');
const path = require('path');
const { getCurrentBranch, isFeatureBranch, getProjectDir } = require('../lib/git-utils');
const { findRelatedDocs, isDocFile, isCodeFile } = require('../lib/doc-mapper');

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

  const toolName = input.tool_name || '';
  const toolInput = input.tool_input || {};
  const projectDir = getProjectDir(input);

  // Write 또는 Edit 도구가 아니면 스킵
  if (!['Write', 'Edit'].includes(toolName)) {
    process.exit(0);
  }

  const filePath = toolInput.file_path || '';

  // 문서 파일 수정은 스킵
  if (isDocFile(filePath)) {
    process.exit(0);
  }

  // 소스 코드 파일이 아니면 스킵
  if (!isCodeFile(filePath)) {
    process.exit(0);
  }

  const currentBranch = getCurrentBranch(projectDir);
  const featureBranch = isFeatureBranch(currentBranch);
  const specsDir = path.join(projectDir, 'specs');
  const warnings = [];

  if (!featureBranch) {
    warnings.push(`[i] Feature 브랜치(${currentBranch})가 아닌 곳에서 코드를 수정하려고 합니다.`);
    warnings.push(`    새 기능 개발은 /speckit.specify 로 시작하여 feature 브랜치를 생성하세요.`);
  } else {
    const featureDir = path.join(specsDir, currentBranch);
    const specExists = fs.existsSync(path.join(featureDir, 'spec.md'));
    const planExists = fs.existsSync(path.join(featureDir, 'plan.md'));
    const tasksExists = fs.existsSync(path.join(featureDir, 'tasks.md'));

    if (!specExists) {
      warnings.push('[!] spec.md가 없습니다. /speckit.specify 로 먼저 스펙을 작성하세요.');
    }
    if (!planExists && specExists) {
      warnings.push('[!] plan.md가 없습니다. /speckit.plan 으로 구현 계획을 작성하세요.');
    }
    if (!tasksExists && planExists) {
      warnings.push('[!] tasks.md가 없습니다. /speckit.tasks 로 작업 목록을 생성하세요.');
    }
  }

  // 관련 문서 찾기
  const relatedDocs = findRelatedDocs(filePath, projectDir, currentBranch);
  if (relatedDocs.length > 0) {
    warnings.push('');
    warnings.push('관련 문서:');
    const uniqueDocs = [...new Set(relatedDocs.map(d => d.document))];
    uniqueDocs.forEach(doc => warnings.push(`  - ${doc}`));
  }

  if (warnings.length > 0) {
    const fileName = path.basename(filePath);
    console.log(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        additionalContext: `
[SDD 알림] 코드 수정 전 확인사항 (${fileName}):

${warnings.join('\n')}
`
      }
    }));
  }

  process.exit(0);
}
