import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
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
      alert('로그인에 실패했습니다. 학번과 비밀번호를 확인해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto py-12 animate-in slide-in-from-bottom-8 duration-500">
      <AuthForm
        mode="login"
        icon={<ShieldCheck size={32} className="text-primary" />}
        title="Welcome Back"
        subtitle="IGRUS 동아리 포털에 오신 것을 환영합니다."
        onSubmit={handleLogin}
        loading={loading}
      />
    </div>
  );
}
