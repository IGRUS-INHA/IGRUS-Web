/**
 * 문서-코드 매핑 유틸리티
 */

const fs = require('fs');
const path = require('path');

/**
 * 문서-코드 매핑 파일 로드
 * @param {string} projectDir - 프로젝트 디렉토리
 * @returns {object|null} 매핑 데이터 또는 null
 */
function loadDocCodeMap(projectDir) {
  const mappingFile = path.join(projectDir, '.specify', 'mapping', 'doc-code-map.json');
  if (fs.existsSync(mappingFile)) {
    try {
      return JSON.parse(fs.readFileSync(mappingFile, 'utf8'));
    } catch (e) {
      return null;
    }
  }
  return null;
}

/**
 * 파일 경로와 관련된 문서 찾기
 * @param {string} filePath - 파일 절대 경로
 * @param {string} projectDir - 프로젝트 디렉토리
 * @param {string} currentBranch - 현재 브랜치 이름
 * @returns {Array} 관련 문서 목록
 */
function findRelatedDocs(filePath, projectDir, currentBranch) {
  const mapping = loadDocCodeMap(projectDir);
  if (!mapping) return [];

  // 상대 경로로 변환 (OS 독립적)
  let relativePath = filePath.replace(projectDir, '').replace(/^[/\\]/, '');
  // Windows 경로를 정규화
  relativePath = relativePath.replace(/\\/g, '/');

  const relatedDocs = [];

  for (const rule of mapping.rules || []) {
    const pattern = new RegExp(rule.pattern, 'i');
    if (pattern.test(relativePath)) {
      for (const doc of rule.documents || []) {
        // {branch} 플레이스홀더 치환
        const resolvedDoc = doc.replace('{branch}', currentBranch);
        relatedDocs.push({
          document: resolvedDoc,
          docType: rule.docType,
          description: rule.description
        });
      }
    }
  }

  return relatedDocs;
}

/**
 * 문서 파일인지 확인
 * @param {string} filePath - 파일 경로
 * @returns {boolean}
 */
function isDocFile(filePath) {
  const docPatterns = [
    /\.md$/i,
    /[/\\]docs[/\\]/i,
    /[/\\]specs[/\\]/i,
    /[/\\]\.specify[/\\]/i,
    /[/\\]\.claude[/\\]/i
  ];

  return docPatterns.some(pattern => pattern.test(filePath));
}

/**
 * 소스 코드 파일인지 확인
 * @param {string} filePath - 파일 경로
 * @returns {boolean}
 */
function isCodeFile(filePath) {
  const codeExtensions = ['.java', '.ts', '.tsx', '.js', '.jsx', '.py', '.kt', '.gradle'];
  const ext = path.extname(filePath).toLowerCase();
  return codeExtensions.includes(ext);
}

module.exports = {
  loadDocCodeMap,
  findRelatedDocs,
  isDocFile,
  isCodeFile
};
