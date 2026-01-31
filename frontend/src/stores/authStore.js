import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set, get) => ({
      // 상태
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      // 액션
      login: async (studentId, password) => {
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
            role: 'MEMBER',
          },
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          isAuthenticated: true,
        });
      },

      setAuth: (user, accessToken, refreshToken) =>
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
        }),

      updateUser: (userData) =>
        set((state) => ({
          user: { ...state.user, ...userData },
        })),

      logout: () => {
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
      isAssociate: () => get().user?.role === 'ASSOCIATE',
      isMember: () => get().user?.role === 'MEMBER',
      isOperator: () => get().user?.role === 'OPERATOR',
      isAdmin: () => get().user?.role === 'ADMIN',

      // 최소 권한 체크
      hasMinRole: (minRole) => {
        const roles = ['ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN'];
        const userRoleIndex = roles.indexOf(get().user?.role);
        const minRoleIndex = roles.indexOf(minRole);
        return userRoleIndex >= minRoleIndex;
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
