import { ChangeEvent } from "react";
import { ChevronDown } from "lucide-react";
import { SORT_TYPE_LABELS } from "@/constants/board";

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
    <div className="relative inline-block min-w-[80px] md:min-w-[100px]">
      <select
        value={value}
        onChange={handleChange}
        className="h-8 md:h-9 pl-s3 md:pl-s4 pr-8 md:pr-10 py-s1 md:py-s2 rounded-full border border-input bg-background text-xs md:text-sm font-bold transition-all appearance-none cursor-pointer w-full"
      >
        {Object.entries(SORT_TYPE_LABELS).map(([sortValue, label]) => (
          <option key={sortValue} value={sortValue}>
            {label}
          </option>
        ))}
      </select>
      <ChevronDown className="absolute right-s4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
    </div>
  );
}
