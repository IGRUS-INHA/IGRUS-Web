import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';
import { useAuthStore } from '@/stores';
import {
  useLogin,
  useResendVerification,
  checkRecoveryEligibility,
  recoverAccount,
} from '@/api/model/password-authentication/password-authentication';
import type {
  PasswordLoginResponse,
  RecoveryEligibilityResponse,
  AccountRecoveryResponse,
} from '@/api/model/models';
import AuthForm from '@/components/feature/auth/AuthForm';
import {
  isEmailNotVerified,
  isAccountWithdrawn,
  isAccountSuspended,
  isRateLimitError,
  getErrorMessage,
} from '@/utils/error';

export default function LoginPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const loginMutation = useLogin();
  const resendVerificationMutation = useResendVerification();

  // 이메일 인증 필요 시 처리
  const handleEmailVerificationRequired = async () => {
    const { value: email, isConfirmed } = await Swal.fire({
      icon: 'warning',
      title: '이메일 인증 필요',
      html: '이메일 인증이 완료되지 않았습니다.<br><br>회원가입 시 사용한 이메일 주소를 입력하면<br>인증 코드가 재발송됩니다.',
      input: 'email',
      inputPlaceholder: '이메일 주소 입력',
      inputAttributes: {
        autocomplete: 'email',
      },
      showCancelButton: true,
      confirmButtonText: '인증 코드 재발송',
      cancelButtonText: '취소',
      confirmButtonColor: '#FFC107',
      cancelButtonColor: '#6C757D',
      showClass: { popup: '', backdrop: '' },
      hideClass: { popup: '', backdrop: '' },
      inputValidator: (value) => {
        if (!value) {
          return '이메일 주소를 입력해주세요.';
        }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
          return '올바른 이메일 형식이 아닙니다.';
        }
        return null;
      },
    });

    if (isConfirmed && email) {
      try {
        await resendVerificationMutation.mutateAsync({
          data: { email },
        });

        await Swal.fire({
          icon: 'success',
          title: '인증 코드 발송 완료',
          html: `<strong>${email}</strong>로<br>인증 코드가 발송되었습니다.<br><br>이메일을 확인해주세요.`,
          confirmButtonText: '인증하기',
          confirmButtonColor: '#28A745',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });

        navigate('/verify-email', { state: { email } });
      } catch (resendError: unknown) {
        const errorText = isRateLimitError(resendError)
          ? '재발송 요청 횟수를 초과했습니다.<br><br>5분 후에 다시 시도해주세요.'
          : '인증 코드 재발송에 실패했습니다.<br><br>다시 시도해주세요.';

        await Swal.fire({
          icon: 'error',
          title: '재발송 실패',
          html: errorText,
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      }
    }
  };

  // 탈퇴 계정 복구 처리
  const handleAccountRecovery = async (studentId: string) => {
    try {
      const eligibilityResponse = await checkRecoveryEligibility({ studentId });
      const eligibility = eligibilityResponse.data as unknown as RecoveryEligibilityResponse;

      if (!eligibility.recoverable) {
        await Swal.fire({
          icon: 'error',
          title: '복구 불가',
          text: eligibility.message ?? '복구 가능 기간(5일)이 지났습니다.',
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
        return;
      }

      const confirmResult = await Swal.fire({
        icon: 'warning',
        title: '탈퇴한 계정',
        html: '탈퇴한 계정입니다.<br><br><strong>5일 이내 복구 가능</strong>합니다.<br>복구하시겠습니까?',
        showCancelButton: true,
        confirmButtonText: '복구하기',
        cancelButtonText: '취소',
        confirmButtonColor: '#28A745',
        cancelButtonColor: '#6C757D',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });

      if (!confirmResult.isConfirmed) return;

      const { value: password } = await Swal.fire({
        icon: 'info',
        title: '계정 복구',
        text: '비밀번호를 입력하여 계정을 복구합니다.',
        input: 'password',
        inputPlaceholder: '비밀번호 입력',
        inputAttributes: { autocomplete: 'current-password' },
        showCancelButton: true,
        confirmButtonText: '복구하기',
        cancelButtonText: '취소',
        confirmButtonColor: '#28A745',
        cancelButtonColor: '#6C757D',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
        inputValidator: (value) => {
          if (!value) return '비밀번호를 입력해주세요.';
          return null;
        },
      });

      if (!password) return;

      const recoveryResponse = await recoverAccount({ studentId, password });
      const recoveryData = recoveryResponse.data as unknown as AccountRecoveryResponse;

      if (recoveryData.accessToken && recoveryData.studentId && recoveryData.name && recoveryData.role) {
        const user = {
          studentId: recoveryData.studentId,
          name: recoveryData.name,
          email: '',
          joinedDate: '',
          role: recoveryData.role,
        };

        localStorage.setItem('accessToken', recoveryData.accessToken);
        setAuth(user, recoveryData.accessToken);

        await Swal.fire({
          icon: 'success',
          title: '복구 완료',
          text: '계정이 복구되었습니다.',
          confirmButtonText: '확인',
          confirmButtonColor: '#28A745',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });

        navigate('/');
      } else {
        await Swal.fire({
          icon: 'success',
          title: '복구 완료',
          text: '계정이 복구되었습니다. 다시 로그인해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#28A745',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      }
    } catch (recoveryError: unknown) {
      await Swal.fire({
        icon: 'error',
        title: '복구 실패',
        text: getErrorMessage(recoveryError),
        confirmButtonText: '확인',
        confirmButtonColor: '#DC3545',
        showClass: { popup: '', backdrop: '' },
        hideClass: { popup: '', backdrop: '' },
      });
    }
  };

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
      const loginData = response.data as unknown as Record<string, unknown>;

      // 탈퇴 계정 복구 가능 응답 처리 (200 + ACCOUNT_RECOVERABLE)
      if (loginData.code === 'ACCOUNT_RECOVERABLE') {
        await handleAccountRecovery(data.studentId);
        return;
      }

      const typedData = loginData as unknown as PasswordLoginResponse;

      // PasswordLoginResponse를 User 타입으로 변환
      if (typedData.accessToken && typedData.studentId && typedData.name && typedData.role) {
        const user = {
          studentId: typedData.studentId,
          name: typedData.name,
          email: '', // PasswordLoginResponse에 email이 없으므로 빈 문자열
          joinedDate: '', // PasswordLoginResponse에 joinedDate가 없으므로 빈 문자열
          role: typedData.role,
        };

        // Access Token만 localStorage에 저장 (client.ts의 customFetch가 사용)
        // Refresh Token은 HttpOnly 쿠키로 자동 관리됨
        localStorage.setItem('accessToken', typedData.accessToken);

        // zustand store에 저장 (Access Token만)
        setAuth(user, typedData.accessToken);
        navigate('/');
      } else {
        throw new Error('Invalid login response');
      }
    } catch (error: unknown) {
      console.error('Login failed:', error);

      if (isEmailNotVerified(error)) {
        await handleEmailVerificationRequired();
      } else if (isAccountWithdrawn(error)) {
        await handleAccountRecovery(data.studentId);
      } else if (isAccountSuspended(error)) {
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
          text: getErrorMessage(error),
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
