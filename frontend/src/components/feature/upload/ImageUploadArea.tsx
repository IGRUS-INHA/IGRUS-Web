import { useRef, useState, useCallback } from 'react';
import { ImagePlus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ImageUploadAreaProps {
  onFilesSelected: (files: FileList) => void;
  maxFiles: number;
  currentCount: number;
  accept?: string;
  disabled?: boolean;
  className?: string;
}

export default function ImageUploadArea({
  onFilesSelected,
  maxFiles,
  currentCount,
  accept = 'image/*',
  disabled = false,
  className,
}: ImageUploadAreaProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);

  const isFull = currentCount >= maxFiles;

  const handleClick = () => {
    if (!disabled && !isFull) {
      inputRef.current?.click();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      onFilesSelected(e.target.files);
      // input 초기화 (같은 파일 재선택 허용)
      e.target.value = '';
    }
  };

  const handleDragOver = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      if (!disabled && !isFull) {
        setIsDragOver(true);
      }
    },
    [disabled, isFull],
  );

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(false);
      if (!disabled && !isFull && e.dataTransfer.files.length > 0) {
        onFilesSelected(e.dataTransfer.files);
      }
    },
    [disabled, isFull, onFilesSelected],
  );

  return (
    <div className={className}>
      <button
        type="button"
        onClick={handleClick}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        disabled={disabled || isFull}
        className={cn(
          'w-full rounded-r3 border-2 border-dashed p-s5 transition-colors cursor-pointer',
          'flex flex-col items-center justify-center gap-s2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          isDragOver
            ? 'border-primary bg-primary/5'
            : 'border-border hover:border-primary/50 hover:bg-muted/50',
        )}
      >
        <ImagePlus size={24} className="text-muted-foreground" />
        <span className="typo-c1 text-muted-foreground">
          {isFull
            ? `최대 ${maxFiles}개 첨부됨`
            : `이미지를 선택하거나 드래그하세요 (${currentCount}/${maxFiles})`}
        </span>
      </button>

      <input
        ref={inputRef}
        type="file"
        accept={accept}
        multiple
        onChange={handleChange}
        className="hidden"
      />
    </div>
  );
}
