/**
 * Git 관련 공통 유틸리티
 */

const { execSync } = require('child_process');

/**
 * 현재 Git 브랜치 이름을 반환
 * @param {string} cwd - 작업 디렉토리
 * @returns {string} 브랜치 이름 또는 'unknown'
 */
function getCurrentBranch(cwd) {
  try {
    return execSync('git rev-parse --abbrev-ref HEAD', {
      cwd,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    }).trim();
  } catch (e) {
    return 'unknown';
  }
}

/**
 * Feature 브랜치인지 확인 (000-feature-name 패턴)
 * @param {string} branch - 브랜치 이름
 * @returns {boolean}
 */
function isFeatureBranch(branch) {
  return /^\d{3}-/.test(branch);
}

/**
 * 프로젝트 디렉토리 결정
 * @param {object} input - Hook 입력 데이터
 * @returns {string} 프로젝트 디렉토리 경로
 */
function getProjectDir(input = {}) {
  return process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd();
}

module.exports = {
  getCurrentBranch,
  isFeatureBranch,
  getProjectDir
};
