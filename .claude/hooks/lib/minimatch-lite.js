/**
 * 경량 글로브 패턴 매칭 유틸리티
 * 외부 의존성 없이 기본적인 글로브 패턴 지원
 */

/**
 * 글로브 패턴을 정규식으로 변환
 * @param {string} pattern - 글로브 패턴
 * @returns {RegExp} 정규식
 */
function globToRegex(pattern) {
  // Windows 경로 정규화
  pattern = pattern.replace(/\\/g, '/');

  let regex = '';
  let i = 0;

  while (i < pattern.length) {
    const char = pattern[i];

    if (char === '*') {
      if (pattern[i + 1] === '*') {
        // ** - 모든 디렉토리 매치
        if (pattern[i + 2] === '/') {
          regex += '(?:.*/)?';
          i += 3;
        } else {
          regex += '.*';
          i += 2;
        }
      } else {
        // * - 단일 세그먼트 매치 (/ 제외)
        regex += '[^/]*';
        i++;
      }
    } else if (char === '?') {
      // ? - 단일 문자 매치
      regex += '[^/]';
      i++;
    } else if (char === '{') {
      // {a,b,c} - 대안 그룹
      const end = pattern.indexOf('}', i);
      if (end !== -1) {
        const alternatives = pattern.slice(i + 1, end).split(',');
        regex += '(?:' + alternatives.map(escapeRegex).join('|') + ')';
        i = end + 1;
      } else {
        regex += escapeRegex(char);
        i++;
      }
    } else if (char === '[') {
      // [abc] - 문자 클래스
      const end = pattern.indexOf(']', i);
      if (end !== -1) {
        regex += pattern.slice(i, end + 1);
        i = end + 1;
      } else {
        regex += escapeRegex(char);
        i++;
      }
    } else {
      // 일반 문자 - 이스케이프
      regex += escapeRegex(char);
      i++;
    }
  }

  return new RegExp('^' + regex + '$', 'i');
}

/**
 * 정규식 특수문자 이스케이프
 * @param {string} str - 이스케이프할 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeRegex(str) {
  return str.replace(/[.+^${}()|[\]\\]/g, '\\$&');
}

/**
 * 파일 경로가 글로브 패턴과 매치되는지 확인
 * @param {string} filePath - 파일 경로
 * @param {string} pattern - 글로브 패턴
 * @returns {boolean} 매치 여부
 */
function minimatch(filePath, pattern) {
  // 경로 정규화
  filePath = filePath.replace(/\\/g, '/');
  pattern = pattern.replace(/\\/g, '/');

  // 패턴이 /로 시작하지 않으면 어디서든 매치 가능
  if (!pattern.startsWith('/') && !pattern.startsWith('**/')) {
    pattern = '**/' + pattern;
  }

  const regex = globToRegex(pattern);
  return regex.test(filePath);
}

module.exports = minimatch;
module.exports.globToRegex = globToRegex;
