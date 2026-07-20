import { useAuthStore, type User } from "../stores/authStore";

// prod(Vercel): VITE_PLAY_API_URL=https://play-api.igrus.co.kr, VITE_IGRUS_API_URL=https://api.igrus.co.kr
// dev: 미설정 → vite proxy (/api,/images → play 로컬, /igrus-api → staging api)
const PLAY_API = import.meta.env.VITE_PLAY_API_URL ?? "";
const IGRUS_API = import.meta.env.VITE_IGRUS_API_URL ?? "/igrus-api";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// ── 세션 (www 프론트 client.ts 와 동일 패턴) ─────────────────────────
// accessToken 은 origin 별 격리지만, refresh 쿠키는 Domain=igrus.co.kr 라
// www 에서 로그인했으면 여기서도 refresh 한 번으로 세션이 복원된다.

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const res = await fetch(`${IGRUS_API}/api/v1/auth/password/refresh`, {
    method: "POST",
    credentials: "include",
  });
  if (!res.ok) throw new ApiError(res.status, "세션이 만료되었습니다");
  const data = (await res.json()) as { accessToken: string };
  useAuthStore.getState().setAccessToken(data.accessToken);
  return data.accessToken;
}

function ensureRefresh(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

/** 앱 시작 시 1회 — refresh 쿠키로 세션 복원 시도. 비로그인이면 조용히 실패. */
export async function bootstrapAuth(): Promise<void> {
  try {
    if (!useAuthStore.getState().accessToken) await ensureRefresh();
    await loadProfile();
  } catch {
    // 비로그인 방문자 — 무시
  } finally {
    useAuthStore.getState().setBootstrapped();
  }
}

export async function loadProfile(): Promise<User> {
  const user = await igrusFetch<User>("/api/v1/mypage/profile");
  useAuthStore.getState().setUser(user);
  return user;
}

export async function login(studentId: string, password: string): Promise<void> {
  const res = await fetch(`${IGRUS_API}/api/v1/auth/password/login`, {
    method: "POST",
    credentials: "include", // Set-Cookie Domain=igrus.co.kr — www 와 세션 공유
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ studentId, password }),
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { message?: string };
    throw new ApiError(res.status, body.message ?? "학번 또는 비밀번호가 올바르지 않습니다");
  }
  const data = (await res.json()) as { accessToken: string };
  useAuthStore.getState().setAccessToken(data.accessToken);
  await loadProfile().catch(() => {});
}

export async function logout(): Promise<void> {
  // 도메인 쿠키가 지워지므로 www 쪽도 함께 로그아웃된다 (SSO 정상 동작)
  await fetch(`${IGRUS_API}/api/v1/auth/password/logout`, {
    method: "POST",
    credentials: "include",
  }).catch(() => {});
  useAuthStore.getState().clear();
}

// ── fetch 코어: 401 이면 refresh 후 1회 재시도 ──────────────────────

async function coreFetch<T>(base: string, path: string, options: RequestInit = {}): Promise<T> {
  const exec = async (): Promise<Response> => {
    const token = useAuthStore.getState().accessToken;
    const headers = new Headers(options.headers);
    if (token) headers.set("Authorization", `Bearer ${token}`);
    return fetch(`${base}${path}`, { ...options, headers });
  };

  let res = await exec();
  if (res.status === 401 && useAuthStore.getState().accessToken) {
    try {
      await ensureRefresh();
    } catch {
      useAuthStore.getState().clear();
      throw new ApiError(401, "로그인이 필요합니다");
    }
    res = await exec();
  }
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { error?: string; message?: string };
    throw new ApiError(res.status, body.error ?? body.message ?? `요청 실패 (${res.status})`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

const playFetch = <T>(path: string, options?: RequestInit) => coreFetch<T>(PLAY_API, path, options);
const igrusFetch = <T>(path: string, options?: RequestInit) => coreFetch<T>(IGRUS_API, path, options);

// ── play API ────────────────────────────────────────────────────────

export interface Project {
  id: number;
  title: string;
  description: string;
  body?: string;
  category: string;
  author: string;
  thumbnailUrl?: string;
  bannerUrl?: string;
  redirectUrl?: string;
  status?: "pending" | "approved" | "rejected";
  rejectReason?: string;
  reviewerName?: string;
  totalClicks: number;
  createdAt: string;
  reviewedAt?: string;
}

/** 백엔드가 주는 상대 경로(/images/..)를 play-api 절대 주소로 */
export const imageSrc = (url?: string) => (url ? `${PLAY_API}${url}` : undefined);

export const fetchProjects = (category?: string) =>
  playFetch<Project[]>(
    `/api/projects${category ? `?category=${encodeURIComponent(category)}` : ""}`,
  );

export const fetchProject = (id: number) => playFetch<Project>(`/api/projects/${id}`);

export const clickProject = (id: number) =>
  playFetch<void>(`/api/projects/${id}/click`, { method: "POST" });

export const fetchMine = () => playFetch<Project[]>("/api/projects/mine");

export const submitProject = (form: FormData) =>
  playFetch<{ id: number; status: string }>("/api/projects", { method: "POST", body: form });

export const fetchAdminProjects = (status: string) =>
  playFetch<Project[]>(`/api/admin/projects?status=${status}`);

export const approveProject = (id: number) =>
  playFetch<{ status: string }>(`/api/admin/projects/${id}/approve`, { method: "POST" });

export const rejectProject = (id: number, reason: string) =>
  playFetch<{ status: string }>(`/api/admin/projects/${id}/reject`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reason }),
  });
