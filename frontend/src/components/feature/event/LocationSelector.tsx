import { useState, useRef, useEffect } from "react";
import { ChevronDown, Search } from "lucide-react";
import { EVENT_LOCATIONS } from "@/constants/event";
import { cn } from "@/lib/utils";

interface LocationSelectorProps {
  selectedPreset: string;
  detail: string;
  onPresetChange: (value: string) => void;
  onDetailChange: (value: string) => void;
  error?: string;
}

const DIRECT_INPUT_VALUE = "__direct__";

function getDetailPlaceholder(preset: string): string {
  if (preset === "온라인") return "예: Zoom 링크, Discord 등";
  if (preset === DIRECT_INPUT_VALUE || preset === "")
    return "장소를 직접 입력하세요";
  return "예: 208호";
}

export function LocationSelector({
  selectedPreset,
  detail,
  onPresetChange,
  onDetailChange,
  error,
}: LocationSelectorProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // 외부 클릭 시 닫기
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const filtered = EVENT_LOCATIONS.filter((loc) =>
    loc.toLowerCase().includes(search.toLowerCase()),
  );

  const displayValue =
    selectedPreset === DIRECT_INPUT_VALUE ? "직접 입력" : selectedPreset || "";

  function handleSelect(value: string) {
    onPresetChange(value);
    onDetailChange("");
    setSearch("");
    setIsOpen(false);
  }

  const showDetail = selectedPreset !== "";

  return (
    <div className="space-y-s3">
      {/* 검색 가능 드롭다운 */}
      <div ref={containerRef} className="relative">
        <button
          type="button"
          onClick={() => {
            setIsOpen(!isOpen);
            if (!isOpen) {
              setSearch("");
              setTimeout(() => inputRef.current?.focus(), 0);
            }
          }}
          className={cn(
            "w-full rounded-r3 px-s4 py-s3 border bg-muted/50 text-sm text-left flex items-center justify-between cursor-pointer transition-colors",
            "focus:outline-none focus:border-primary",
            isOpen && "border-primary",
            error && !selectedPreset && "border-destructive",
            selectedPreset
              ? "text-foreground border-border"
              : "text-muted-foreground border-border",
          )}
        >
          <span>{displayValue || "장소를 선택하세요"}</span>
          <ChevronDown
            size={16}
            className={cn(
              "text-muted-foreground transition-transform",
              isOpen && "rotate-180",
            )}
          />
        </button>

        {isOpen && (
          <div className="absolute z-20 mt-s1 w-full rounded-r3 border border-border bg-card shadow-lg max-h-60 overflow-hidden">
            {/* 검색 입력 */}
            <div className="flex items-center gap-s2 px-s3 py-s2 border-b border-border">
              <Search size={14} className="text-muted-foreground shrink-0" />
              <input
                ref={inputRef}
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="장소 검색..."
                className="w-full bg-transparent text-sm focus:outline-none placeholder:text-muted-foreground/50"
              />
            </div>

            {/* 옵션 목록 */}
            <div className="overflow-y-auto max-h-48">
              {filtered.map((loc) => (
                <button
                  key={loc}
                  type="button"
                  onClick={() => handleSelect(loc)}
                  className={cn(
                    "w-full text-left px-s4 py-s2 text-sm transition-colors cursor-pointer hover:bg-accent",
                    selectedPreset === loc &&
                      "bg-primary/5 text-primary font-medium",
                  )}
                >
                  {loc}
                </button>
              ))}

              {filtered.length === 0 && (
                <p className="px-s4 py-s2 text-sm text-muted-foreground">
                  검색 결과가 없습니다
                </p>
              )}

              {/* 직접 입력 옵션 */}
              <button
                type="button"
                onClick={() => handleSelect(DIRECT_INPUT_VALUE)}
                className={cn(
                  "w-full text-left px-s4 py-s2 text-sm transition-colors cursor-pointer hover:bg-accent border-t border-border",
                  selectedPreset === DIRECT_INPUT_VALUE &&
                    "bg-primary/5 text-primary font-medium",
                )}
              >
                직접 입력
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 상세 입력 */}
      {showDetail && (
        <input
          type="text"
          value={detail}
          onChange={(e) => onDetailChange(e.target.value)}
          placeholder={getDetailPlaceholder(selectedPreset)}
          className={cn(
            "w-full rounded-r3 px-s4 py-s3 border bg-muted/50 border-border text-sm",
            "focus:outline-none focus:border-primary",
            error &&
              selectedPreset === DIRECT_INPUT_VALUE &&
              !detail.trim() &&
              "border-destructive",
          )}
        />
      )}

      {error && <p className="typo-c1 text-destructive mt-s1">{error}</p>}
    </div>
  );
}

export { DIRECT_INPUT_VALUE };
