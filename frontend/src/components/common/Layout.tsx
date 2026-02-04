import { Outlet, useLocation } from 'react-router-dom';
import { useUIStore } from '@/stores';
import Sidebar from './Sidebar';
import Header from './Header';
import Footer from './Footer';

export default function Layout() {
  const location = useLocation();
  const { sidebarOpen, setSidebarOpen } = useUIStore();

  // 푸터를 표시할 페이지 경로 확인
  const shouldShowFooter = (pathname: string): boolean => {
    return (
      pathname === '/' ||
      pathname.startsWith('/board') ||
      pathname.startsWith('/events') ||
      pathname.startsWith('/inquiry')
    );
  };

  const showFooter = shouldShowFooter(location.pathname);

  return (
    <div className="min-h-screen bg-background text-foreground flex">
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content */}
      <div className="flex-1 min-w-0 flex flex-col">
        {/* Header */}
        <Header />

        {/* Page Content */}
        <main className="px-6 pb-6 flex-1">
          <div className="max-w-7xl mx-auto h-full">
            <Outlet />
          </div>
        </main>

        {/* Footer - 조건부 렌더링 */}
        {showFooter && <Footer />}
      </div>
    </div>
  );
}
