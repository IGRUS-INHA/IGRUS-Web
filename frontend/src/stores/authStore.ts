import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthStore, AuthPersistState } from '@/types/store';
import type { User } from '@/types/entities';
import { ROLES, type Role } from '@/types/common';

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
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      // 액션
      login: async (studentId: string, _password: string): Promise<void> => {
        // TODO: API 연동
        // const response = await authApi.login({ studentId, password });
        // set({ user: response.user, accessToken: response.accessToken, refreshToken: response.refreshToken, isAuthenticated: true });

        // Mock login
        set({
          user: {
            studentId,
            name: '테스트 유저',
            email: `${studentId}@inha.edu`,
            joinedDate: '2024-03-01',
            role: ROLES.ADMIN,
          },
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          isAuthenticated: true,
        });
      },

      setAuth: (
        user: User,
        accessToken: string,
        refreshToken: string
      ): void => {
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
        });
      },

      updateUser: (userData: Partial<User>): void => {
        set((state) => ({
          user: state.user ? { ...state.user, ...userData } : null,
        }));
      },

      logout: (): void => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
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
      name: 'auth-storage',
      partialize: (state): AuthPersistState => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
