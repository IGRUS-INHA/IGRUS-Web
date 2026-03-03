import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { getImageDownloadUrl } from "@/services/uploadService";

/** presigned download URL 캐싱 시간 (50분) — 백엔드 만료 1시간 기준 10분 여유 */
const STALE_TIME = 50 * 60 * 1000;

/**
 * objectKey 배열을 presigned download URL로 변환하는 훅.
 * useQueries로 각 objectKey마다 병렬 쿼리 실행.
 *
 * 호출부에서 useMemo로 objectKeys 배열 참조 안정성을 보장할 것:
 * ```
 * const objectKeys = useMemo(() => post.imageUrls ?? [], [post.imageUrls]);
 * const { urls, isLoading } = useResolvedImageUrls(objectKeys);
 * ```
 */
export function useResolvedImageUrls(objectKeys: string[]) {
  const queries = useMemo(
    () =>
      objectKeys.map((key) => ({
        queryKey: ["/api/v1/storage/download-url", { objectKey: key }] as const,
        queryFn: async () => {
          // 이미 URL이면 그대로 반환 (하위 호환)
          if (key.startsWith("http")) return key;
          return getImageDownloadUrl(key);
        },
        staleTime: STALE_TIME,
        retry: 3,
        enabled: key.length > 0,
      })),
    [objectKeys],
  );

  const results = useQueries({ queries });

  const urls = useMemo(() => {
    const map = new Map<string, string>();
    results.forEach((result, index) => {
      const key = objectKeys[index];
      if (result.data && key) {
        map.set(key, result.data);
      }
    });
    return map;
  }, [results, objectKeys]);

  const isLoading = results.some((r) => r.isLoading);
  const hasError = results.some((r) => r.isError);

  return { urls, isLoading, hasError };
}
