import { useState } from 'react';
import { Search, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { SEARCH_TYPE, SEARCH_TYPE_LABELS } from '@/constants/board';

/**
 * 검색바 컴포넌트
 */
export function SearchBar({
  onSearch,
  onClear,
  initialKeyword = '',
  initialSearchType = SEARCH_TYPE.TITLE_CONTENT,
  placeholder = '검색어를 입력하세요',
}) {
  const [keyword, setKeyword] = useState(initialKeyword);
  const [searchType, setSearchType] = useState(initialSearchType);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch(keyword.trim(), searchType);
    }
  };

  const handleClear = () => {
    setKeyword('');
    onClear?.();
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      {/* 검색 타입 선택 */}
      <select
        value={searchType}
        onChange={(e) => setSearchType(e.target.value)}
        className="h-9 rounded-md border border-input bg-background px-3 text-sm"
      >
        {Object.entries(SEARCH_TYPE_LABELS).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>

      {/* 검색어 입력 */}
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder={placeholder}
          className="pl-9 pr-9"
        />
        {keyword && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
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

/**
 * 간단한 검색바 (검색 타입 없음)
 */
export function SimpleSearchBar({ onSearch, onClear, initialKeyword = '' }) {
  const [keyword, setKeyword] = useState(initialKeyword);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch(keyword.trim());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          type="text"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
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
