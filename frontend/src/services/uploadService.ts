import { createUploadUrl, confirmUpload, createDownloadUrl } from '@/api/model/storage/storage';

/** 업로드 용도 상수 */
export const UPLOAD_PURPOSE = {
  POST_IMAGE: 'POST_IMAGE',
  INQUIRY_ATTACHMENT: 'INQUIRY_ATTACHMENT',
} as const;

/**
 * 백엔드에서 presigned upload URL을 발급받는다.
 * Mock 모드: VITE_MOCK_UPLOAD_URL 설정 시 stub 동작.
 */
export async function getPresignedUploadUrl(
  file: File,
  purpose: string,
): Promise<{ presignedUrl: string; objectKey: string }> {
  const mockUploadUrl = import.meta.env.VITE_MOCK_UPLOAD_URL;

  if (mockUploadUrl) {
    const objectKey = `uploads/${Date.now()}-${file.name}`;
    return {
      presignedUrl: `${mockUploadUrl}/${objectKey}`,
      objectKey,
    };
  }

  const response = await createUploadUrl({
    fileName: file.name,
    contentType: file.type,
    fileSize: file.size,
    purpose,
  });

  if (response.status !== 200 || !response.data.presignedUrl || !response.data.objectKey) {
    throw new Error('Presigned URL 발급에 실패했습니다');
  }

  return {
    presignedUrl: response.data.presignedUrl,
    objectKey: response.data.objectKey,
  };
}

/**
 * Presigned URL을 사용하여 S3에 직접 업로드한다.
 * fetch는 upload progress를 지원하지 않으므로 XMLHttpRequest 사용.
 */
export function uploadFileToS3(
  presignedUrl: string,
  file: File,
  onProgress?: (progress: number) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', presignedUrl);
    xhr.setRequestHeader('Content-Type', file.type);

    xhr.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable && onProgress) {
        const percent = Math.round((event.loaded / event.total) * 100);
        onProgress(percent);
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve();
      } else {
        reject(new Error(`업로드 실패 (HTTP ${xhr.status})`));
      }
    });

    xhr.addEventListener('error', () => {
      reject(new Error('네트워크 오류로 업로드에 실패했습니다'));
    });

    xhr.send(file);
  });
}

/**
 * S3 업로드 완료 후 백엔드에 확인 요청.
 * Mock 모드: skip (바로 반환).
 */
export async function confirmS3Upload(objectKey: string): Promise<void> {
  const mockUploadUrl = import.meta.env.VITE_MOCK_UPLOAD_URL;
  if (mockUploadUrl) return;

  const response = await confirmUpload({ objectKey });

  if (response.status !== 200) {
    throw new Error(
      `업로드 확인 실패: ${response.data.reason ?? `HTTP ${response.status}`}`,
    );
  }
}

/**
 * objectKey로 presigned download URL을 발급받는다.
 */
export async function getImageDownloadUrl(objectKey: string): Promise<string> {
  const mockUploadUrl = import.meta.env.VITE_MOCK_UPLOAD_URL;
  if (mockUploadUrl) {
    return `${mockUploadUrl}/${objectKey}`;
  }

  const response = await createDownloadUrl({ objectKey });

  if (response.status !== 200 || !response.data.presignedUrl) {
    throw new Error('다운로드 URL 발급에 실패했습니다');
  }

  return response.data.presignedUrl;
}
