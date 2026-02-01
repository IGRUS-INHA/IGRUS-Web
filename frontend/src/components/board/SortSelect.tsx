import { ChangeEvent } from 'react';
import { SORT_TYPE, SORT_TYPE_LABELS } from '@/constants/board';

interface SortSelectProps {
  value: string;
  onChange: (value: string) => void;
}

/**
 * 정렬 선택 컴포넌트
 */
export function SortSelect({ value, onChange }: SortSelectProps) {
  const handleChange = (e: ChangeEvent<HTMLSelectElement>) => {
    onChange(e.target.value);
  };

  return (
    <select
      value={value}
      onChange={handleChange}
      className="px-s6 py-s2 rounded-full border border-input bg-background text-sm font-bold transition-all"
    >
      {Object.entries(SORT_TYPE_LABELS).map(([sortValue, label]) => (
        <option key={sortValue} value={sortValue}>
          {label}
        </option>
      ))}
    </select>
  );
}
