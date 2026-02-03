import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
        alert('이메일 인증이 완료되지 않았습니다.\n\n회원가입 시 입력하신 이메일에서 인증 메일을 확인해주세요.');
      } else if (errorMessage.includes('승인')) {
        alert('관리자 승인 대기 중입니다.\n\n승인 완료 후 로그인이 가능합니다.');
      } else {
        alert('로그인에 실패했습니다.\n\n학번과 비밀번호를 확인해주세요.');
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
