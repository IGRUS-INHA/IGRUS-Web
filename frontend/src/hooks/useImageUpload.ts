import { useState, useCallback, useRef, useEffect } from 'react';
import type { UploadFile, UploadConfig, UploadResult } from '@/types/upload';
import { UPLOAD_STATUS } from '@/types/upload';
import { validateFiles, generateUploadId, IMAGE_UPLOAD_CONFIG } from '@/utils/upload';
import { getPresignedUploadUrl, uploadFileToS3, confirmS3Upload, UPLOAD_PURPOSE } from '@/services/uploadService';

interface UseImageUploadOptions {
  config?: UploadConfig;
  purpose?: string;
  onValidationError?: (errors: string[]) => void;
}

interface UseImageUploadReturn {
  files: UploadFile[];
  isUploading: boolean;
  addFiles: (fileList: FileList | File[]) => void;
  removeFile: (id: string) => void;
  uploadAll: () => Promise<UploadResult[]>;
  getUploadResults: () => UploadResult[];
  reset: () => void;
  setExistingItems: (items: Array<{ objectKey: string; previewUrl: string }>) => void;
}

export function useImageUpload(
  options: UseImageUploadOptions = {},
): UseImageUploadReturn {
  const {
    config = IMAGE_UPLOAD_CONFIG,
    purpose = UPLOAD_PURPOSE.POST_IMAGE,
    onValidationError,
  } = options;
  const [files, setFiles] = useState<UploadFile[]>([]);
  const filesRef = useRef<UploadFile[]>([]);
  filesRef.current = files;

  // Blob URL cleanup on unmount
  useEffect(() => {
    return () => {
      filesRef.current.forEach((f) => {
        if (f.previewUrl.startsWith('blob:')) {
          URL.revokeObjectURL(f.previewUrl);
        }
      });
    };
  }, []);

  const isUploading = files.some((f) => f.status === UPLOAD_STATUS.UPLOADING);

  const addFiles = useCallback(
    (fileList: FileList | File[]) => {
      const newFiles = Array.from(fileList);
      const currentCount = filesRef.current.length;
      const errors = validateFiles(newFiles, config, currentCount);

      if (errors.length > 0) {
        const errorMessages = [...new Set(errors.map((e) => e.reason))];
        onValidationError?.(errorMessages);

        const invalidFiles = new Set(errors.map((e) => e.file));
        const validFiles = newFiles.filter((f) => !invalidFiles.has(f));
        if (validFiles.length === 0) return;

        const uploadFiles: UploadFile[] = validFiles.map((file) => ({
          id: generateUploadId(),
          file,
          previewUrl: URL.createObjectURL(file),
          status: UPLOAD_STATUS.PENDING,
          progress: 0,
        }));
        setFiles((prev) => [...prev, ...uploadFiles]);
        return;
      }

      const uploadFiles: UploadFile[] = newFiles.map((file) => ({
        id: generateUploadId(),
        file,
        previewUrl: URL.createObjectURL(file),
        status: UPLOAD_STATUS.PENDING,
        progress: 0,
      }));
      setFiles((prev) => [...prev, ...uploadFiles]);
    },
    [config, onValidationError],
  );

  const removeFile = useCallback((id: string) => {
    setFiles((prev) => {
      const file = prev.find((f) => f.id === id);
      if (file && file.previewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(file.previewUrl);
      }
      return prev.filter((f) => f.id !== id);
    });
  }, []);

  const updateFile = useCallback(
    (id: string, updates: Partial<UploadFile>) => {
      setFiles((prev) =>
        prev.map((f) => {
          if (f.id !== id) return f;
          const updated = { ...f, ...updates };
          if (updates.status === UPLOAD_STATUS.UPLOADING) {
            delete updated.error;
          }
          return updated;
        }),
      );
    },
    [],
  );

  const uploadAll = useCallback(async (): Promise<UploadResult[]> => {
    const currentFiles = filesRef.current;
    const results: UploadResult[] = [];

    // 이미 성공한 파일의 결과도 포함
    for (const f of currentFiles) {
      if (f.status === UPLOAD_STATUS.SUCCESS && f.objectKey) {
        results.push({
          objectKey: f.objectKey,
          fileName: f.file.name,
          fileSize: f.file.size,
        });
      }
    }

    const pendingFiles = currentFiles.filter(
      (f) => f.status === UPLOAD_STATUS.PENDING,
    );

    const MAX_RETRIES = 2;
    const BACKOFF_BASE_MS = 1000; // 지수 백오프: 1s, 2s, 4s

    for (const uploadFile of pendingFiles) {
      let lastError = '';
      let succeeded = false;

      for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
        // 재시도 시 지수 백오프 대기 (첫 시도는 즉시 실행)
        if (attempt > 0) {
          const delay = BACKOFF_BASE_MS * 2 ** (attempt - 1);
          await new Promise((resolve) => setTimeout(resolve, delay));
        }

        try {
          updateFile(uploadFile.id, {
            status: UPLOAD_STATUS.UPLOADING,
            progress: 0,
          });

          // 1단계: presigned URL + objectKey 발급
          const { presignedUrl, objectKey } = await getPresignedUploadUrl(
            uploadFile.file,
            purpose,
          );

          // 2단계: S3 직접 업로드
          await uploadFileToS3(presignedUrl, uploadFile.file, (progress) => {
            updateFile(uploadFile.id, { progress });
          });

          // 3단계: 백엔드 업로드 확인
          await confirmS3Upload(objectKey);

          updateFile(uploadFile.id, {
            status: UPLOAD_STATUS.SUCCESS,
            progress: 100,
            objectKey,
          });

          results.push({
            objectKey,
            fileName: uploadFile.file.name,
            fileSize: uploadFile.file.size,
          });

          succeeded = true;
          break;
        } catch (error) {
          lastError =
            error instanceof Error ? error.message : '업로드 실패';
        }
      }

      if (!succeeded) {
        updateFile(uploadFile.id, {
          status: UPLOAD_STATUS.ERROR,
          error: lastError,
        });
      }
    }

    return results;
  }, [updateFile, purpose]);

  const getUploadResults = useCallback((): UploadResult[] => {
    return filesRef.current
      .filter((f) => f.status === UPLOAD_STATUS.SUCCESS && f.objectKey)
      .map((f) => ({
        // Exception 기록:
        // 규칙: non-null assertion 금지
        // 이유: 바로 위 filter에서 objectKey 존재를 보장
        // 범위: 이 한 줄
        // 대체안: 중복 undefined 체크는 불필요
        objectKey: f.objectKey!,
        fileName: f.file.name,
        fileSize: f.file.size,
      }));
  }, []);

  const reset = useCallback(() => {
    filesRef.current.forEach((f) => {
      if (f.previewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(f.previewUrl);
      }
    });
    setFiles([]);
  }, []);

  const setExistingItems = useCallback(
    (items: Array<{ objectKey: string; previewUrl: string }>) => {
      const existingFiles: UploadFile[] = items.map((item) => ({
        id: generateUploadId(),
        file: new File([], item.objectKey.split('/').pop() ?? 'image'),
        previewUrl: item.previewUrl,
        status: UPLOAD_STATUS.SUCCESS,
        progress: 100,
        objectKey: item.objectKey,
      }));
      setFiles(existingFiles);
    },
    [],
  );

  return {
    files,
    isUploading,
    addFiles,
    removeFile,
    uploadAll,
    getUploadResults,
    reset,
    setExistingItems,
  };
}
