import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useUIStore } from '@/stores';
import { User, Lock, Mail, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';

export default function AuthForm({
  mode = 'login',
  icon,
  title,
  subtitle,
  onSubmit,
  loading = false,
}) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const [form, setForm] = useState({
    studentId: '',
    name: '',
    email: '',
    password: '',
    passwordConfirm: '',
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit?.(form);
  };

  const isLogin = mode === 'login';

  return (
    <Card className={`p-s6 lg:p-s7 rounded-[2.5rem] border ${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}>
      <CardContent className="p-0">
        <div className="text-center mb-s6">
          <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
            {icon}
          </div>
          <h2 className="text-h2 mb-2">{title}</h2>
          <p className="text-muted-foreground text-b2">{subtitle}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-s4">
          <div className="relative">
            <User size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="text"
              name="studentId"
              placeholder="학번 (8자리)"
              value={form.studentId}
              onChange={handleChange}
              className={`w-full rounded-r4 pl-12 pr-4 py-6 border focus:border-primary transition-all ${
                isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
              }`}
            />
          </div>

          {!isLogin && (
            <>
              <div className="relative">
                <User size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="text"
                  name="name"
                  placeholder="이름"
                  value={form.name}
                  onChange={handleChange}
                  className={`w-full rounded-r4 pl-12 pr-4 py-6 border focus:border-primary transition-all ${
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  }`}
                />
              </div>

              <div className="relative">
                <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <Input
                  type="email"
                  name="email"
                  placeholder="이메일"
                  value={form.email}
                  onChange={handleChange}
                  className={`w-full rounded-r4 pl-12 pr-4 py-6 border focus:border-primary transition-all ${
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  }`}
                />
              </div>
            </>
          )}

          <div className="relative">
            <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="password"
              name="password"
              placeholder="비밀번호"
              value={form.password}
              onChange={handleChange}
              className={`w-full rounded-r4 pl-12 pr-4 py-6 border focus:border-primary transition-all ${
                isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
              }`}
            />
          </div>

          {!isLogin && (
            <div className="relative">
              <Lock size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="password"
                name="passwordConfirm"
                placeholder="비밀번호 확인"
                value={form.passwordConfirm}
                onChange={handleChange}
                className={`w-full rounded-r4 pl-12 pr-4 py-6 border focus:border-primary transition-all ${
                  isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                }`}
              />
            </div>
          )}

          <Button
            type="submit"
            disabled={loading}
            className="w-full py-6 rounded-r4 font-bold flex items-center justify-center gap-2 shadow-lg shadow-primary/20"
          >
            {loading ? (isLogin ? '로그인 중...' : '가입 중...') : (isLogin ? '로그인' : '회원가입')}
            <ArrowRight size={18} />
          </Button>
        </form>

        <div className="mt-s5 text-center space-y-s3">
          {isLogin ? (
            <>
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
            </>
          ) : (
            <Link
              to="/login"
              className="text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest"
            >
              이미 계정이 있으신가요? 로그인
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
