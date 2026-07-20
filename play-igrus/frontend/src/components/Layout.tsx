import { useState } from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { logout } from "../api/client";
import { isStaff, useAuthStore } from "../stores/authStore";
import { PersonIcon } from "./Avatar";

export default function Layout() {
  const { accessToken, user } = useAuthStore();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

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
            className={flex({
              align: "center",
              gap: "2",
              fontSize: "lg",
              fontWeight: "800",
              letterSpacing: "tight",
            })}
          >
            <img
              src="/logo.jpg"
              alt=""
              className={css({ h: "8", w: "8", rounded: "md", objectFit: "cover" })}
            />
            <span>
              Play <span className={css({ color: "indigo.600" })}>Inha</span>
            </span>
          </Link>

          <nav className={flex({ align: "center", gap: "3", fontSize: "sm" })}>
            {accessToken ? (
              <>
                <Link to="/submit" className={navBtn}>
                  출시하기
                </Link>
                {isStaff(user) && (
                  <Link to="/admin" className={navLink}>
                    검수
                  </Link>
                )}

                {/* 프로필 아바타 → 드롭다운 메뉴 (내 정보 / 내 작품 / 로그아웃) */}
                <div className={css({ pos: "relative" })}>
                  <button
                    type="button"
                    aria-label="프로필 메뉴"
                    onClick={() => setMenuOpen((o) => !o)}
                    className={flex({
                      w: "9",
                      h: "9",
                      align: "center",
                      justify: "center",
                      rounded: "full",
                      bg: "gray.200",
                      color: "gray.500",
                      cursor: "pointer",
                      overflow: "hidden",
                      _hover: { bg: "gray.300" },
                    })}
                  >
                    {/* 프로필 자리표시자 (회색 사람 실루엣) */}
                    <PersonIcon />
                  </button>

                  {menuOpen && (
                    <>
                      {/* 바깥 탭으로 닫기 */}
                      <div
                        className={css({ pos: "fixed", inset: 0, zIndex: 10 })}
                        onClick={() => setMenuOpen(false)}
                      />
                      <div
                        className={flex({
                          pos: "absolute",
                          right: 0,
                          top: "calc(100% + 8px)",
                          zIndex: 11,
                          direction: "column",
                          w: "36",
                          bg: "white",
                          border: "1px solid",
                          borderColor: "gray.200",
                          rounded: "xl",
                          shadow: "lg",
                          overflow: "hidden",
                          py: "1",
                        })}
                      >
                        <Link to="/profile" className={menuItem} onClick={() => setMenuOpen(false)}>
                          내 정보
                        </Link>
                        <Link to="/my" className={menuItem} onClick={() => setMenuOpen(false)}>
                          내 작품
                        </Link>
                        <button
                          type="button"
                          className={`${menuItem} ${css({ color: "red.500", textAlign: "left" })}`}
                          onClick={() => {
                            setMenuOpen(false);
                            void logout().then(() => navigate("/"));
                          }}
                        >
                          로그아웃
                        </button>
                      </div>
                    </>
                  )}
                </div>
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

const menuItem = css({
  display: "block",
  px: "4",
  py: "2.5",
  fontSize: "sm",
  fontWeight: "600",
  color: "gray.700",
  cursor: "pointer",
  _hover: { bg: "gray.50" },
});

const navBtn = css({
  bg: "indigo.600",
  color: "white",
  px: "3",
  py: "1.5",
  rounded: "lg",
  fontWeight: "600",
  _hover: { bg: "indigo.700" },
});
