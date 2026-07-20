import { create } from "zustand";
import { persist } from "zustand/middleware";

export interface User {
  studentId: string;
  name: string;
  department: string;
  role: string; // ASSOCIATE | MEMBER | OPERATOR | ADMIN
}

interface AuthState {
  accessToken?: string;
  user?: User;
  /** 시작 시 silent refresh 시도가 끝났는지 — 보호 페이지가 이거 전에 리다이렉트하면 안 된다 */
  bootstrapped: boolean;
  setAccessToken: (token: string) => void;
  setUser: (user: User) => void;
  setBootstrapped: () => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: undefined,
      user: undefined,
      bootstrapped: false,
      setAccessToken: (accessToken) => set({ accessToken }),
      setUser: (user) => set({ user }),
      setBootstrapped: () => set({ bootstrapped: true }),
      clear: () => set({ accessToken: undefined, user: undefined }),
    }),
    {
      name: "play-auth",
      partialize: (s) => ({ accessToken: s.accessToken, user: s.user }),
    },
  ),
);

export const isStaff = (user?: User) =>
  user?.role === "OPERATOR" || user?.role === "ADMIN";
