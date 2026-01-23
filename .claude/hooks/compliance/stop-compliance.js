#!/usr/bin/env node
/**
 * Stop Hook: 규칙/헌법 준수 체크
 * 작업 종료 시 프로젝트 규칙 준수 여부 확인
 */

const path = require('path');
const { getCurrentBranch, getProjectDir } = require('../lib/git-utils');
const {
  loadRules,
  getChangedFiles,
  runChecks,
  generateReport
} = require('../lib/compliance-checker');

// stdin에서 JSON 입력 읽기 (타임아웃 포함)
const STDIN_TIMEOUT_MS = 3000;
let inputData = '';
let mainCalled = false;

process.stdin.setEncoding('utf8');

// 타임아웃 설정: stdin이 닫히지 않아도 일정 시간 후 실행
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

// stdin 에러 처리
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
    // JSON 파싱 실패 시 종료
    process.exit(0);
  }

  // 이미 Stop hook이 실행 중이면 무한 루프 방지
  if (input.stop_hook_active) {
    process.exit(0);
  }

  const projectDir = getProjectDir(input);
  const branch = getCurrentBranch(projectDir);

  // 변경된 파일 목록 수집
  const changedFiles = getChangedFiles(projectDir);

  // 변경 파일이 없으면 스킵
  if (changedFiles.length === 0) {
    process.exit(0);
  }

  // 소스 코드 변경만 필터링 (문서, 설정 파일 제외)
  const codeChanges = changedFiles.filter(f => {
    const ext = path.extname(f).toLowerCase();
    const codeExts = ['.java', '.ts', '.tsx', '.js', '.jsx', '.kt', '.py'];
    return codeExts.includes(ext);
  });

  // 코드 변경이 없으면 스킵
  if (codeChanges.length === 0) {
    process.exit(0);
  }

  try {
    // 규칙 로드
    const rulesConfig = loadRules(projectDir);

    // 체크 실행
    const results = runChecks(rulesConfig, changedFiles, projectDir, branch);

    // 유의미한 결과만 필터링 (skip 제외)
    const meaningfulResults = results.filter(r => r.status !== 'skip');

    // 모두 통과하면 간단한 메시지만
    const hasIssues = meaningfulResults.some(r => r.status === 'fail' || r.status === 'warn');

    if (meaningfulResults.length === 0) {
      // 체크할 항목 없음
      process.exit(0);
    }

    // 리포트 생성
    const report = generateReport(
      results,
      rulesConfig.manualCheckReminders,
      branch,
      changedFiles.length
    );

    // 결과 출력
    console.log(JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'Stop',
        additionalContext: '\n' + report
      }
    }));

  } catch (error) {
    // 에러 발생 시 조용히 실패 (작업 흐름 방해 안 함)
    // 디버그용 로그만 남김
    if (process.env.DEBUG_COMPLIANCE) {
      console.error('Compliance check error:', error.message);
    }
  }

  process.exit(0);
}
