import { useLocation } from 'react-router-dom';
import { useUIStore } from '@/stores';
import SearchBar from './SearchBar';
import { getPageTitle } from '@/constants/routes';
import { formatHeaderDate } from '@/utils';
import { Menu } from 'lucide-react';

export default function Header() {
  const location = useLocation();
  const { sidebarOpen, toggleSidebar, theme } = useUIStore();

  const pageTitle = getPageTitle(location.pathname);
  const currentDate = formatHeaderDate();
  const isDark = theme === 'dark';

  return (
    <header className={`sticky top-0 backdrop-blur-md py-s5 mb-s3 transition-colors ${
      sidebarOpen ? 'z-30' : 'z-40'
    } ${
      isDark ? 'bg-background/80' : 'bg-white/80'
    }`}>
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

          {/* 오른쪽: 검색 */}
          <div className="flex items-center">
            {/* 검색창 (모바일: 작게, 데스크톱: 크게) */}
            <SearchBar />
          </div>
        </div>
      </div>
    </header>
  );
}
