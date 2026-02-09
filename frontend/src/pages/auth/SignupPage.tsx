import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useSignup } from '@/api/model/password-authentication/password-authentication';
import type { PasswordSignupResponse } from '@/api/model/models';
import AuthForm from '@/components/feature/auth/AuthForm';
import { formatPhoneNumber } from '@/utils';

export default function SignupPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{
    studentId?: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    password?: string;
    passwordConfirm?: string;
    department?: string;
    grade?: string;
    gender?: string;
    motivation?: string;
    privacyConsent?: string;
  }>({});
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
    // 에러 초기화
    setErrors({});
    setLoading(true);

    // 클라이언트 측 유효성 검사
    try {
      if (data.password !== data.passwordConfirm) {
        setErrors({ passwordConfirm: '비밀번호가 일치하지 않습니다.' });
        setLoading(false);
        return;
      }

      // 필수 필드 검증
      const validationErrors: typeof errors = {};
      if (!data.phoneNumber) validationErrors.phoneNumber = '전화번호를 입력해주세요.';
      if (!data.department) validationErrors.department = '학과를 입력해주세요.';
      if (!data.motivation) validationErrors.motivation = '가입 동기를 입력해주세요.';
      if (!data.gender) validationErrors.gender = '성별을 선택해주세요.';
      if (!data.grade) validationErrors.grade = '학년을 입력해주세요.';
      if (!data.privacyConsent) validationErrors.privacyConsent = '개인정보 처리방침에 동의해주세요.';

      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        setLoading(false);
        return;
      }

      const response = await signupMutation.mutateAsync({
        data: {
          studentId: data.studentId,
          password: data.password,
          name: data.name,
          email: data.email,
          phoneNumber: formatPhoneNumber(data.phoneNumber!),
          department: data.department!,
          motivation: data.motivation!,
          gender: data.gender!,
          grade: data.grade!,
          privacyConsent: data.privacyConsent!,
        },
      });

      // 회원가입 성공 (response.data가 null이어도 정상)
      // HTTP status가 201 Created 또는 200 OK면 성공
      // 회원가입 성공 메시지 표시
      alert('회원가입이 완료되었습니다!\n\n입력하신 이메일로 인증 메일이 발송되었습니다.\n이메일 인증 페이지로 이동합니다.');

      // 이메일 인증 페이지로 리다이렉트 (이메일 정보를 state로 전달)
      navigate('/verify-email', { state: { email: data.email } });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';

      // 백엔드 에러 메시지 파싱
      const newErrors: typeof errors = {};

      if (errorMessage.includes('이미 가입된 학번') || errorMessage.includes('학번')) {
        newErrors.studentId = '이미 가입된 학번입니다.';
      }
      if (errorMessage.includes('이미 존재하는 이메일') || errorMessage.includes('이메일')) {
        newErrors.email = '이미 존재하는 이메일입니다.';
      }
      if (errorMessage.includes('이미 등록된 전화번호') || errorMessage.includes('전화번호')) {
        newErrors.phoneNumber = '이미 등록된 전화번호입니다.';
      }
      if (errorMessage.includes('비밀번호')) {
        newErrors.password = '비밀번호는 영문, 숫자를 포함하여 8자 이상이어야 합니다.';
      }

      // 필드별 에러가 있으면 설정, 없으면 일반 에러 메시지
      if (Object.keys(newErrors).length > 0) {
        setErrors(newErrors);
      } else {
        alert('회원가입에 실패했습니다. 다시 시도해주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-[1616px] mx-auto animate-in slide-in-from-bottom-8 duration-500">
      <AuthForm
        mode="signup"
        title="회원가입"
        subtitle="IGRUS 동아리에 가입하여 다양한 활동에 참여하세요."
        onSubmit={handleSignup}
        loading={loading}
        errors={errors}
      />
    </div>
  );
}
