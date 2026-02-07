import { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useUIStore, useAuthStore } from '@/stores';
import { isTokenExpired } from '@/utils/jwt';
import { API_BASE_URL } from '@/api/client';
import Sidebar from './Sidebar';
import Header from './Header';
import Footer from './Footer';
import { ToastContainer } from '@/components/ui/toast';

export default function Layout() {
  const location = useLocation();
  const { sidebarOpen, setSidebarOpen } = useUIStore();
  const { accessToken, isAuthenticated, logout } = useAuthStore();

  // 앱 초기화 시 zustand persist → localStorage 동기화
  // (client.ts의 customFetch가 localStorage에서 accessToken을 읽음)
  useEffect(() => {
    if (accessToken && !localStorage.getItem('accessToken')) {
      localStorage.setItem('accessToken', accessToken);
    }
  }, [accessToken]);

  // 마운트 시 만료된 토큰 선제 갱신
  // (401 발생 전에 처리하여 불필요한 실패 요청 방지)
  useEffect(() => {
    if (!isAuthenticated || !accessToken) return;
    if (!isTokenExpired(accessToken, 60)) return;

    const refreshToken = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/api/v1/auth/password/refresh`,
          {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
          }
        );

        if (!response.ok) throw new Error('Token refresh failed');

        const result = (await response.json()) as {
          accessToken: string;
          expiresIn: number;
        };

        localStorage.setItem('accessToken', result.accessToken);
        useAuthStore.setState({ accessToken: result.accessToken });
      } catch {
        logout();
      }
    };

    refreshToken();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

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
      <div className="flex-1 min-w-0 flex flex-col min-h-screen">
        {/* Header */}
        <Header />

        {/* Page Content */}
        <main className="px-s5 pb-s5 flex-1">
          <div className="max-w-7xl mx-auto h-full">
            <Outlet />
          </div>
        </main>

        {/* Footer - 조건부 렌더링 */}
        {showFooter && <Footer />}
      </div>

      {/* Toast Container */}
      <ToastContainer />
    </div>
  );
}
