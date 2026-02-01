import { Link, useLocation } from 'react-router-dom';
import { useAuthStore, useUIStore } from '@/stores';
import {
  Home,
  MessageSquare,
  Calendar,
  HelpCircle,
  User,
  ShieldCheck,
  Sun,
  Moon,
  X,
  Code
} from 'lucide-react';

const MenuItem = ({ to, icon, label, active, onClick }) => {
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
      <span className="text-label">{label}</span>
      {active && <span className="absolute left-0 w-1 h-6 bg-primary rounded-r-full" />}
    </Link>
  );
};

export default function Sidebar({ isOpen, onClose }) {
  const location = useLocation();
  const { user, isAuthenticated } = useAuthStore();
  const { theme, toggleTheme } = useUIStore();
  const isDark = theme === 'dark';

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  const handleMenuClick = () => {
    if (window.innerWidth < 1024) {
      onClose?.();
    }
  };

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'OPERATOR';

  return (
    <>
      {/* Mobile Backdrop */}
      <div
        className={`fixed inset-0 bg-black/60 backdrop-blur-sm z-40 transition-opacity duration-300 lg:hidden ${
          isOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
        onClick={onClose}
      />

      {/* Sidebar */}
      <nav
        className={`fixed lg:sticky top-0 left-0 z-50 w-64 border-r p-s5 flex flex-col gap-s6 h-screen transition-transform duration-300 ease-in-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
          ${isDark ? 'border-border bg-background' : 'border-border bg-background'}`}
      >
        {/* Logo */}
        <div className="flex justify-between items-center">
          <Link to="/" className="text-h3 ml-2 flex items-center gap-2">
            <Code className="w-6 h-6 text-primary" />
            <span>IGRUS</span>
          </Link>
          <button
            onClick={onClose}
            className="lg:hidden p-2 text-muted-foreground hover:text-primary transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Main Navigation */}
        <div className="flex flex-col gap-1">
          <MenuItem
            to="/"
            icon={<Home size={20} />}
            label="홈"
            active={isActive('/')}
            onClick={handleMenuClick}
          />
          <MenuItem
            to="/board/notices"
            icon={<MessageSquare size={20} />}
            label="게시판"
            active={isActive('/board')}
            onClick={handleMenuClick}
          />
          <MenuItem
            to="/events"
            icon={<Calendar size={20} />}
            label="행사"
            active={isActive('/events')}
            onClick={handleMenuClick}
          />
          <MenuItem
            to="/inquiry"
            icon={<HelpCircle size={20} />}
            label="문의"
            active={isActive('/inquiry')}
            onClick={handleMenuClick}
          />
        </div>

        {/* Bottom Navigation */}
        <div className="mt-auto flex flex-col gap-1">
          {/* Theme Toggle */}
          <button
            onClick={toggleTheme}
            className={`flex items-center gap-3 w-full px-4 py-3 rounded-r3 transition-all mb-s3 ${
              isDark
                ? 'text-muted-foreground hover:text-foreground hover:bg-white/5'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            }`}
          >
            {isDark ? (
              <Sun size={20} className="text-warning" />
            ) : (
              <Moon size={20} className="text-primary" />
            )}
            <span className="text-label">{isDark ? '라이트 모드' : '다크 모드'}</span>
          </button>

          {isAuthenticated ? (
            <>
              <MenuItem
                to="/mypage"
                icon={<User size={20} />}
                label="마이페이지"
                active={isActive('/mypage')}
                onClick={handleMenuClick}
              />
              {isAdmin && (
                <MenuItem
                  to="/admin"
                  icon={<ShieldCheck size={20} />}
                  label="관리자"
                  active={isActive('/admin')}
                  onClick={handleMenuClick}
                />
              )}
            </>
          ) : (
            <MenuItem
              to="/login"
              icon={<User size={20} />}
              label="로그인"
              active={isActive('/login')}
              onClick={handleMenuClick}
            />
          )}
        </div>
      </nav>
    </>
  );
}
