/**
 * 날짜 포맷팅 유틸리티
 * Intl.DateTimeFormat (toLocaleDateString/toLocaleTimeString) 기반
 */

/**
 * ISO 문자열 → "2026년 3월 15일"
 */
export function formatDate(isoString?: string): string {
  if (!isoString) return 'TBD';
  try {
    return new Date(isoString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return isoString;
  }
}

/**
 * ISO 문자열 → "오후 02:00"
 */
export function formatTime(isoString?: string): string {
  if (!isoString) return 'TBD';
  try {
    return new Date(isoString).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}

/**
 * ISO 문자열 → { date: "2026년 3월 15일", time: "오후 02:00" }
 */
export function formatDateTime(isoString?: string): { date: string; time: string } {
  return {
    date: formatDate(isoString),
    time: formatTime(isoString),
  };
}
