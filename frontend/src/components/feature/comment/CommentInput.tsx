import { useState } from 'react';
import { Send } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useUIStore } from '@/stores';

interface CommentInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: (content: string, anonymous: boolean) => void;
  placeholder?: string;
  isSubmitting?: boolean;
  autoFocus?: boolean;
}

/**
 * 댓글/답글 입력 컴포넌트
 * - 익명 체크박스 포함
 * - 500자 제한
 * - Enter 키 제출 지원
 */
export function CommentInput({
  value,
  onChange,
  onSubmit,
  placeholder = '댓글을 입력하세요...',
  isSubmitting = false,
  autoFocus = false,
}: CommentInputProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [isAnonymous, setIsAnonymous] = useState(false);

  const handleSubmit = () => {
    if (!value.trim() || isSubmitting) return;
    onSubmit(value.trim(), isAnonymous);
    setIsAnonymous(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const remainingChars = 500 - value.length;
  const isOverLimit = remainingChars < 0;

  return (
    <div className="space-y-s2">
      <div className="relative">
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={isSubmitting}
          autoFocus={autoFocus}
          className={cn(
            'w-full rounded-r4 px-s5 py-s3 pr-s7 border focus:outline-none focus:border-primary transition-all',
            isDark ? 'bg-white/5 border-border' : 'bg-muted/50 border-border',
            isSubmitting && 'opacity-50 cursor-not-allowed',
            isOverLimit && 'border-red-500 focus:border-red-500'
          )}
        />
        <button
          onClick={handleSubmit}
          type="button"
          disabled={isSubmitting || !value.trim() || isOverLimit}
          className={cn(
            'absolute right-s2 top-1/2 -translate-y-1/2 p-s2 rounded-r2 transition cursor-pointer',
            'text-primary hover:bg-primary/10',
            (isSubmitting || !value.trim() || isOverLimit) &&
              'opacity-50 cursor-not-allowed hover:bg-transparent'
          )}
        >
          <Send size={18} />
        </button>
      </div>

      <div className="flex items-center justify-between px-s1">
        <label className="flex items-center gap-s2 cursor-pointer">
          <input
            type="checkbox"
            checked={isAnonymous}
            onChange={(e) => setIsAnonymous(e.target.checked)}
            disabled={isSubmitting}
            className="cursor-pointer"
          />
          <span className="text-c1 text-muted-foreground">익명</span>
        </label>

        <span
          className={cn(
            'text-c1',
            isOverLimit ? 'text-red-500' : 'text-muted-foreground'
          )}
        >
          {remainingChars} / 500
        </span>
      </div>
    </div>
  );
}
