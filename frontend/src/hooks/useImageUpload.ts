import { useState, useCallback, useRef, useEffect } from 'react';
import type { UploadFile, UploadConfig, UploadResult } from '@/types/upload';
import { UPLOAD_STATUS } from '@/types/upload';
import { validateFiles, generateUploadId, IMAGE_UPLOAD_CONFIG } from '@/utils/upload';
import { getPresignedUrl, uploadFileToS3 } from '@/services/uploadService';

interface UseImageUploadOptions {
  config?: UploadConfig;
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
  setExistingUrls: (urls: string[]) => void;
}

export function useImageUpload(
  options: UseImageUploadOptions = {},
): UseImageUploadReturn {
  const { config = IMAGE_UPLOAD_CONFIG, onValidationError } = options;
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
          // uploading 상태로 전환 시 이전 에러 메시지 제거
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
      if (f.status === UPLOAD_STATUS.SUCCESS && f.uploadedUrl) {
        results.push({
          fileUrl: f.uploadedUrl,
          fileName: f.file.name,
          fileSize: f.file.size,
        });
      }
    }

    const pendingFiles = currentFiles.filter(
      (f) => f.status === UPLOAD_STATUS.PENDING,
    );

    const MAX_RETRIES = 2;

    for (const uploadFile of pendingFiles) {
      let lastError = '';
      let succeeded = false;

      for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
        try {
          updateFile(uploadFile.id, {
            status: UPLOAD_STATUS.UPLOADING,
            progress: 0,
          });

          const { uploadUrl, fileUrl } = await getPresignedUrl(
            uploadFile.file.name,
            uploadFile.file.type,
          );

          await uploadFileToS3(uploadUrl, uploadFile.file, (progress) => {
            updateFile(uploadFile.id, { progress });
          });

          updateFile(uploadFile.id, {
            status: UPLOAD_STATUS.SUCCESS,
            progress: 100,
            uploadedUrl: fileUrl,
          });

          results.push({
            fileUrl,
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
  }, [updateFile]);

  const getUploadResults = useCallback((): UploadResult[] => {
    return filesRef.current
      .filter((f) => f.status === UPLOAD_STATUS.SUCCESS && f.uploadedUrl)
      .map((f) => ({
        // Exception 기록:
        // 규칙: non-null assertion 금지
        // 이유: 바로 위 filter에서 uploadedUrl 존재를 보장
        // 범위: 이 한 줄
        // 대체안: 중복 undefined 체크는 불필요
        fileUrl: f.uploadedUrl!,
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

  const setExistingUrls = useCallback((urls: string[]) => {
    const existingFiles: UploadFile[] = urls.map((url) => ({
      id: generateUploadId(),
      file: new File([], url.split('/').pop() ?? 'image'),
      previewUrl: url,
      status: UPLOAD_STATUS.SUCCESS,
      progress: 100,
      uploadedUrl: url,
    }));
    setFiles(existingFiles);
  }, []);

  return {
    files,
    isUploading,
    addFiles,
    removeFile,
    uploadAll,
    getUploadResults,
    reset,
    setExistingUrls,
  };
}
