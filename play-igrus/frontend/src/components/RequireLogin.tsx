import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { css } from "styled-system/css";
import { isStaff, useAuthStore } from "../stores/authStore";

/** 로그인 필요 페이지 가드 — 부트스트랩(silent refresh)이 끝나기 전엔 리다이렉트하지 않는다 */
export default function RequireLogin({
  staff = false,
  children,
}: {
  staff?: boolean;
  children: ReactNode;
}) {
  const { accessToken, user, bootstrapped } = useAuthStore();
  const location = useLocation();

  if (!bootstrapped && !accessToken) {
    return <p className={css({ textAlign: "center", color: "gray.400", py: "20" })}>확인 중…</p>;
  }
  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (staff && !isStaff(user)) {
    return <Navigate to="/" replace />;
  }
  return children;
}
