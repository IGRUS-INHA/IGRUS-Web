/**
 * 규칙/헌법 준수 체크 실행 엔진
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const { getCurrentBranch, isFeatureBranch, getProjectDir } = require('./git-utils');
const { findRelatedDocs, isDocFile, isCodeFile } = require('./doc-mapper');
const { parseAllRuleSources, mergeManualReminders } = require('./rule-parser');

// 글로브 패턴 매칭 유틸리티
const minimatch = require('./minimatch-lite');

/**
 * 규칙 설정 파일 로드
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 규칙 설정
 */
function loadRulesConfig(projectDir) {
  const configPath = path.join(projectDir, '.claude', 'hooks', 'config', 'compliance-rules.json');
  if (!fs.existsSync(configPath)) {
    return { rules: [], manualCheckReminders: [], ruleSources: [] };
  }
  return JSON.parse(fs.readFileSync(configPath, 'utf8'));
}

/**
 * 규칙 로드 및 파싱된 규칙과 병합
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 병합된 규칙
 */
function loadRules(projectDir) {
  const config = loadRulesConfig(projectDir);
  const parsed = parseAllRuleSources(projectDir, config.ruleSources || []);

  // 수동 확인 알림 병합
  const manualReminders = mergeManualReminders(
    parsed.principles,
    config.manualCheckReminders || []
  );

  return {
    rules: config.rules || [],
    manualCheckReminders: manualReminders,
    parsedPrinciples: parsed.principles,
    parsedRules: parsed.rules,
    ruleSourcesHash: parsed.hash
  };
}

/**
 * Git에서 변경된 파일 목록 추출
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {Array} 변경된 파일 목록
 */
function getChangedFiles(projectDir) {
  const files = new Set();

  try {
    // staged 파일
    const staged = execSync('git diff --cached --name-only', {
      cwd: projectDir,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    }).trim();
    if (staged) staged.split('\n').forEach(f => files.add(f));

    // unstaged 파일
    const unstaged = execSync('git diff --name-only', {
      cwd: projectDir,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    }).trim();
    if (unstaged) unstaged.split('\n').forEach(f => files.add(f));

    // untracked 파일
    const untracked = execSync('git ls-files --others --exclude-standard', {
      cwd: projectDir,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    }).trim();
    if (untracked) untracked.split('\n').forEach(f => files.add(f));
  } catch (e) {
    // Git 명령 실패 시 빈 배열 반환
  }

  return Array.from(files).filter(f => f.length > 0);
}

/**
 * 파일 쌍 존재 여부 체크 (테스트 커버리지)
 * @param {object} rule - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 체크 결과
 */
function checkFilePair(rule, changedFiles, projectDir) {
  const config = rule.config;
  const results = { passed: [], failed: [] };

  for (const file of changedFiles) {
    // 소스 패턴과 매치되는지 확인
    if (!minimatch(file, config.sourcePattern)) continue;

    // 제외 패턴 체크
    if (config.excludePatterns) {
      const excluded = config.excludePatterns.some(p => minimatch(file, p));
      if (excluded) continue;
    }

    // 타겟 파일 경로 계산
    const targetFile = file.replace(
      new RegExp(config.transform.from),
      config.transform.to
    );

    const targetPath = path.join(projectDir, targetFile);
    if (fs.existsSync(targetPath)) {
      results.passed.push({ source: file, target: targetFile });
    } else {
      results.failed.push({ source: file, target: targetFile });
    }
  }

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status: results.failed.length === 0 ? 'pass' : 'fail',
    passed: results.passed,
    failed: results.failed,
    message: results.failed.length === 0
      ? rule.message.pass
      : `${rule.message.fail}: ${results.failed.map(f => path.basename(f.source)).join(', ')}`
  };
}

/**
 * 필수 패턴 존재 여부 체크 (Swagger 어노테이션)
 * @param {object} rule - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 체크 결과
 */
