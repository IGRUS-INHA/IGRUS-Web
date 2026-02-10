import { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom';
import Swal from 'sweetalert2';
import { Lock, Key, ArrowRight } from 'lucide-react';
import { useConfirmPasswordReset } from '@/api/model/password-authentication/password-authentication';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { useUIStore } from '@/stores';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  // URL query parameter에서 token 가져오기 (이메일 링크를 통한 접근)
  const tokenFromUrl = searchParams.get('token') || '';

  const [token, setToken] = useState(tokenFromUrl);
  const [newPassword, setNewPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [loading, setLoading] = useState(false);

  const confirmResetMutation = useConfirmPasswordReset();

  // ForgotPasswordPage에서 전달받은 학번 (optional)
  const studentIdFromState = location.state?.studentId as string | undefined;

  useEffect(() => {
    // URL에서 토큰을 받은 경우 자동으로 설정
    if (tokenFromUrl) {
      setToken(tokenFromUrl);
    }
  }, [tokenFromUrl]);

  const validatePassword = (password: string): string | null => {
    if (password.length < 8 || password.length > 72) {
      return '비밀번호는 8자 이상 72자 이하여야 합니다.';
    }

    const hasLetter = /[A-Za-z]/.test(password);
    const hasNumber = /\d/.test(password);

    if (!hasLetter || !hasNumber) {
      return '비밀번호는 영문, 숫자를 포함해야 합니다.';
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!token) {
      Swal.fire({
        icon: 'warning',
        title: '토큰 입력 필요',
        text: '재설정 토큰을 입력해주세요.',
        confirmButtonText: '확인',
        confirmButtonColor: '#FFC107',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
      return;
    }

    if (!newPassword || !passwordConfirm) {
      Swal.fire({
        icon: 'warning',
        title: '비밀번호 입력 필요',
        text: '새 비밀번호를 입력해주세요.',
        confirmButtonText: '확인',
        confirmButtonColor: '#FFC107',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
      return;
    }

    // 비밀번호 유효성 검사
    const passwordError = validatePassword(newPassword);
    if (passwordError) {
      Swal.fire({
        icon: 'warning',
        title: '비밀번호 형식 오류',
        text: passwordError,
        confirmButtonText: '확인',
        confirmButtonColor: '#FFC107',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
      return;
    }

    // 비밀번호 확인
    if (newPassword !== passwordConfirm) {
      Swal.fire({
        icon: 'error',
        title: '비밀번호 불일치',
        text: '비밀번호가 일치하지 않습니다.',
        confirmButtonText: '확인',
        confirmButtonColor: '#DC3545',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
      return;
    }

    setLoading(true);
    try {
      await confirmResetMutation.mutateAsync({
        data: {
          token,
          newPassword,
        },
      });

      await Swal.fire({
        icon: 'success',
        title: '재설정 완료',
        html: '비밀번호가 성공적으로 변경되었습니다.<br><br>새 비밀번호로 로그인해주세요.',
        confirmButtonText: '로그인하러 가기',
        confirmButtonColor: '#28A745',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
      navigate('/login');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';

      if (errorMessage.includes('만료')) {
        Swal.fire({
          icon: 'error',
          title: '토큰 만료',
          html: '재설정 토큰이 만료되었습니다.<br><br>비밀번호 찾기를 다시 시도해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      } else if (errorMessage.includes('토큰')) {
        Swal.fire({
          icon: 'error',
          title: '유효하지 않은 토큰',
          html: '유효하지 않은 토큰입니다.<br><br>토큰을 확인해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      } else if (errorMessage.includes('비밀번호')) {
        Swal.fire({
          icon: 'warning',
          title: '비밀번호 형식 오류',
          html: '비밀번호 형식이 올바르지 않습니다.<br><br>요구사항을 확인해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#FFC107',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      } else {
        Swal.fire({
          icon: 'error',
          title: '재설정 실패',
          html: '비밀번호 재설정에 실패했습니다.<br><br>다시 시도해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center h-full">
      <div className="max-w-md w-full animate-in slide-in-from-bottom-8 duration-500">
        <Card
          className={`p-s6 lg:p-s7 rounded-r4 border ${isDark ? 'bg-card' : 'bg-card shadow-xl'}`}
        >
          <CardContent className="p-0">
            <div className="text-center mb-s6">
              <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
                <Key size={32} className="text-primary" />
              </div>
              <h2 className="typo-h2 mb-s2">비밀번호 재설정</h2>
              <p className="text-muted-foreground typo-b2">
                {studentIdFromState
                  ? `학번 ${studentIdFromState}의 비밀번호를 재설정합니다.`
                  : '이메일로 받은 토큰과 새 비밀번호를 입력해주세요.'}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-s4">
              <div className="relative">
                <Key
                  size={18}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="text"
                  placeholder="재설정 토큰 (이메일 확인)"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  required
                  disabled={!!tokenFromUrl}
                  className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all ${
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  }`}
                />
              </div>

              <div className="relative">
                <Lock
                  size={18}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="password"
                  placeholder="새 비밀번호 (영문, 숫자 포함 8자 이상)"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all ${
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  }`}
                />
              </div>

              <div className="relative">
                <Lock
                  size={18}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="password"
                  placeholder="새 비밀번호 확인"
                  value={passwordConfirm}
                  onChange={(e) => setPasswordConfirm(e.target.value)}
                  required
                  className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all ${
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  }`}
                />
              </div>

              <Button
                type="submit"
                disabled={loading || !token || !newPassword || !passwordConfirm}
                className="w-full py-s6 rounded-r4 font-bold flex items-center justify-center gap-s2 shadow-lg shadow-primary/20"
              >
                {loading ? '재설정 중...' : '비밀번호 재설정'}
                <ArrowRight size={18} />
              </Button>
            </form>

            <div className="mt-s5 text-center space-y-s3">
              <Link
                to="/forgot-password"
                className="typo-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                토큰을 못 받으셨나요? 다시 요청하기
              </Link>
              <Link
                to="/login"
                className="typo-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                로그인 페이지로 이동
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
