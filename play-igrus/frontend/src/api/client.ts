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

// ── 세션 ─────────────────────────────────────────────────────────────
// 인증은 play 백엔드의 /auth/* 프록시를 통한다. 프록시가 igrus refresh 토큰을
// "이 도메인의 퍼스트파티 쿠키"로 심기 때문에 inhaplay.com 처럼 igrus.co.kr 과
// 등록 도메인이 달라도 세션이 유지된다. www 에서 로그인한 세션은 igrus 의
// /api/v1/auth/sso/authorize 로 1회 top-level 리다이렉트(SSO 핸드오프)해 가져온다.

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const res = await fetch(`${PLAY_API}/auth/refresh`, {
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

// SSO 바운스는 탭 세션당 1회만 — 리다이렉트 루프 방지
const SSO_ATTEMPTED_KEY = "playSsoAttempted";

/** SSO 일회용 코드를 play 세션(퍼스트파티 refresh 쿠키)으로 교환.
 *  state 는 /auth/sso/start 가 심은 쿠키와 서버에서 대조된다 (로그인 CSRF 방지). */
async function exchangeSsoCode(code: string, state: string): Promise<void> {
  const res = await fetch(`${PLAY_API}/auth/sso/exchange`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code, state }),
  });
  if (!res.ok) return; // 만료/재사용 코드, state 불일치 — 비로그인으로 진행
  const data = (await res.json()) as { accessToken: string };
  useAuthStore.getState().setAccessToken(data.accessToken);
}

/** SSO 핸드오프 시작 — state 발급 후 igrus authorize 로 top-level 리다이렉트 */
async function startSsoHandoff(): Promise<void> {
  const res = await fetch(`${PLAY_API}/auth/sso/start`, {
    method: "POST",
    credentials: "include",
  });
  if (!res.ok) throw new ApiError(res.status, "SSO 시작 실패");
  const { state } = (await res.json()) as { state: string };
  const back = new URL(location.href);
  back.searchParams.set("sso_state", state);
  location.replace(
    `${IGRUS_API}/api/v1/auth/sso/authorize?redirect_uri=${encodeURIComponent(back.toString())}`,
  );
}

/**
 * 앱 시작 시 1회 — 퍼스트파티 refresh 쿠키로 세션 복원을 시도하고,
 * 실패하면 www 세션을 가져오기 위해 igrus SSO authorize 로 1회 리다이렉트한다.
 * (돌아올 때 ?sso_code= 또는 ?sso=none 이 붙는다)
 */
export async function bootstrapAuth(): Promise<void> {
  try {
    const url = new URL(location.href);
    if (
      url.searchParams.has("sso_code") ||
      url.searchParams.has("sso_state") ||
      url.searchParams.has("sso")
    ) {
      const code = url.searchParams.get("sso_code");
      const state = url.searchParams.get("sso_state");
      url.searchParams.delete("sso_code");
      url.searchParams.delete("sso_state");
      url.searchParams.delete("sso");
      history.replaceState(null, "", url); // 주소창 정리는 스토리지보다 먼저 (스토리지 예외에도 URL 은 깨끗하게)
      sessionStorage.setItem(SSO_ATTEMPTED_KEY, "1");
      if (code && state) await exchangeSsoCode(code, state);
    }
    if (!useAuthStore.getState().accessToken) await ensureRefresh();
    await loadProfile();
  } catch {
    // 비로그인 — www 에 로그인돼 있을 수 있으니 SSO 핸드오프 1회 시도
    if (!sessionStorage.getItem(SSO_ATTEMPTED_KEY)) {
      sessionStorage.setItem(SSO_ATTEMPTED_KEY, "1");
      try {
        await startSsoHandoff();
        return; // 리다이렉트로 떠남
      } catch {
        // play 서버 문제 — 비로그인으로 진행
      }
    }
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
  const res = await fetch(`${PLAY_API}/auth/login`, {
    method: "POST",
    credentials: "include", // 프록시가 refresh 토큰을 이 도메인 쿠키로 심는다
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
  // play 전용 토큰 패밀리만 종료한다 — www 세션은 영향 없음
  await fetch(`${PLAY_API}/auth/logout`, {
    method: "POST",
    credentials: "include",
  }).catch(() => {});
  sessionStorage.setItem(SSO_ATTEMPTED_KEY, "1"); // 로그아웃 직후 SSO 재바운스 방지
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
  /** 프로필 사진 (play API 가 관리) */
  avatarUrl?: string;
  /** 이 작성자의 승인작 목록 */
  projects?: Project[];
}

export const fetchAuthor = (projectId: number) =>
  playFetch<AuthorProfile>(`/api/projects/${projectId}/author`);

// ── 내 공개 프로필 (igrus 마이페이지) ────────────────────────────────

export interface MyPublicProfile {
  /** 읽기 전용 — 학번/본명은 여기서 수정할 수 없다 */
  studentId?: string;
  name?: string;
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

// ── 내 프로필 사진 (play API — 이미지 저장이 여기라서) ────────────────

export const fetchMyAvatar = () => playFetch<{ avatarUrl: string }>("/api/profile/avatar");

export const uploadMyAvatar = (file: File) => {
  const form = new FormData();
  form.append("image", file);
  return playFetch<{ avatarUrl: string }>("/api/profile/avatar", { method: "PUT", body: form });
};

export const deleteMyAvatar = () =>
  playFetch<void>("/api/profile/avatar", { method: "DELETE" });

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
