import { useState, useEffect } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useUIStore } from "@/stores";
import SearchBar from "./SearchBar";
import { getPageTitle } from "@/constants/routes";
import { formatHeaderDate } from "@/utils";
import { Menu } from "lucide-react";

export default function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { sidebarOpen, toggleSidebar, theme } = useUIStore();

  const pageTitle = getPageTitle(location.pathname);
  const currentDate = formatHeaderDate();
  const isDark = theme === "dark";

  // 검색 가능한 페이지 체크
  const searchablePaths = ["/board", "/events"];
  const shouldShowSearch = searchablePaths.some((path) =>
    location.pathname.startsWith(path)
  );

  // 검색어 상태 (URL 쿼리 파라미터와 동기화)
  const [searchKeyword, setSearchKeyword] = useState("");

  // URL 쿼리 파라미터에서 검색어 읽기
  useEffect(() => {
    const keyword = searchParams.get("search") || "";
    setSearchKeyword(keyword);
  }, [searchParams]);

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
      className={`sticky top-0 backdrop-blur-md py-s5 mb-s3 transition-colors ${
        sidebarOpen ? "z-30" : "z-40"
      } ${isDark ? "bg-background/80" : "bg-white/80"}`}
    >
      <div className="w-full px-s4 lg:px-s7">
        {/* 상단 행: 메뉴 + 제목 + 인증 버튼 */}
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
              <p className="hidden lg:block text-xs text-muted-foreground mt-s1">
                {currentDate}
              </p>
            </div>
          </div>

          {/* 오른쪽: 검색 (검색 가능한 페이지에서만 표시) */}
          {shouldShowSearch && (
            <div className="flex items-center">
              {/* 검색창 (모바일: 작게, 데스크톱: 크게) */}
              <SearchBar
                value={searchKeyword}
                onChange={setSearchKeyword}
                onSearch={handleSearch}
              />
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
