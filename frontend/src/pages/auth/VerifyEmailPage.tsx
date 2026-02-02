import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Mail, Key, ArrowRight } from 'lucide-react';
import { useVerifyEmail, useResendVerification } from '@/api/model/password-authentication/password-authentication';
import type { PasswordSignupResponse } from '@/api/model/models';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { useUIStore } from '@/stores';

export default function VerifyEmailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  // SignupPage에서 전달받은 이메일
  const signupEmail = location.state?.email as string | undefined;

  const [email, setEmail] = useState(signupEmail || '');
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  const verifyEmailMutation = useVerifyEmail();
  const resendVerificationMutation = useResendVerification();

  // 재발송 쿨다운 타이머
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => setResendCooldown(resendCooldown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  // 이메일이 없으면 로그인 페이지로 리다이렉트
  useEffect(() => {
    if (!signupEmail && !email) {
      alert('이메일 정보가 없습니다. 로그인 페이지로 이동합니다.');
      navigate('/login');
    }
  }, [signupEmail, email, navigate]);

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email || !code) {
      alert('이메일과 인증 코드를 모두 입력해주세요.');
      return;
    }

    if (code.length !== 6) {
      alert('인증 코드는 6자리 숫자입니다.');
      return;
    }

    setLoading(true);
    try {
      const response = await verifyEmailMutation.mutateAsync({
        data: {
          email,
          code,
        },
      });

      // Blob 타입 우회
      const verificationData = response.data as unknown as PasswordSignupResponse;

      console.log('Email verification success:', verificationData);

      alert('이메일 인증이 완료되었습니다!\n\n이제 로그인할 수 있습니다.');
      navigate('/login');
    } catch (error) {
      console.error('Email verification failed:', error);

      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';

      if (errorMessage.includes('만료')) {
        alert('인증 코드가 만료되었습니다.\n\n인증 코드를 다시 발송해주세요.');
      } else if (errorMessage.includes('시도')) {
        alert('인증 시도 횟수를 초과했습니다.\n\n인증 코드를 다시 발송해주세요.');
      } else if (errorMessage.includes('코드')) {
        alert('인증 코드가 일치하지 않습니다.\n\n다시 확인해주세요.');
      } else {
        alert('이메일 인증에 실패했습니다.\n\n다시 시도해주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (!email) {
      alert('이메일을 입력해주세요.');
      return;
    }

    if (resendCooldown > 0) {
      alert(`${resendCooldown}초 후에 다시 시도해주세요.`);
      return;
    }

    setResendLoading(true);
    try {
      await resendVerificationMutation.mutateAsync({
        data: { email },
      });

      alert('인증 코드가 재발송되었습니다.\n\n이메일을 확인해주세요.');
      setResendCooldown(60); // 60초 쿨다운
      setCode(''); // 기존 코드 입력 초기화
    } catch (error) {
      console.error('Resend verification failed:', error);

      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';

      if (errorMessage.includes('5분')) {
        alert('재발송 요청 횟수를 초과했습니다.\n\n5분 후에 다시 시도해주세요.');
      } else {
        alert('인증 코드 재발송에 실패했습니다.\n\n다시 시도해주세요.');
      }
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto py-12 animate-in slide-in-from-bottom-8 duration-500">
      <Card className={`p-s6 lg:p-s7 rounded-[2.5rem] border ${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}>
        <CardContent className="p-0">
          <div className="text-center mb-s6">
            <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
              <Mail size={32} className="text-primary" />
            </div>
            <h2 className="text-h2 mb-s2">이메일 인증</h2>
            <p className="text-muted-foreground text-b2">
              입력하신 이메일로 발송된 6자리 인증 코드를 입력해주세요.
            </p>
          </div>

          <form onSubmit={handleVerify} className="space-y-s4">
            <div className="relative">
              <Mail size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="email"
                placeholder="이메일 주소"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                disabled={!!signupEmail}
                className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all ${
                  isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                }`}
              />
            </div>

            <div className="relative">
              <Key size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                type="text"
                placeholder="6자리 인증 코드"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                required
                maxLength={6}
                className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all text-center text-2xl tracking-widest ${
                  isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                }`}
              />
            </div>

            <Button
              type="submit"
              disabled={loading || code.length !== 6}
              className="w-full py-s6 rounded-r4 font-bold flex items-center justify-center gap-s2 shadow-lg shadow-primary/20"
            >
              {loading ? '인증 중...' : '인증 확인'}
              <ArrowRight size={18} />
            </Button>
          </form>

          <div className="mt-s5 space-y-s3">
            <button
              type="button"
              onClick={handleResendCode}
              disabled={resendLoading || resendCooldown > 0}
              className="w-full text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {resendLoading
                ? '재발송 중...'
                : resendCooldown > 0
                  ? `인증 코드 재발송 (${resendCooldown}초)`
                  : '인증 코드 재발송'}
            </button>

            <button
              type="button"
              onClick={() => navigate('/login')}
              className="w-full text-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest"
            >
              로그인 페이지로 이동
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
