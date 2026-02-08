import { useState, FormEvent, ChangeEvent } from 'react';
import { Search, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SEARCH_TYPE, SEARCH_TYPE_LABELS } from '@/constants/board';

interface SearchBarProps {
  onSearch: (keyword: string, searchType: string) => void;
  onClear?: () => void;
  initialKeyword?: string;
  initialSearchType?: string;
  placeholder?: string;
}

/**
 * 검색바 컴포넌트
 */
export function SearchBar({
  onSearch,
  onClear,
  initialKeyword = '',
  initialSearchType = SEARCH_TYPE.TITLE_CONTENT,
  placeholder = '검색어를 입력하세요',
}: SearchBarProps) {
  const [keyword, setKeyword] = useState(initialKeyword);
  const [searchType, setSearchType] = useState(initialSearchType);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch(keyword.trim(), searchType);
    }
  };

  const handleClear = () => {
    setKeyword('');
    onClear?.();
  };

  const handleKeywordChange = (e: ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
  };

  const handleSearchTypeChange = (e: ChangeEvent<HTMLSelectElement>) => {
    setSearchType(e.target.value);
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-s2">
      {/* 검색 타입 선택 */}
      <select
        value={searchType}
        onChange={handleSearchTypeChange}
        className="h-9 rounded-r2 border border-input bg-background px-s3 text-sm"
      >
        {Object.entries(SEARCH_TYPE_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      {/* 검색어 입력 */}
      <div className="relative flex-1">
        <Search className="absolute left-s3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          value={keyword}
          onChange={handleKeywordChange}
          placeholder={placeholder}
          className="pl-9 pr-9"
        />
        {keyword && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* 검색 버튼 */}
      <Button type="submit">검색</Button>
    </form>
  );
}

interface SimpleSearchBarProps {
  onSearch: (keyword: string) => void;
  onClear?: () => void;
  initialKeyword?: string;
}

/**
 * 간단한 검색바 (검색 타입 없음)
 */
export function SimpleSearchBar({
  onSearch,
  onClear,
  initialKeyword = '',
}: SimpleSearchBarProps) {
  const [keyword, setKeyword] = useState(initialKeyword);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch(keyword.trim());
    }
  };

  const handleKeywordChange = (e: ChangeEvent<HTMLInputElement>) => {
    setKeyword(e.target.value);
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-s2">
      <div className="relative flex-1">
        <Search className="absolute left-s3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          value={keyword}
          onChange={handleKeywordChange}
          placeholder="검색"
          className="pl-9"
        />
      </div>
      <Button type="submit" size="icon" variant="outline">
        <Search className="h-4 w-4" />
      </Button>
    </form>
  );
}
