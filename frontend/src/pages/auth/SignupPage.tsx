import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useSignup } from '@/api/model/password-authentication/password-authentication';
import type { PasswordSignupResponse } from '@/api/model/models';
import AuthForm from '@/components/feature/auth/AuthForm';

export default function SignupPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const signupMutation = useSignup();

  const handleSignup = async (data: {
    studentId: string;
    name: string;
    email: string;
    password: string;
    passwordConfirm: string;
    phoneNumber?: string;
    department?: string;
    motivation?: string;
    gender?: 'MALE' | 'FEMALE';
    grade?: number;
    privacyConsent?: boolean;
  }) => {
    setLoading(true);
    try {
      if (data.password !== data.passwordConfirm) {
        alert('비밀번호가 일치하지 않습니다.');
        setLoading(false);
        return;
      }

      // 필수 필드 검증
      if (!data.phoneNumber || !data.department || !data.motivation || !data.gender || !data.grade || !data.privacyConsent) {
        alert('모든 필수 항목을 입력해주세요.');
        setLoading(false);
        return;
      }

      const response = await signupMutation.mutateAsync({
        data: {
          studentId: data.studentId,
          password: data.password,
          name: data.name,
          email: data.email,
          phoneNumber: data.phoneNumber,
          department: data.department,
          motivation: data.motivation,
          gender: data.gender,
          grade: data.grade,
          privacyConsent: data.privacyConsent,
        },
      });

      // Blob 타입 우회 (타입 캐스팅)
      const signupData = response.data as unknown as PasswordSignupResponse;

      // 회원가입 성공 메시지 표시
      if (signupData.requiresVerification) {
        alert(
          signupData.message ||
            '회원가입이 완료되었습니다. 이메일 인증 후 로그인해주세요.'
        );
      } else {
        alert(signupData.message || '회원가입이 완료되었습니다.');
      }

      // 로그인 페이지로 리다이렉트
      navigate('/login');
    } catch (error) {
      console.error('Signup failed:', error);
      alert('회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto py-12 animate-in slide-in-from-bottom-8 duration-500">
      <AuthForm
        mode="signup"
        icon={<ShieldCheck size={32} className="text-primary" />}
        title="회원가입"
        subtitle="IGRUS 동아리에 가입하여 다양한 활동에 참여하세요."
        onSubmit={handleSignup}
        loading={loading}
      />
    </div>
  );
}
