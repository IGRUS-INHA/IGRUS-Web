/** 업로드 상태 */
export const UPLOAD_STATUS = {
  PENDING: "pending",
  UPLOADING: "uploading",
  SUCCESS: "success",
  ERROR: "error",
} as const;

export type UploadStatus = (typeof UPLOAD_STATUS)[keyof typeof UPLOAD_STATUS];

/** 업로드 라이프사이클을 추적하는 개별 파일 */
export interface UploadFile {
  /** React key 및 상태 관리용 고유 ID */
  id: string;
  /** 브라우저 File 객체 */
  file: File;
  /** 미리보기용 blob URL (URL.createObjectURL) */
  previewUrl: string;
  /** 현재 업로드 상태 */
  status: UploadStatus;
  /** 업로드 진행률 0-100 */
  progress: number;
  /** 업로드 완료 후 S3 object key */
  objectKey?: string;
  /** 업로드 실패 시 에러 메시지 */
  error?: string;
}

/** 업로드 제한 설정 */
export interface UploadConfig {
  /** 최대 파일 개수 (undefined이면 무제한) */
  maxFiles?: number;
  /** 최대 파일 크기 (bytes) */
  maxFileSize: number;
  /** 허용 MIME 타입 */
  acceptedTypes: string[];
}

/** 완료된 업로드 결과 (폼 제출용) */
export interface UploadResult {
  /** S3 object key */
  objectKey: string;
  /** 원본 파일명 */
  fileName: string;
  /** 파일 크기 (bytes) */
  fileSize: number;
}