function checkContentPattern(rule, changedFiles, projectDir) {
  const config = rule.config;
  const results = { passed: [], failed: [] };

  for (const file of changedFiles) {
    // 파일 패턴 매치
    if (!minimatch(file, config.filePattern)) continue;

    // 제외 패턴 체크
    if (config.excludePatterns) {
      const excluded = config.excludePatterns.some(p => minimatch(file, p));
      if (excluded) continue;
    }

    const filePath = path.join(projectDir, file);
    if (!fs.existsSync(filePath)) continue;

    const content = fs.readFileSync(filePath, 'utf8');

    // 모든 필수 패턴이 존재하는지 확인
    const missingPatterns = [];
    for (const pattern of config.requiredPatterns) {
      const regex = new RegExp(pattern);
      if (!regex.test(content)) {
        missingPatterns.push(pattern);
      }
    }

    if (missingPatterns.length === 0) {
      results.passed.push({ file });
    } else {
      results.failed.push({ file, missingPatterns });
    }
  }

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status: results.failed.length === 0 ? 'pass' : 'fail',
    passed: results.passed,
    failed: results.failed,
    message: results.failed.length === 0
      ? rule.message.pass
      : `${rule.message.fail}: ${results.failed.map(f => path.basename(f.file)).join(', ')}`
  };
}

/**
 * 금지 패턴 감지 (보안 체크)
 * @param {object} rule - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 체크 결과
 */
function checkForbiddenPattern(rule, changedFiles, projectDir) {
  const config = rule.config;
  const results = { passed: [], failed: [] };

  for (const file of changedFiles) {
    // 파일 패턴 매치 (확장자 기반)
    const ext = path.extname(file).slice(1);
    const allowedExts = config.filePattern.match(/\{([^}]+)\}/)?.[1]?.split(',') || [];
    if (allowedExts.length > 0 && !allowedExts.includes(ext)) continue;

    // 제외 패턴 체크
    if (config.excludePatterns) {
      const excluded = config.excludePatterns.some(p => minimatch(file, p));
      if (excluded) continue;
    }

    const filePath = path.join(projectDir, file);
    if (!fs.existsSync(filePath)) continue;

    const content = fs.readFileSync(filePath, 'utf8');

    // 금지 패턴 검색
    const foundPatterns = [];
    for (const pattern of config.forbiddenPatterns) {
      const regex = new RegExp(pattern, 'gi');
      if (regex.test(content)) {
        foundPatterns.push(pattern);
      }
    }

    if (foundPatterns.length === 0) {
      results.passed.push({ file });
    } else {
      results.failed.push({ file, foundPatterns });
    }
  }

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status: results.failed.length === 0 ? 'pass' : 'fail',
    passed: results.passed,
    failed: results.failed,
    message: results.failed.length === 0
      ? rule.message.pass
      : `${rule.message.fail}: ${results.failed.map(f => path.basename(f.file)).join(', ')}`
  };
}

/**
 * 관련 파일 존재 여부 체크 (Flyway 마이그레이션)
 * @param {object} rule - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object} 체크 결과
 */
function checkFileExists(rule, changedFiles, projectDir) {
  const config = rule.config;

  // 트리거 패턴에 매치되는 파일이 있는지 확인
  const triggeredFiles = changedFiles.filter(f => minimatch(f, config.triggerPattern));

  if (triggeredFiles.length === 0) {
    return {
      ruleId: rule.id,
      ruleName: rule.name,
      principle: rule.principle,
      status: 'skip',
      message: '해당 파일 없음'
    };
  }

  // 체크 경로에 파일이 존재하는지 확인
  const checkPath = path.join(projectDir, config.checkPath);
  if (!fs.existsSync(checkPath)) {
    return {
      ruleId: rule.id,
      ruleName: rule.name,
      principle: rule.principle,
      status: 'fail',
      triggered: triggeredFiles,
      message: rule.message.fail
    };
  }

  const files = fs.readdirSync(checkPath);
  const patternRegex = new RegExp(config.checkPattern.replace(/\*/g, '.*'));
  const matchingFiles = files.filter(f => patternRegex.test(f));

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status: matchingFiles.length > 0 ? 'pass' : 'fail',
    triggered: triggeredFiles,
    found: matchingFiles,
    message: matchingFiles.length > 0
      ? `${rule.message.pass}: ${matchingFiles[matchingFiles.length - 1]}`
      : rule.message.fail
  };
}

