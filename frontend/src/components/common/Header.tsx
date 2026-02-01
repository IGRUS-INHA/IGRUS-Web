import { Link, useLocation } from 'react-router-dom';
import { useAuthStore, useUIStore } from '@/stores';
import { Button } from '@/components/ui/button';
import SearchBar from './SearchBar';
import { getPageTitle } from '@/constants/routes';
import { formatHeaderDate } from '@/utils';
import { Menu } from 'lucide-react';

export default function Header() {
  const location = useLocation();
  const { isAuthenticated } = useAuthStore();
  const { sidebarOpen, toggleSidebar } = useUIStore();

  const pageTitle = getPageTitle(location.pathname);
  const currentDate = formatHeaderDate();

  return (
    <header className="sticky top-0 z-40 backdrop-blur-md py-s5 mb-s6 transition-colors bg-white/80">
      <div className="container mx-auto px-s6">
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

          {/* 오른쪽: 검색 + 인증 */}
          <div className="flex items-center gap-s3 md:gap-s6">
            {/* 검색창 (모바일: 작게, 데스크톱: 크게) */}
            <SearchBar />

            {/* 인증 UI */}
            {!isAuthenticated && (
              <Link to="/login">
                <Button className="bg-primary text-white px-s4 lg:px-s6 py-s2 rounded-full text-sm font-bold hover:bg-primary/90 transition">
                  Sign In
                </Button>
              </Link>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
