#!/usr/bin/env node
/**
 * PostToolUse Hook: 코드 수정 후 문서 업데이트 알림
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
  const toolResponse = input.tool_response || {};
  const projectDir = getProjectDir(input);

  // Write 또는 Edit 도구가 아니면 스킵
  if (!['Write', 'Edit'].includes(toolName)) {
    process.exit(0);
  }

  // 성공하지 않은 경우 스킵
  if (toolResponse && toolResponse.success === false) {
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
  let relativePath = filePath.replace(projectDir, '').replace(/^[/\\]/, '').replace(/\\/g, '/');

  // 파일 유형에 따른 문서 업데이트 제안
  const updateSuggestions = [];

  if (/backend\/.*\/(domain|entity|model)\//i.test(relativePath)) {
    updateSuggestions.push('- 도메인 모델 문서 업데이트 필요 (docs/feature/ 또는 data-model.md)');
  }
  if (/backend\/.*\/(controller|api)\//i.test(relativePath)) {
    updateSuggestions.push('- API 문서 업데이트 필요');
    updateSuggestions.push('- contracts/ 스펙과 일치 여부 확인');
  }
  if (/backend\/.*\/service\//i.test(relativePath)) {
    updateSuggestions.push('- 비즈니스 로직 문서 업데이트 고려');
  }
  if (/backend\/.*\/test\//i.test(relativePath)) {
    updateSuggestions.push('- backend/docs/test-case/ 테스트 케이스 문서 업데이트');
  }
  if (/frontend\/.*\/components\//i.test(relativePath)) {
    updateSuggestions.push('- 컴포넌트 가이드 문서 업데이트 필요');
  }
  if (/frontend\/.*\/pages\//i.test(relativePath)) {
    updateSuggestions.push('- 화면 흐름 문서 업데이트 고려');
  }

  // 관련 문서 찾기
  const relatedDocs = findRelatedDocs(filePath, projectDir, currentBranch);

  // tasks.md 완료 표시 제안
  if (isFeatureBranch(currentBranch)) {
    const tasksFile = path.join(projectDir, 'specs', currentBranch, 'tasks.md');
    if (fs.existsSync(tasksFile)) {
      updateSuggestions.push('- tasks.md에서 관련 작업 완료 표시 [x] 필요');
    }
  }

  if (updateSuggestions.length > 0 || relatedDocs.length > 0) {
    const fileName = path.basename(filePath);
    let message = `
[SDD 알림] 코드가 수정되었습니다: ${fileName}

`;

    if (relatedDocs.length > 0) {
      message += '관련 문서:\n';
      const uniqueDocs = [...new Set(relatedDocs.map(d => d.document))];
      uniqueDocs.forEach(doc => message += `  - ${doc}\n`);
      message += '\n';
    }

    if (updateSuggestions.length > 0) {
      message += '업데이트 제안:\n';
      updateSuggestions.forEach(s => message += `${s}\n`);
    }

    console.log(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PostToolUse',
        additionalContext: message
      }
    }));
  }

  process.exit(0);
}
