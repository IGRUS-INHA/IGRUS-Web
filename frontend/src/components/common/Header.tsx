import { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useUIStore } from "@/stores";
import SearchBar from "./SearchBar";
import { getPageTitle } from "@/constants/routes";
import { formatHeaderDate } from "@/utils";
import { Menu, Search } from "lucide-react";
import { useIsMobile } from "@/hooks/useIsMobile";

export default function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { sidebarOpen, toggleSidebar, theme } = useUIStore();

  const isMobile = useIsMobile();
  const pageTitle = getPageTitle(location.pathname);
  const currentDate = formatHeaderDate();
  const isDark = theme === "dark";
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // 검색 가능한 페이지 체크
  const searchablePaths = ["/board", "/events"];
  const shouldShowSearch =
    __FEATURE_SEARCH__ &&
    searchablePaths.some((path) => location.pathname.startsWith(path));

  // 검색어 상태 (URL 쿼리 파라미터와 동기화)
  const [searchKeyword, setSearchKeyword] = useState("");

  // URL 쿼리 파라미터에서 검색어 읽기
  useEffect(() => {
    const keyword = searchParams.get("search") || "";
    setSearchKeyword(keyword);
  }, [searchParams]);

  // 페이지 이동 시 모바일 검색 닫기
  useEffect(() => {
    setMobileSearchOpen(false);
  }, [location.pathname]);

  // 바깥 클릭 시 모바일 검색 닫기
  useEffect(() => {
    if (!mobileSearchOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setMobileSearchOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [mobileSearchOpen]);

  // 검색 처리
  const handleSearch = (keyword: string) => {
    const trimmedKeyword = keyword.trim();

    if (trimmedKeyword) {
      // 현재 경로의 쿼리 파라미터 업데이트
      const newParams = new URLSearchParams(searchParams);
      newParams.set("search", trimmedKeyword);
      navigate(`${location.pathname}?${newParams.toString()}`);
    } else {
      // 검색어가 없으면 search 파라미터 제거
      const newParams = new URLSearchParams(searchParams);
      newParams.delete("search");
      const queryString = newParams.toString();
      navigate(`${location.pathname}${queryString ? `?${queryString}` : ""}`);
    }
  };

  return (
    <header
      className={`sticky top-0 backdrop-blur-md pt-s5 transition-colors ${
        sidebarOpen ? "z-30" : "z-40"
      } ${isDark ? "bg-background/80" : "bg-white/80"}`}
    >
      <div className="w-full px-s4 lg:px-s7">
        {isMobile && mobileSearchOpen && shouldShowSearch ? (
          /* 모바일 검색 열림 상태: 전체 너비 검색 바 */
          <div ref={searchRef} className="flex items-center">
            <SearchBar
              value={searchKeyword}
              onChange={setSearchKeyword}
              onSearch={(keyword) => {
                handleSearch(keyword);
                setMobileSearchOpen(false);
              }}
              className="flex-1"
              autoFocus
            />
          </div>
        ) : (
          /* 기본 레이아웃: 메뉴 + 제목 + 검색 */
          <div className="flex justify-between items-center">
            {/* 왼쪽: 메뉴 + 제목 */}
            <div className="flex items-center gap-s3">
              <button
                onClick={toggleSidebar}
                className="lg:hidden p-s2 text-muted-foreground hover:text-primary transition-colors"
                type="button"
                aria-label="메뉴 열기"
                aria-expanded={sidebarOpen}
              >
                <Menu size={24} />
              </button>
              <div>
                <h1 className="text-xl lg:text-3xl font-bold capitalize tracking-tight">
                  {pageTitle}
                </h1>
                <p
                  className={`hidden lg:block text-xs text-muted-foreground mt-s1 ${pageTitle.trim() ? "" : "invisible"}`}
                >
                  {currentDate}
                </p>
              </div>
            </div>

            {/* 오른쪽: 검색 */}
            {shouldShowSearch &&
              (isMobile ? (
                <button
                  onClick={() => setMobileSearchOpen(true)}
                  type="button"
                  className="p-s2 text-muted-foreground hover:text-primary transition-colors"
                  aria-label="검색"
                >
                  <Search size={20} />
                </button>
              ) : (
                <div className="flex items-center">
                  <SearchBar
                    value={searchKeyword}
                    onChange={setSearchKeyword}
                    onSearch={handleSearch}
                  />
                </div>
              ))}
          </div>
        )}
      </div>
    </header>
  );
}
