import { cn } from '@/lib/utils';

/**
 * 로딩 스피너
 * @param {string} size - 'sm' | 'md' | 'lg'
 * @param {string} className - 추가 클래스
 */
export function Spinner({ size = 'md', className }) {
  const sizeClasses = {
    sm: 'h-4 w-4 border-2',
    md: 'h-6 w-6 border-2',
    lg: 'h-8 w-8 border-3',
  };

  return (
    <div
      className={cn(
        'animate-spin rounded-full border-muted-foreground/30 border-t-primary',
        sizeClasses[size],
        className
      )}
    />
  );
}

/**
 * 전체 화면 로딩
 */
export function FullPageSpinner() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <Spinner size="lg" />
    </div>
  );
}

/**
 * 버튼 내부 로딩 (텍스트와 함께)
 */
export function ButtonSpinner({ className }) {
  return <Spinner size="sm" className={cn('mr-2', className)} />;
}
