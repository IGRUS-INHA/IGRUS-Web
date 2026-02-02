import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore, useUIStore } from '@/stores';
import {
  LayoutDashboard,
  Users,
  UserCheck,
  HelpCircle,
  FileText,
  Menu,
  X,
  LogOut,
  Code,
} from 'lucide-react';
import { useState } from 'react';

interface AdminMenuItemProps {
  to: string;
  icon: React.ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
}

const AdminMenuItem = ({ to, icon, label, active, onClick }: AdminMenuItemProps) => {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  return (
    <Link
      to={to}
      onClick={onClick}
      className={`flex items-center gap-3 w-full px-4 py-3 rounded-r3 transition-all relative group ${
        active
          ? isDark
            ? 'bg-white/10 text-foreground'
            : 'bg-primary/10 text-primary'
          : isDark
            ? 'text-muted-foreground hover:text-foreground hover:bg-white/5'
            : 'text-muted-foreground hover:text-foreground hover:bg-muted'
      }`}
    >
      <span className={`${active ? 'text-primary' : 'group-hover:text-primary'} transition-colors`}>
        {icon}
      </span>
      <span className="text-label font-medium">{label}</span>
      {active && <span className="absolute left-0 w-1 h-6 bg-primary rounded-r-full" />}
    </Link>
  );
};

export default function AdminLayout() {
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const { theme } = useUIStore();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const isDark = theme === 'dark';

  const isActive = (path: string) => {
    if (path === '/admin' && location.pathname === '/admin') return true;
    if (path === '/admin') return false;
    return location.pathname.startsWith(path);
  };

  const handleMenuClick = () => {
    if (window.innerWidth < 1024) {
      setIsSidebarOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="flex min-h-screen bg-background">
      {/* Mobile Backdrop */}
      <div
        className={`fixed inset-0 bg-black/60 backdrop-blur-sm z-40 transition-opacity duration-300 lg:hidden ${
          isSidebarOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
        onClick={() => setIsSidebarOpen(false)}
      />

      {/* Admin Sidebar */}
      <aside
        className={`fixed lg:sticky top-0 left-0 z-50 w-64 border-r border-border p-s5 flex flex-col gap-s6 h-screen transition-transform duration-300 ease-in-out bg-background
          ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}
      >
        {/* Logo & Title */}
        <div className="flex justify-between items-center">
          <Link to="/" className="text-h3 ml-2 flex items-center gap-2">
            <Code className="w-6 h-6 text-primary" />
            <span>IGRUS</span>
          </Link>
          <button
            onClick={() => setIsSidebarOpen(false)}
            className="lg:hidden p-2 text-muted-foreground hover:text-primary transition-colors"
            type="button"
          >
            <X size={20} />
          </button>
        </div>

        {/* Admin Badge */}
        <div className="px-4 py-3 bg-primary/10 rounded-r3 border border-primary/20">
          <p className="text-c1 text-muted-foreground mb-1">관리자 모드</p>
          <p className="text-label font-semibold text-foreground">{user?.name ?? '관리자'}</p>
          <p className="text-c2 text-muted-foreground">{user?.email}</p>
        </div>

        {/* Navigation Menu */}
        <nav className="flex-1 flex flex-col gap-1">
          <AdminMenuItem
            to="/admin"
            icon={<LayoutDashboard size={20} />}
            label="대시보드"
            active={isActive('/admin') && location.pathname === '/admin'}
            onClick={handleMenuClick}
          />
          <AdminMenuItem
            to="/admin/users"
            icon={<Users size={20} />}
            label="회원 관리"
            active={isActive('/admin/users')}
            onClick={handleMenuClick}
          />
          <AdminMenuItem
            to="/admin/associates"
            icon={<UserCheck size={20} />}
            label="준회원 관리"
            active={isActive('/admin/associates')}
            onClick={handleMenuClick}
          />
          <AdminMenuItem
            to="/admin/inquiries"
            icon={<HelpCircle size={20} />}
            label="문의 관리"
            active={isActive('/admin/inquiries')}
            onClick={handleMenuClick}
          />
          <AdminMenuItem
            to="/admin/scraps"
            icon={<FileText size={20} />}
            label="스크랩 관리"
            active={isActive('/admin/scraps')}
            onClick={handleMenuClick}
          />
        </nav>

        {/* Bottom Actions */}
        <div className="flex flex-col gap-2">
          <Link
            to="/"
            className={`flex items-center gap-3 w-full px-4 py-3 rounded-r3 transition-all ${
              isDark
                ? 'text-muted-foreground hover:text-foreground hover:bg-white/5'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            }`}
          >
            <Code size={20} />
            <span className="text-label">메인으로</span>
          </Link>
          <button
            onClick={handleLogout}
            className={`flex items-center gap-3 w-full px-4 py-3 rounded-r3 transition-all ${
              isDark
                ? 'text-destructive hover:bg-destructive/10'
                : 'text-destructive hover:bg-destructive/10'
            }`}
            type="button"
          >
            <LogOut size={20} />
            <span className="text-label">로그아웃</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Mobile Header */}
        <header className="lg:hidden sticky top-0 z-30 bg-card border-b border-border px-4 py-3 flex items-center gap-3">
          <button
            onClick={() => setIsSidebarOpen(true)}
            className="p-2 text-muted-foreground hover:text-primary transition-colors"
            type="button"
          >
            <Menu size={24} />
          </button>
          <h1 className="text-h3">관리자 패널</h1>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-s5 lg:p-s7">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
