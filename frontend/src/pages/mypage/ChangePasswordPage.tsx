import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Swal from 'sweetalert2';
import { AlertCircle, ArrowLeft, KeyRound } from 'lucide-react';
import { useAuth } from '@/hooks';
import { useChangeMyPassword } from '@/api/model/my-page/my-page';
import { isInvalidCredentials, hasErrorCode, getErrorMessage } from '@/utils/error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, '현재 비밀번호를 입력해주세요'),
    newPassword: z
      .string()
      .min(8, '비밀번호는 8자 이상이어야 합니다')
      .regex(PASSWORD_REGEX, '비밀번호는 영문, 숫자를 포함해야 합니다'),
    confirmPassword: z.string().min(1, '새 비밀번호를 다시 입력해주세요'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: '새 비밀번호가 일치하지 않습니다',
    path: ['confirmPassword'],
  });

type ChangePasswordForm = z.infer<typeof changePasswordSchema>;

export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ChangePasswordForm>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const { mutate: changePassword, isPending } = useChangeMyPassword({
    mutation: {
      onSuccess: () => {
        Swal.fire({
          icon: 'success',
          title: '비밀번호 변경 완료',
          text: '비밀번호가 변경되었습니다. 다시 로그인해주세요.',
          confirmButtonText: '확인',
          confirmButtonColor: '#28A745',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        }).then(() => {
          logout();
          navigate('/login');
        });
      },
      onError: (error: unknown) => {
        let errorMessage = getErrorMessage(error);

        if (isInvalidCredentials(error)) {
          errorMessage = '현재 비밀번호가 일치하지 않습니다.';
        } else if (hasErrorCode(error, 'SAME_PASSWORD')) {
          errorMessage = '현재 비밀번호와 다른 비밀번호를 입력해주세요.';
        } else if (hasErrorCode(error, 'INVALID_PASSWORD_FORMAT')) {
          errorMessage = '비밀번호는 영문, 숫자를 포함하여 8자 이상이어야 합니다.';
        }

        Swal.fire({
          icon: 'error',
          title: '비밀번호 변경 실패',
          text: errorMessage,
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      },
    },
  });

  const onSubmit = (data: ChangePasswordForm) => {
    changePassword({
      data: {
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      },
    });
  };

  const handleGoBack = () => {
    navigate('/mypage');
  };

  return (
    <div className="max-w-2xl mx-auto space-y-s6 animate-in fade-in duration-300">
      <button
        type="button"
        onClick={handleGoBack}
        className="flex items-center gap-s2 text-muted-foreground hover:text-foreground transition cursor-pointer"
      >
        <ArrowLeft size={16} />
        마이페이지로 돌아가기
      </button>

      <Card className="border-primary/30 bg-primary/5">
        <CardHeader>
          <CardTitle className="flex items-center gap-s2 text-primary">
            <AlertCircle size={20} />
            비밀번호 변경 안내
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-s2 text-sm">
          <p>비밀번호 변경 후 <strong>자동으로 로그아웃</strong>됩니다</p>
          <p>변경된 비밀번호로 다시 로그인해주세요</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-s2">
            <KeyRound size={20} />
            비밀번호 변경
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-s6">
            <input type="text" name="username" autoComplete="username" className="hidden" tabIndex={-1} aria-hidden="true" />
            <div>
              <label className="block text-sm font-medium mb-s2">
                현재 비밀번호 <span className="text-destructive">*</span>
              </label>
              <Input
                type="password"
                placeholder="현재 비밀번호 입력"
                autoComplete="current-password"
                {...register('currentPassword')}
                aria-invalid={!!errors.currentPassword}
              />
              {errors.currentPassword && (
                <p className="text-xs text-destructive mt-s1">{errors.currentPassword.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium mb-s2">
                새 비밀번호 <span className="text-destructive">*</span>
              </label>
              <Input
                type="password"
                placeholder="영문, 숫자 포함 8자 이상"
                autoComplete="new-password"
                {...register('newPassword')}
                aria-invalid={!!errors.newPassword}
              />
              {errors.newPassword && (
                <p className="text-xs text-destructive mt-s1">{errors.newPassword.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium mb-s2">
                새 비밀번호 확인 <span className="text-destructive">*</span>
              </label>
              <Input
                type="password"
                placeholder="새 비밀번호 다시 입력"
                autoComplete="new-password"
                {...register('confirmPassword')}
                aria-invalid={!!errors.confirmPassword}
              />
              {errors.confirmPassword && (
                <p className="text-xs text-destructive mt-s1">{errors.confirmPassword.message}</p>
              )}
            </div>

            <div className="flex justify-end">
              <Button
                type="submit"
                disabled={isPending}
                className="cursor-pointer"
              >
                {isPending ? '변경 중...' : '비밀번호 변경'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
