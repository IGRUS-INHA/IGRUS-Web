const UTF8_BOM = "\uFEFF";

// 스프레드시트가 수식으로 해석하는 시작 문자 (CSV 수식 인젝션 방지)
const FORMULA_TRIGGER = /^[=+\-@\t\r]/;

function escapeCsvCell(value: string): string {
  const safe = FORMULA_TRIGGER.test(value) ? `'${value}` : value;
  return /[",\r\n]/.test(safe) ? `"${safe.replace(/"/g, '""')}"` : safe;
}

/**
 * 2차원 문자열 배열을 엑셀 호환 CSV 문자열로 변환한다.
 * - UTF-8 BOM 포함 (엑셀 한글 깨짐 방지)
 * - 쉼표/따옴표/개행 이스케이프
 * - `= + - @` 탭/CR로 시작하는 값은 앞에 `'`를 붙여 수식 실행을 막는다
 */
export function toCsv(rows: string[][]): string {
  return (
    UTF8_BOM + rows.map((row) => row.map(escapeCsvCell).join(",")).join("\r\n")
  );
}

/**
 * CSV 문자열을 파일로 내려받는다.
 */
export function downloadCsv(fileName: string, csv: string): void {
  const url = URL.createObjectURL(
    new Blob([csv], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
