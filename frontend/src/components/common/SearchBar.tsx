import { Search, X } from 'lucide-react';
import { KeyboardEvent } from 'react';

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  onSearch: (keyword: string) => void;
  className?: string;
  placeholder?: string;
  autoFocus?: boolean;
}

export default function SearchBar({
  value,
  onChange,
  onSearch,
  className = '',
  placeholder = 'Search...',
  autoFocus = false,
}: SearchBarProps) {
  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      onSearch(value);
    }
  };

  const handleClear = () => {
    onChange('');
    onSearch('');
  };

  return (
    <div className={`relative group ${className}`}>
      <Search
        className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground group-focus-within:text-primary transition"
        size={18}
      />
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className="border border-gray-400 dark:border-gray-500 rounded-full pl-s7 pr-s10 py-s3 text-base md:text-sm
                   w-full md:w-[450px] lg:w-[500px] xl:w-[600px]
                   bg-background text-foreground focus:outline-none focus:border-primary
                   transition-all focus:xl:w-[700px]"
      />
      {value && (
        <button
          type="button"
          onClick={handleClear}
          className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition"
          aria-label="검색어 지우기"
        >
          <X size={18} />
        </button>
      )}
    </div>
  );
}
