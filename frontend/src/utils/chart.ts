export interface ChartDataItem {
  name: string;
  value: number;
  color: string;
}

const CHART_COLORS = [
  "#03A69E",
  "#0088FE",
  "#FF8042",
  "#FFBB28",
  "#FF6384",
  "#66CBC5",
  "#9966FF",
  "#4BC0C0",
  "#FF9F40",
  "#C9CBCF",
];

function assignColors(
  items: { name: string; value: number }[],
): ChartDataItem[] {
  return items.map((item, i) => ({
    ...item,
    color: CHART_COLORS[i % CHART_COLORS.length] ?? "#C9CBCF",
  }));
}

const GENDER_LABELS: Record<string, string> = { MALE: "남성", FEMALE: "여성" };
const GENDER_COLORS: Record<string, string> = {
  남성: "#0088FE",
  여성: "#FF6384",
  미응답: "#C9CBCF",
};

export function aggregateByGender(
  registrations: Array<{ userGender?: string }>,
): ChartDataItem[] {
  const counts: Record<string, number> = {};
  for (const r of registrations) {
    const label = r.userGender
      ? (GENDER_LABELS[r.userGender] ?? r.userGender)
      : "미응답";
    counts[label] = (counts[label] ?? 0) + 1;
  }
  return Object.entries(counts)
    .map(([name, value]) => ({
      name,
      value,
      color: GENDER_COLORS[name] ?? "#C9CBCF",
    }))
    .sort((a, b) => b.value - a.value);
}

export function aggregateByGrade(
  registrations: Array<{ userGrade?: number }>,
): ChartDataItem[] {
  const counts: Record<string, number> = {};
  for (const r of registrations) {
    const key = r.userGrade ? `${r.userGrade}학년` : "미응답";
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return assignColors(
    Object.entries(counts)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => {
        const ga = parseInt(a.name);
        const gb = parseInt(b.name);
        if (isNaN(ga)) return 1;
        if (isNaN(gb)) return -1;
        return ga - gb;
      }),
  );
}

export function aggregateByDepartment(
  registrations: Array<{ userDepartment?: string }>,
): ChartDataItem[] {
  const counts: Record<string, number> = {};
  for (const r of registrations) {
    const key = r.userDepartment ?? "미응답";
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return assignColors(
    Object.entries(counts)
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value),
  );
}

const STATUS_LABELS: Record<string, string> = {
  REGISTERED: "등록",
  WAITING: "대기",
  APPROVED: "승인",
  REJECTED: "거절",
  CANCELED: "취소",
};
const STATUS_COLORS: Record<string, string> = {
  REGISTERED: "#03A69E",
  WAITING: "#FFBB28",
  APPROVED: "#28A745",
  REJECTED: "#DC3545",
  CANCELED: "#C9CBCF",
};

export function aggregateByStatus(
  registrations: Array<{ status?: string }>,
): ChartDataItem[] {
  const counts: Record<string, number> = {};
  for (const r of registrations) {
    const key = r.status ?? "UNKNOWN";
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return Object.entries(counts)
    .map(([key, value]) => ({
      name: STATUS_LABELS[key] ?? key,
      value,
      color: STATUS_COLORS[key] ?? "#C9CBCF",
    }))
    .sort((a, b) => b.value - a.value);
}
