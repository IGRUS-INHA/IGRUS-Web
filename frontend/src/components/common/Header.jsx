import { Link } from 'react-router-dom';
import { useAuthStore } from '@/stores';
import { Button } from '@/components/ui/button';
import { ROLE_LABELS } from '@/constants';

export default function Header() {
  const { user, isAuthenticated } = useAuthStore();

  return (
    <header className="border-b bg-background">
      <div className="container mx-auto px-4 h-16 flex items-center justify-between">
        <Link to="/" className="text-xl font-bold">
          IGRUS
        </Link>

        <nav className="hidden md:flex items-center gap-6">
          <Link to="/board/notices" className="hover:text-primary">
            공지사항
          </Link>
          <Link to="/board/general" className="hover:text-primary">
            자유게시판
          </Link>
          <Link to="/board/insight" className="hover:text-primary">
            정보공유
          </Link>
          <Link to="/events" className="hover:text-primary">
            행사
          </Link>
        </nav>

        <div className="flex items-center gap-4">
          {isAuthenticated ? (
            <>
              <span className="text-sm text-muted-foreground">
                {user?.name} ({ROLE_LABELS[user?.role]})
              </span>
              <Link to="/mypage">
                <Button variant="ghost" size="sm">
                  마이페이지
                </Button>
              </Link>
            </>
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  로그인
                </Button>
              </Link>
              <Link to="/signup">
                <Button size="sm">회원가입</Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
