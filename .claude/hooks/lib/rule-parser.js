/**
 * 규칙 파일 파서 - 마크다운에서 규칙을 동적으로 추출
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

/**
 * constitution.md에서 핵심 원칙 추출
 * @param {string} filePath - constitution.md 경로
 * @returns {Array} 파싱된 원칙 목록
 */
function parseConstitution(filePath) {
  if (!fs.existsSync(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf8');
  const principles = [];

  // ### I. 테스트 우선 개발 형식 파싱
  const principleRegex = /###\s+(I{1,3}V?|VI{0,3})\.\s+([^\n]+)\n([\s\S]*?)(?=###\s+[IVX]+\.|## |$)/g;
  let match;

  while ((match = principleRegex.exec(content)) !== null) {
    const number = match[1];
    const name = match[2].trim();
    const body = match[3].trim();

    // 원칙 내 체크포인트 추출 (- 로 시작하는 항목들)
    const checkpoints = [];
    const checkpointRegex = /^-\s+(.+)$/gm;
    let cpMatch;
    while ((cpMatch = checkpointRegex.exec(body)) !== null) {
      checkpoints.push(cpMatch[1].trim());
    }

    // 근거 추출
    const reasonMatch = body.match(/\*\*근거\*\*:\s*(.+)/);
    const reason = reasonMatch ? reasonMatch[1].trim() : '';

    principles.push({
      id: `constitution-${number}`,
      number,
      name,
      checkpoints,
      reason,
      source: 'constitution.md'
    });
  }

  return principles;
}

/**
 * CLAUDE.md에서 규칙 추출
 * @param {string} filePath - CLAUDE.md 경로
 * @returns {Array} 파싱된 규칙 목록
 */
function parseClaudeMd(filePath) {
  if (!fs.existsSync(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf8');
  const rules = [];

  // 커밋 규칙 섹션 추출
  const commitSection = content.match(/## 커밋 규칙([\s\S]*?)(?=##|$)/);
  if (commitSection) {
    const types = [];
    const typeRegex = /- `(\w+)`:\s*(.+)/g;
    let typeMatch;
    while ((typeMatch = typeRegex.exec(commitSection[1])) !== null) {
      types.push({
        type: typeMatch[1],
        description: typeMatch[2].trim()
      });
    }
    if (types.length > 0) {
      rules.push({
        id: 'commit-format',
        name: '커밋 메시지 형식',
        types,
        source: 'CLAUDE.md'
      });
    }
  }

  // 문서화 규칙 섹션 추출
  const docSection = content.match(/## 문서화([\s\S]*?)(?=##|$)/);
  if (docSection) {
    const docRules = [];
    const ruleRegex = /^-\s+(.+)$/gm;
    let ruleMatch;
    while ((ruleMatch = ruleRegex.exec(docSection[1])) !== null) {
      docRules.push(ruleMatch[1].trim());
    }
    if (docRules.length > 0) {
      rules.push({
        id: 'documentation-rules',
        name: '문서화 규칙',
        rules: docRules,
        source: 'CLAUDE.md'
      });
    }
  }

  return rules;
}

/**
 * 백엔드 CLAUDE.md에서 규칙 추출
 * @param {string} filePath - backend/CLAUDE.md 경로
 * @returns {Array} 파싱된 규칙 목록
 */
function parseBackendClaudeMd(filePath) {
  if (!fs.existsSync(filePath)) {
    return [];
  }

  const content = fs.readFileSync(filePath, 'utf8');
  const rules = [];

  // 코드 컨벤션 추출
  const conventionSection = content.match(/## 코드 컨벤션([\s\S]*?)(?=##|$)/);
  if (conventionSection) {
    rules.push({
      id: 'backend-conventions',
      name: '백엔드 코드 컨벤션',
      content: conventionSection[1].trim(),
      source: 'backend/CLAUDE.md'
    });
  }

  // 테스트 규칙 추출
  const testSection = content.match(/## 테스트([\s\S]*?)(?=##|$)/);
  if (testSection) {
    rules.push({
      id: 'backend-test-rules',
      name: '백엔드 테스트 규칙',
      content: testSection[1].trim(),
      source: 'backend/CLAUDE.md'
    });
  }

  return rules;
}

/**
 * 규칙 파일들의 해시 계산 (변경 감지용)
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {Array} ruleSources - 규칙 파일 경로 목록
 * @returns {string} 해시 값
 */
function getRuleSourcesHash(projectDir, ruleSources) {
  const hash = crypto.createHash('md5');

  for (const source of ruleSources) {
    const filePath = path.join(projectDir, source);
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, 'utf8');
      hash.update(content);
    }
  }

  return hash.digest('hex');
}

/**
 * 모든 규칙 파일에서 원칙 파싱
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {Array} ruleSources - 규칙 파일 경로 목록
 * @returns {object} 파싱된 규칙들
 */
function parseAllRuleSources(projectDir, ruleSources) {
  const result = {
    principles: [],
    rules: [],
    hash: ''
  };

  for (const source of ruleSources) {
    const filePath = path.join(projectDir, source);

    if (source.includes('constitution.md')) {
      result.principles.push(...parseConstitution(filePath));
    } else if (source === 'CLAUDE.md') {
      result.rules.push(...parseClaudeMd(filePath));
    } else if (source.includes('backend/CLAUDE.md')) {
      result.rules.push(...parseBackendClaudeMd(filePath));
    }
  }

  result.hash = getRuleSourcesHash(projectDir, ruleSources);
  return result;
}

/**
 * 파싱된 원칙을 수동 확인 알림으로 변환
 * @param {Array} principles - 파싱된 원칙 목록
 * @param {Array} existingReminders - 기존 수동 확인 알림
 * @returns {Array} 병합된 수동 확인 알림
 */
function mergeManualReminders(principles, existingReminders) {
  // 기존 알림만 반환 (JSON에 정의된 것만 사용)
  // constitution.md의 원칙은 별도로 표시하지 않음 (자동 체크에 이미 반영됨)
  return existingReminders;
}

module.exports = {
  parseConstitution,
  parseClaudeMd,
  parseBackendClaudeMd,
  getRuleSourcesHash,
  parseAllRuleSources,
  mergeManualReminders
};
