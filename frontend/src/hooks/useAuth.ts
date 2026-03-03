import { useAuthStore } from "@/stores";
import type { User } from "@/types/entities";

interface UseAuthReturn {
  // 인증 상태
  user: User | undefined;
  isAuthenticated: boolean;
  isHydrated: boolean;

  // 인증 액션
  login: (user: User, accessToken: string, refreshToken?: string) => void;
  logout: () => void;

  // 역할 편의 속성
  isAdmin: boolean;
  isOperator: boolean;
  isMember: boolean;
  isAssociate: boolean;
}

export function useAuth(): UseAuthReturn {
  const store = useAuthStore();

  return {
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    isHydrated: store.isHydrated,
    login: store.setAuth,
    logout: store.logout,
    isAdmin: store.user?.role === "ADMIN",
    isOperator: store.user?.role === "OPERATOR",
    isMember: store.user?.role === "MEMBER",
    isAssociate: store.user?.role === "ASSOCIATE",
  };
}
