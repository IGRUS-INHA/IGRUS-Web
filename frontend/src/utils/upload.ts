import type { UploadConfig } from '@/types/upload';

/** 게시글 이미지 업로드 설정 */
export const IMAGE_UPLOAD_CONFIG: UploadConfig = {
  maxFiles: 5,
  maxFileSize: 10 * 1024 * 1024, // 10MB
  acceptedTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
};

/** 문의 첨부파일 업로드 설정 */
export const INQUIRY_ATTACHMENT_CONFIG: UploadConfig = {
  maxFiles: 3,
  maxFileSize: 10 * 1024 * 1024, // 10MB
  acceptedTypes: ['image/jpeg', 'image/png', 'image/gif', 'image/webp'],
};

export interface FileValidationError {
  file: File;
  reason: string;
}

/**
 * 파일 목록을 업로드 설정에 맞게 검증한다.
 * 에러 배열이 비어있으면 모두 유효.
 */
export function validateFiles(
  files: File[],
  config: UploadConfig,
  currentCount: number,
): FileValidationError[] {
  const errors: FileValidationError[] = [];

  const allowable = Math.max(0, config.maxFiles - currentCount);
  if (files.length > allowable) {
    files.slice(allowable).forEach((file) => {
      errors.push({
        file,
        reason: `최대 ${config.maxFiles}개까지 업로드 가능합니다`,
      });
    });
  }

  for (const file of files) {
    if (!config.acceptedTypes.includes(file.type)) {
      errors.push({
        file,
        reason: `지원하지 않는 파일 형식입니다: ${file.type || '알 수 없음'}`,
      });
    }

    if (file.size > config.maxFileSize) {
      const maxMB = config.maxFileSize / (1024 * 1024);
      errors.push({
        file,
        reason: `파일 크기가 ${maxMB}MB를 초과합니다`,
      });
    }
  }

  return errors;
}

/** 파일 크기를 읽기 좋은 형태로 포맷 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

/** 업로드 파일 추적용 고유 ID 생성 */
export function generateUploadId(): string {
  return crypto.randomUUID();
}
