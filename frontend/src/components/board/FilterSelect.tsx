import { ChangeEvent } from 'react';
import { ChevronDown } from 'lucide-react';

interface FilterSelectProps {
  value: string;
  onChange: (value: string) => void;
  options: Record<string, string>;
}

/**
 * 필터 선택 컴포넌트
 * 범용적으로 사용 가능한 필터 드롭다운
 */
export function FilterSelect({ value, onChange, options }: FilterSelectProps) {
  const handleChange = (e: ChangeEvent<HTMLSelectElement>) => {
    onChange(e.target.value);
  };

  return (
    <div className="relative inline-block min-w-[100px]">
      <select
        value={value}
        onChange={handleChange}
        className="h-9 pl-s4 pr-s7 py-s2 rounded-full border border-input bg-background text-sm font-bold transition-all appearance-none cursor-pointer w-full"
      >
        {Object.entries(options).map(([filterValue, label]) => (
          <option key={filterValue} value={filterValue}>
            {label}
          </option>
        ))}
      </select>
      <ChevronDown className="absolute right-s4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
    </div>
  );
}
