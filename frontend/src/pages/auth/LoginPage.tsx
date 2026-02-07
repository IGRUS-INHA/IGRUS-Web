import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';
import { useAuthStore } from '@/stores';
import { useLogin } from '@/api/model/password-authentication/password-authentication';
import type { PasswordLoginResponse } from '@/api/model/models';
import AuthForm from '@/components/feature/auth/AuthForm';

export default function LoginPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const loginMutation = useLogin();

  const handleLogin = async (data: {
    studentId: string;
    name: string;
    email: string;
    password: string;
    passwordConfirm: string;
  }) => {
    setLoading(true);
    try {
      const response = await loginMutation.mutateAsync({
        data: {
          studentId: data.studentId,
          password: data.password,
        },
      });

      // Blob 타입 우회 (타입 캐스팅)
      const loginData = response.data as unknown as PasswordLoginResponse;

      // PasswordLoginResponse를 User 타입으로 변환
      if (loginData.accessToken && loginData.studentId && loginData.name && loginData.role) {
        const user = {
          studentId: loginData.studentId,
          name: loginData.name,
          email: '', // PasswordLoginResponse에 email이 없으므로 빈 문자열
          joinedDate: '', // PasswordLoginResponse에 joinedDate가 없으므로 빈 문자열
          role: loginData.role,
        };

        // Access Token만 localStorage에 저장 (client.ts의 customFetch가 사용)
        // Refresh Token은 HttpOnly 쿠키로 자동 관리됨
        localStorage.setItem('accessToken', loginData.accessToken);

        // zustand store에 저장 (Access Token만)
        setAuth(user, loginData.accessToken);
        navigate('/');
      } else {
        throw new Error('Invalid login response');
      }
    } catch (error) {
      console.error('Login failed:', error);

      // 에러 메시지 확인
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';

      // 이메일 인증 관련 에러 처리
      if (errorMessage.includes('이메일 인증')) {
        Swal.fire({
          icon: 'warning',
          title: '이메일 인증 필요',
          html: '이메일 인증이 완료되지 않았습니다.<br><br>회원가입 시 입력하신 이메일에서 인증 메일을 확인해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#FFC107',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      } else if (errorMessage.includes('승인')) {
        Swal.fire({
          icon: 'info',
          title: '승인 대기 중',
          html: '관리자 승인 대기 중입니다.<br><br>승인 완료 후 로그인이 가능합니다.',
          confirmButtonText: '확인',
          confirmButtonColor: '#17A2B8',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      } else {
        Swal.fire({
          icon: 'error',
          title: '로그인 실패',
          html: '로그인에 실패했습니다.<br><br>학번과 비밀번호를 확인해주세요.',
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
      <div className="max-w-3xl w-full animate-in slide-in-from-bottom-8 duration-500">
        <AuthForm
          mode="login"
          icon={<img src="/igruslogo.png" alt="IGRUS Logo" className="w-12 h-12" />}
          title="Welcome IGRUS"
          subtitle="IGRUS 동아리 포털에 오신 것을 환영합니다."
          onSubmit={handleLogin}
          loading={loading}
        />
      </div>
    </div>
  );
}
