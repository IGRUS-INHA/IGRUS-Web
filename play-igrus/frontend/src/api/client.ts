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
  author?: string;
  thumbnailUrl?: string;
  bannerUrl?: string;
  redirectUrl?: string;
  status?: "pending" | "approved" | "rejected";
  /** 작성자가 공개를 내린 상태 (내 작품 응답에만) */
  hidden?: boolean;
  rejectReason?: string;
  reviewerName?: string;
  createdAt: string;
  reviewedAt?: string;
  /** 라이브 버전(내 작품) 또는 해당 이력의 버전 번호(검수 목록) */
  version?: number;
  /** 심사 대기/반려된 최신 제출 버전 */
  update?: Project;
}

/** 백엔드가 주는 상대 경로(/images/..)를 play-api 절대 주소로 */
export const imageSrc = (url?: string) => (url ? `${PLAY_API}${url}` : undefined);

export type SortKey = "popular" | "recent";

export const fetchProjects = (sort: SortKey = "popular", category?: string) => {
  const params = new URLSearchParams({ sort });
  if (category) params.set("category", category);
  return playFetch<Project[]>(`/api/projects?${params}`);
};

export const fetchProject = (id: number) => playFetch<Project>(`/api/projects/${id}`);

// ── 작성자 공개 프로필 ──────────────────────────────────────────────
// 닉네임 폴백은 서버가 한다 (닉네임이 있으면 실명·학번은 응답에 없다).

export interface ProfileLink {
  label: string;
  url: string;
}

export interface AuthorProfile {
  displayName: string;
  introduction?: string;
  links: ProfileLink[];
  /** 이 작성자의 승인작 목록 */
  projects?: Project[];
}

export const fetchAuthor = (projectId: number) =>
  playFetch<AuthorProfile>(`/api/projects/${projectId}/author`);

// ── 내 공개 프로필 (igrus 마이페이지) ────────────────────────────────

export interface MyPublicProfile {
  nickname?: string;
  introduction?: string;
  links?: ProfileLink[];
}

export const fetchMyProfile = () => igrusFetch<MyPublicProfile>("/api/v1/mypage/profile");

/** 닉네임/자기소개/링크를 통째로 교체 (빈 값은 비움) */
export const updateMyProfile = (profile: MyPublicProfile) =>
  igrusFetch<void>("/api/v1/mypage/profile", {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(profile),
  });

export const clickProject = (id: number) =>
  playFetch<void>(`/api/projects/${id}/click`, { method: "POST" });

export const fetchMine = () => playFetch<Project[]>("/api/projects/mine");

export const submitProject = (form: FormData) =>
  playFetch<{ id: number; status: string }>("/api/projects", { method: "POST", body: form });

/** 본인 작품 수정 — 승인작은 수정본으로 쌓여 재승인 후 반영된다 */
export const updateProject = (id: number, form: FormData) =>
  playFetch<{ id: number; status: string }>(`/api/projects/${id}`, {
    method: "PUT",
    body: form,
  });

/** 본인 승인작 공개/비공개 토글 — 재공개는 재심사 없이 즉시 반영 */
export const setProjectVisibility = (id: number, hidden: boolean) =>
  playFetch<{ hidden: boolean }>(`/api/projects/${id}/visibility`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ hidden }),
  });

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
