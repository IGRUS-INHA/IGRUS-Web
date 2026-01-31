export const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8080";

interface CustomFetchConfig {
  url: string;
  method: string;
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

export const customFetch = async <T>({
  url,
  method,
  params,
  data,
  headers,
  signal,
}: CustomFetchConfig): Promise<T> => {
  const queryString = params
    ? `?${new URLSearchParams(params as Record<string, string>).toString()}`
    : "";

  const response = await fetch(`${API_BASE_URL}${url}${queryString}`, {
    method,
    signal,
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: data ? JSON.stringify(data) : undefined,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new Error(
      (errorBody as { message?: string }).message || `HTTP ${response.status}`,
    );
  }

  return response.json();
};