/**
 * 마크다운 체크리스트 상태 체크 (tasks.md)
 * @param {object} rule - 규칙 설정
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {string} branch - 현재 브랜치
 * @returns {object} 체크 결과
 */
function checkMarkdownChecklist(rule, projectDir, branch) {
  const config = rule.config;
  const filePath = path.join(projectDir, config.filePath.replace('{branch}', branch));

  if (!fs.existsSync(filePath)) {
    return {
      ruleId: rule.id,
      ruleName: rule.name,
      principle: rule.principle,
      status: 'skip',
      message: 'tasks.md 파일 없음'
    };
  }

  const content = fs.readFileSync(filePath, 'utf8');

  // 체크리스트 아이템 카운트
  const unchecked = (content.match(/- \[ \]/g) || []).length;
  const checked = (content.match(/- \[x\]/gi) || []).length;

  const status = unchecked <= config.warnThreshold ? 'pass' : 'warn';
  const message = status === 'pass'
    ? rule.message.pass
    : rule.message.fail.replace('{count}', unchecked);

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status,
    unchecked,
    checked,
    total: unchecked + checked,
    message
  };
}

/**
 * 문서-코드 동기화 체크
 * @param {object} rule - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {string} branch - 현재 브랜치
 * @returns {object} 체크 결과
 */
function checkDocCodeSync(rule, changedFiles, projectDir, branch) {
  const codeFiles = changedFiles.filter(f => isCodeFile(f) && !isDocFile(f));
  const docFiles = changedFiles.filter(f => isDocFile(f));

  if (codeFiles.length === 0) {
    return {
      ruleId: rule.id,
      ruleName: rule.name,
      principle: rule.principle,
      status: 'skip',
      message: '코드 변경 없음'
    };
  }

  // 코드 파일에 대한 관련 문서 찾기
  const needsUpdate = [];
  for (const codeFile of codeFiles) {
    const relatedDocs = findRelatedDocs(
      path.join(projectDir, codeFile),
      projectDir,
      branch
    );

    // 관련 문서 중 변경되지 않은 것 찾기
    for (const doc of relatedDocs) {
      const docPath = doc.document.replace('{branch}', branch);
      if (!docFiles.some(d => d.includes(docPath) || docPath.includes(d))) {
        needsUpdate.push({
          codeFile,
          document: docPath,
          docType: doc.docType
        });
      }
    }
  }

  // 중복 제거
  const uniqueNeeds = [];
  const seen = new Set();
  for (const item of needsUpdate) {
    const key = `${item.codeFile}:${item.document}`;
    if (!seen.has(key)) {
      seen.add(key);
      uniqueNeeds.push(item);
    }
  }

  return {
    ruleId: rule.id,
    ruleName: rule.name,
    principle: rule.principle,
    status: uniqueNeeds.length === 0 ? 'pass' : 'warn',
    codeFiles,
    docFiles,
    needsUpdate: uniqueNeeds,
    message: uniqueNeeds.length === 0
      ? rule.message.pass
      : `${rule.message.fail} (${uniqueNeeds.length}개 문서)`
  };
}

/**
 * 모든 활성화된 규칙 실행
 * @param {object} rulesConfig - 규칙 설정
 * @param {Array} changedFiles - 변경된 파일 목록
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {string} branch - 현재 브랜치
 * @returns {Array} 체크 결과 목록
 */
