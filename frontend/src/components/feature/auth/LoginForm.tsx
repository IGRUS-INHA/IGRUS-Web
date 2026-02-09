import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useUIStore } from '@/stores';
import { User, Lock, ArrowRight, Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import type { ReactNode } from 'react';

interface LoginFormData {
  studentId: string;
  password: string;
}

interface LoginFormProps {
  icon?: ReactNode;
  title: string;
  subtitle: string;
  onSubmit?: (data: LoginFormData) => void;
  loading?: boolean;
  errors?: {
    studentId?: string;
    password?: string;
  };
}

export default function LoginForm({
  icon,
  title,
  subtitle,
  onSubmit,
  loading = false,
  errors = {},
}: LoginFormProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const [studentId, setStudentId] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSubmit?.({ studentId, password });
  };

  return (
    <Card className={`p-s4 lg:p-s7 rounded-r4 border ${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}>
      <CardContent className="p-0">
        <div className={`mb-s6 ${icon ? 'text-center' : 'text-left pt-s4'}`}>
          {icon && (
            <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
              {icon}
            </div>
          )}
          <h2 className="text-h2 mb-s2">{title}</h2>
          <p className="text-muted-foreground text-b2">{subtitle}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-s4">
          <div>
            <div className="relative">
              <User size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="text"
                placeholder="학번 (8자리)"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                required
                className={`w-full rounded-r4 pl-12 pr-s4 py-s6 border transition-all ${
                  errors.studentId
                    ? 'border-red-500 focus:border-red-500'
                    : 'focus:border-primary border-border'
                } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
              />
            </div>
            {errors.studentId && (
              <p className="mt-s1 text-sm text-red-500">{errors.studentId}</p>
            )}
          </div>

          <div>
            <div className="relative group">
              <Lock size={18} className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type={showPassword ? 'text' : 'password'}
                placeholder="비밀번호 (영문, 숫자 포함 8자 이상)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className={`w-full rounded-r4 pl-12 pr-12 py-s6 border transition-all ${
                  errors.password
                    ? 'border-red-500 focus:border-red-500'
                    : 'focus:border-primary border-border'
                } ${isDark ? 'bg-white/5' : 'bg-muted'}`}
              />
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-all opacity-0 group-focus-within:opacity-100"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.password && (
              <p className="mt-s1 text-sm text-red-500">{errors.password}</p>
            )}
          </div>

          <Button
            type="submit"
            disabled={loading}
            className="w-full py-s6 rounded-r4 font-bold flex items-center justify-center gap-s2 shadow-lg shadow-primary/20"
          >
            {loading ? '로그인 중...' : '로그인'}
            <ArrowRight size={18} />
          </Button>
        </form>

        <div className="mt-s5 text-center space-y-s3">
          <Link
            to="/forgot-password"
            className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
          >
            비밀번호를 잊으셨나요?
          </Link>
          <Link
            to="/signup"
            className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
          >
            계정이 없으신가요? 회원가입
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}
