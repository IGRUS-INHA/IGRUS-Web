import { Link, Outlet, useNavigate } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { logout } from "../api/client";
import { isStaff, useAuthStore } from "../stores/authStore";

export default function Layout() {
  const { accessToken, user } = useAuthStore();
  const navigate = useNavigate();

  return (
    <div className={css({ minH: "100dvh", bg: "gray.50" })}>
      <header
        className={css({
          pos: "sticky",
          top: 0,
          zIndex: 10,
          bg: "white/90",
          backdropFilter: "blur(8px)",
          borderBottom: "1px solid",
          borderColor: "gray.200",
        })}
      >
        <div
          className={flex({
            maxW: "5xl",
            mx: "auto",
            px: "4",
            h: "14",
            align: "center",
            justify: "space-between",
          })}
        >
          <Link
            to="/"
            className={css({ fontSize: "lg", fontWeight: "800", letterSpacing: "tight" })}
          >
            PLAY{" "}
            <span className={css({ color: "indigo.600" })}>IGRUS</span>
          </Link>

          <nav className={flex({ align: "center", gap: "3", fontSize: "sm" })}>
            {accessToken ? (
              <>
                <Link to="/submit" className={navBtn}>
                  출시하기
                </Link>
                <Link to="/my" className={navLink}>
                  내 작품
                </Link>
                {isStaff(user) && (
                  <Link to="/admin" className={navLink}>
                    검수
                  </Link>
                )}
                <button
                  type="button"
                  className={css({ color: "gray.500", cursor: "pointer" })}
                  onClick={() => {
                    void logout().then(() => navigate("/"));
                  }}
                >
                  로그아웃
                </button>
              </>
            ) : (
              <Link to="/login" className={navBtn}>
                로그인
              </Link>
            )}
          </nav>
        </div>
      </header>

      <main className={css({ maxW: "5xl", mx: "auto", px: "4", py: "5" })}>
        <Outlet />
      </main>
    </div>
  );
}

const navLink = css({ color: "gray.700", _hover: { color: "indigo.600" } });

const navBtn = css({
  bg: "indigo.600",
  color: "white",
  px: "3",
  py: "1.5",
  rounded: "lg",
  fontWeight: "600",
  _hover: { bg: "indigo.700" },
});
