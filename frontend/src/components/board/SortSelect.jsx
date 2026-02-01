import { SORT_TYPE, SORT_TYPE_LABELS } from '@/constants/board';

/**
 * 정렬 선택 컴포넌트
 */
export function SortSelect({ value, onChange }) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="px-6 py-2 rounded-full border border-input bg-background text-sm font-bold transition-all"
    >
      {Object.entries(SORT_TYPE_LABELS).map(([sortValue, label]) => (
        <option key={sortValue} value={sortValue}>
          {label}
        </option>
      ))}
    </select>
  );
}