function runChecks(rulesConfig, changedFiles, projectDir, branch) {
  const results = [];

  for (const rule of rulesConfig.rules) {
    if (!rule.enabled) continue;

    let result;
    switch (rule.type) {
      case 'file-pair':
        result = checkFilePair(rule, changedFiles, projectDir);
        break;
      case 'content-pattern':
        result = checkContentPattern(rule, changedFiles, projectDir);
        break;
      case 'forbidden-pattern':
        result = checkForbiddenPattern(rule, changedFiles, projectDir);
        break;
      case 'file-exists':
        result = checkFileExists(rule, changedFiles, projectDir);
        break;
      case 'markdown-checklist':
        result = checkMarkdownChecklist(rule, projectDir, branch);
        break;
      case 'doc-code-sync':
        result = checkDocCodeSync(rule, changedFiles, projectDir, branch);
        break;
      default:
        continue;
    }

    results.push(result);
  }

  return results;
}

/**
 * 체크 결과 리포트 생성
 * @param {Array} results - 체크 결과 목록
 * @param {Array} manualReminders - 수동 확인 알림
 * @param {string} branch - 현재 브랜치
 * @param {number} fileCount - 변경된 파일 수
 * @returns {string} 포맷팅된 리포트
 */
function generateReport(results, manualReminders, branch, fileCount) {
  const lines = [];

  lines.push('========================================');
  lines.push('   규칙/헌법 준수 체크 결과');
  lines.push('========================================');
  lines.push('');
  lines.push(`브랜치: ${branch}`);
  lines.push(`변경 파일: ${fileCount}개`);
  lines.push('');
  lines.push('[자동 체크 결과]');
  lines.push('');

  let passCount = 0;
  let warnCount = 0;
  let failCount = 0;

  for (const result of results) {
    if (result.status === 'skip') continue;

    let icon;
    switch (result.status) {
      case 'pass':
        icon = '[v]';
        passCount++;
        break;
      case 'warn':
        icon = '[!]';
        warnCount++;
        break;
      case 'fail':
        icon = '[x]';
        failCount++;
        break;
      default:
        icon = '[-]';
    }

    lines.push(` ${icon} ${result.ruleName}`);
    lines.push(`     ${result.message}`);

    // 실패/경고 시 상세 정보
    if (result.status === 'fail' && result.failed && result.failed.length > 0) {
      for (const item of result.failed.slice(0, 3)) {
        if (item.source) {
          lines.push(`     - ${path.basename(item.source)} -> ${path.basename(item.target)}`);
        } else if (item.file) {
          lines.push(`     - ${path.basename(item.file)}`);
        }
      }
      if (result.failed.length > 3) {
        lines.push(`     ... 외 ${result.failed.length - 3}개`);
      }
    }

    if (result.needsUpdate && result.needsUpdate.length > 0) {
      for (const item of result.needsUpdate.slice(0, 3)) {
        lines.push(`     - ${path.basename(item.codeFile)} -> ${item.document}`);
      }
      if (result.needsUpdate.length > 3) {
        lines.push(`     ... 외 ${result.needsUpdate.length - 3}개`);
      }
    }

    lines.push('');
  }

  const total = passCount + warnCount + failCount;
  lines.push('----------------------------------------');
  lines.push(`${total}개 체크: ${passCount}개 통과, ${warnCount}개 주의, ${failCount}개 실패`);
  lines.push('----------------------------------------');

  // 수동 확인 권장 사항
  if (manualReminders && manualReminders.length > 0) {
    lines.push('');
    lines.push('[수동 확인 권장]');
    for (const reminder of manualReminders) {
      lines.push(`- ${reminder.reminder}`);
    }
  }

  lines.push('');
  lines.push('========================================');

  return lines.join('\n');
}

module.exports = {
  loadRulesConfig,
  loadRules,
  getChangedFiles,
  checkFilePair,
  checkContentPattern,
  checkForbiddenPattern,
  checkFileExists,
  checkMarkdownChecklist,
  checkDocCodeSync,
  runChecks,
  generateReport
};
