import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="border-t bg-muted/50">
      <div className="container mx-auto px-4 py-8">
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          <div className="text-sm text-muted-foreground">
            © {new Date().getFullYear()} IGRUS. All rights reserved.
          </div>
          <div className="flex gap-4 text-sm">
            <Link to="/inquiry" className="hover:text-primary">
              문의하기
            </Link>
            <Link to="/privacy" className="hover:text-primary">
              개인정보처리방침
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
