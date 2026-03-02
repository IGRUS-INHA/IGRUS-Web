import { Outlet, useLocation } from "react-router-dom";
import { Menu } from "lucide-react";
import { useUIStore } from "@/stores";
import Sidebar from "./Sidebar";
import Header from "./Header";
import Footer from "./Footer";
import { ToastContainer } from "@/components/ui/toast";

export default function Layout() {
  const location = useLocation();
  const { sidebarOpen, setSidebarOpen, toggleSidebar } = useUIStore();

  // 헤더를 숨길 페이지 경로 확인
  const headerlessPrefixes = ["/login", "/signup", "/inquiry"];
  const isHeaderless = headerlessPrefixes.some((prefix) =>
    location.pathname.startsWith(prefix),
  );

  // 푸터를 표시할 페이지 경로 확인
  const shouldShowFooter = (pathname: string): boolean => {
    return (
      pathname === "/" ||
      pathname.startsWith("/board") ||
      pathname.startsWith("/events") ||
      pathname.startsWith("/inquiry")
    );
  };

  const showFooter = shouldShowFooter(location.pathname);

  return (
    <div className="min-h-screen bg-background text-foreground flex">
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content */}
      <div className="flex-1 min-w-0 flex flex-col min-h-screen">
        {/* Header - 인증/문의 페이지에서 숨김 */}
        {!isHeaderless ? (
          <Header />
        ) : (
          <div
            className={`lg:hidden sticky top-0 backdrop-blur-md py-s5 ${sidebarOpen ? "z-30" : "z-40"} bg-background/80`}
          >
            <div className="w-full px-s4">
              <button
                onClick={toggleSidebar}
                className="p-s2 text-muted-foreground hover:text-primary transition-colors"
                type="button"
                aria-label="메뉴 열기"
                aria-expanded={sidebarOpen}
              >
                <Menu size={24} />
              </button>
            </div>
          </div>
        )}

        {/* Page Content */}
        <main className="px-s5 pt-s5 pb-s5 flex-1">
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
