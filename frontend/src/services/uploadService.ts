import type { PresignedUrlResponse } from '@/types/upload';

/**
 * 백엔드에서 presigned URL을 발급받는다.
 *
 * TODO: 백엔드 엔드포인트 추가 시 이 함수만 Orval 호출로 교체하면 됨.
 * 예상 엔드포인트: POST /api/v1/uploads/presigned-url
 * 예상 요청: { fileName: string, contentType: string }
 * 예상 응답: { uploadUrl: string, fileUrl: string }
 */
export async function getPresignedUrl(
  fileName: string,
  _contentType: string,
): Promise<PresignedUrlResponse> {
  const mockUploadUrl = import.meta.env.VITE_MOCK_UPLOAD_URL;

  if (mockUploadUrl) {
    const fileKey = `uploads/${Date.now()}-${fileName}`;
    return {
      uploadUrl: `${mockUploadUrl}/${fileKey}`,
      fileUrl: `${mockUploadUrl}/${fileKey}`,
    };
  }

  throw new Error(
    'Presigned URL 엔드포인트가 아직 준비되지 않았습니다. ' +
      '개발 테스트를 위해 VITE_MOCK_UPLOAD_URL 환경변수를 설정하세요.',
  );
}

/**
 * Presigned URL을 사용하여 S3에 직접 업로드한다.
 * fetch는 upload progress를 지원하지 않으므로 XMLHttpRequest 사용.
 * CLAUDE.md: fetch는 외부 API에서만 허용 — S3는 외부이므로 OK.
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
