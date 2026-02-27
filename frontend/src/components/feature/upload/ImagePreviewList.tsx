import { X, AlertCircle, CheckCircle2 } from 'lucide-react';
import type { UploadFile } from '@/types/upload';
import { UPLOAD_STATUS } from '@/types/upload';
import { formatFileSize } from '@/utils/upload';
import { cn } from '@/lib/utils';

interface ImagePreviewListProps {
  files: UploadFile[];
  onRemove: (id: string) => void;
  className?: string;
}

export default function ImagePreviewList({
  files,
  onRemove,
  className,
}: ImagePreviewListProps) {
  if (files.length === 0) return undefined;

  return (
    <div
      className={cn(
        'grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-s3',
        className,
      )}
    >
      {files.map((file) => (
        <div
          key={file.id}
          className="group relative rounded-r2 border border-border overflow-hidden bg-muted/30"
        >
          {/* 썸네일 */}
          <div className="aspect-square">
            <img
              src={file.previewUrl}
              alt={file.file.name}
              className="h-full w-full object-cover"
            />
          </div>

          {/* 업로드 상태 오버레이 */}
          {file.status === UPLOAD_STATUS.UPLOADING && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/40">
              <div className="w-3/4">
                <div className="h-1.5 w-full rounded-full bg-white/30">
                  <div
                    className="h-full rounded-full bg-white transition-all"
                    style={{ width: `${file.progress}%` }}
                  />
                </div>
                <span className="mt-s1 block text-center text-xs text-white">
                  {file.progress}%
                </span>
              </div>
            </div>
          )}

          {file.status === UPLOAD_STATUS.ERROR && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/50 p-s2">
              <AlertCircle size={20} className="text-destructive" />
              <span className="mt-s1 text-center text-xs text-white line-clamp-2">
                {file.error}
              </span>
            </div>
          )}

          {file.status === UPLOAD_STATUS.SUCCESS && (
            <div className="absolute bottom-s1 left-s1">
              <CheckCircle2
                size={16}
                className="text-green-500 drop-shadow"
              />
            </div>
          )}

          {/* 파일 정보 */}
          <div className="px-s2 py-s1">
            <p className="truncate text-xs text-muted-foreground">
              {file.file.name}
            </p>
            <p className="text-xs text-muted-foreground/70">
              {formatFileSize(file.file.size)}
            </p>
          </div>

          {/* 제거 버튼 */}
          <button
            type="button"
            onClick={() => onRemove(file.id)}
            className={cn(
              'absolute top-s1 right-s1 rounded-full p-0.5 transition cursor-pointer',
              'bg-black/50 text-white hover:bg-black/70',
              'opacity-0 group-hover:opacity-100',
            )}
          >
            <X size={14} />
          </button>
        </div>
      ))}
    </div>
  );
}
