import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthStore, AuthPersistState } from "@/types/store";
import type { User } from "@/types/entities";
import { ROLES, type Role } from "@/types/common";
import { queryClient } from "@/lib/queryClient";

const ROLE_ORDER: readonly Role[] = [
  ROLES.ASSOCIATE,
  ROLES.MEMBER,
  ROLES.OPERATOR,
  ROLES.ADMIN,
] as const;

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      // 상태
      user: undefined,
      accessToken: undefined,
      refreshToken: undefined,
      isAuthenticated: false,
      isHydrated: false,

      // 액션
      setAuth: (
        user: User,
        accessToken: string,
        refreshToken?: string,
      ): void => {
        set({
          user,
          accessToken,
          refreshToken: refreshToken || undefined,
          isAuthenticated: true,
        });
      },

      updateUser: (userData: Partial<User>): void => {
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : undefined,
        }));
      },

      logout: (): void => {
        // Access Token만 localStorage에서 제거
        // Refresh Token은 HttpOnly 쿠키로 관리되므로 서버에서 제거
        localStorage.removeItem("accessToken");
        // TanStack Query 캐시 초기화 (이전 사용자 데이터 잔류 방지)
        queryClient.clear();
        set({
          user: undefined,
          accessToken: undefined,
          refreshToken: undefined,
          isAuthenticated: false,
        });
      },

      // 권한 체크 헬퍼
      isAssociate: (): boolean => get().user?.role === ROLES.ASSOCIATE,
      isMember: (): boolean => get().user?.role === ROLES.MEMBER,
      isOperator: (): boolean => get().user?.role === ROLES.OPERATOR,
      isAdmin: (): boolean => get().user?.role === ROLES.ADMIN,

      // 최소 권한 체크
      hasMinRole: (minRole: string): boolean => {
        const userRole = get().user?.role;
        if (!userRole) return false;
        const userIndex = ROLE_ORDER.indexOf(userRole);
        const minIndex = ROLE_ORDER.indexOf(minRole as Role);
        return userIndex >= minIndex;
      },
    }),
    {
      name: "auth-storage",
      partialize: (state): AuthPersistState => ({
        user: state.user,
        accessToken: state.accessToken,
        // refreshToken은 HttpOnly 쿠키로 관리되므로 persist 제외
        refreshToken: undefined,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);

// hydration 완료 시 isHydrated 플래그 설정 + localStorage 동기화
useAuthStore.persist.onFinishHydration(() => {
  useAuthStore.setState({ isHydrated: true });
  // Zustand persist → standalone localStorage 동기화 (client.ts가 읽는 키)
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    localStorage.setItem("accessToken", accessToken);
  }
});

// Zustand v5: hydration이 콜백 등록 전에 이미 완료된 경우 대비
if (useAuthStore.persist.hasHydrated()) {
  useAuthStore.setState({ isHydrated: true });
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    localStorage.setItem("accessToken", accessToken);
  }
}
