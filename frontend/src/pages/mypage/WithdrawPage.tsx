import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Swal from 'sweetalert2';
import { AlertCircle, ArrowLeft, UserX } from 'lucide-react';
import { useAuth } from '@/hooks';
import { useWithdraw } from '@/api/model/my-page/my-page';
import { isInvalidCredentials, getErrorMessage } from '@/utils/error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';

const withdrawSchema = z.object({
  password: z.string().min(1, '비밀번호를 입력해주세요'),
  reason: z
    .string()
    .min(1, '탈퇴 사유를 입력해주세요')
    .max(500, '500자 이내로 입력해주세요'),
});

type WithdrawForm = z.infer<typeof withdrawSchema>;

export default function WithdrawPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<WithdrawForm>({
    resolver: zodResolver(withdrawSchema),
    defaultValues: {
      password: '',
      reason: '',
    },
  });

  const reasonLength = watch('reason')?.length ?? 0;

  const { mutate: withdraw, isPending } = useWithdraw({
    mutation: {
      onSuccess: () => {
        Swal.fire({
          icon: 'success',
          title: '탈퇴 완료',
          html: '회원 탈퇴가 완료되었습니다.<br><br><strong>5일 이내에 로그인 페이지에서 복구 가능</strong>합니다.',
          confirmButtonText: '확인',
          confirmButtonColor: '#28A745',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        }).then(() => {
          logout();
          navigate('/');
        });
      },
      onError: (error: unknown) => {
        const errorMessage = isInvalidCredentials(error)
          ? '비밀번호가 일치하지 않습니다.'
          : getErrorMessage(error);

        Swal.fire({
          icon: 'error',
          title: '탈퇴 실패',
          text: errorMessage,
          confirmButtonText: '확인',
          confirmButtonColor: '#DC3545',
          showClass: { popup: '', backdrop: '' },
          hideClass: { popup: '', backdrop: '' },
        });
      },
    },
  });

  const onSubmit = async (data: WithdrawForm) => {
    const result = await Swal.fire({
      icon: 'warning',
      title: '정말 탈퇴하시겠습니까?',
      html: '탈퇴 후 <strong>5일 이내에만 복구 가능</strong>합니다.<br>5일 경과 시 모든 데이터가 영구 삭제됩니다.',
      showCancelButton: true,
      confirmButtonText: '탈퇴하기',
      cancelButtonText: '취소',
      confirmButtonColor: '#DC3545',
      cancelButtonColor: '#6C757D',
      showClass: { popup: '', backdrop: '' },
      hideClass: { popup: '', backdrop: '' },
    });

    if (result.isConfirmed) {
      withdraw({ data });
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-s6 animate-in fade-in duration-300">
      <button
        type="button"
        onClick={() => navigate('/mypage')}
        className="flex items-center gap-s2 text-muted-foreground hover:text-foreground transition cursor-pointer"
      >
        <ArrowLeft size={16} />
        마이페이지로 돌아가기
      </button>

      <Card className="border-destructive/30 bg-destructive/5">
        <CardHeader>
          <CardTitle className="flex items-center gap-s2 text-destructive">
            <AlertCircle size={20} />
            회원 탈퇴 시 주의사항
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-s2 text-sm">
          <p>
            <strong>5일 이내 복구 가능</strong>합니다 (로그인 페이지에서 복구)
          </p>
          <p>작성한 게시글과 댓글은 삭제되지 않습니다</p>
          <p>
            5일 경과 후 모든 개인정보가 <strong>영구 삭제</strong>됩니다
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-s2">
            <UserX size={20} />
            회원 탈퇴
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-s6">
            <div>
              <label className="block text-sm font-medium mb-s2">
                비밀번호 확인 <span className="text-destructive">*</span>
              </label>
              <Input
                type="password"
                placeholder="현재 비밀번호 입력"
                autoComplete="current-password"
                {...register('password')}
                aria-invalid={!!errors.password}
              />
              {errors.password && (
                <p className="text-xs text-destructive mt-s1">{errors.password.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium mb-s2">
                탈퇴 사유 <span className="text-destructive">*</span>
              </label>
              <textarea
                placeholder="탈퇴 사유를 입력해주세요 (최대 500자)"
                rows={5}
                {...register('reason')}
                className={cn(
                  'w-full px-3 py-2 border rounded-md resize-none bg-background text-foreground',
                  'focus:outline-none focus:ring-2 focus:ring-ring',
                  errors.reason ? 'border-destructive' : 'border-input',
                )}
              />
              <div className="flex justify-between items-center mt-s1">
                {errors.reason ? (
                  <p className="text-xs text-destructive">{errors.reason.message}</p>
                ) : (
                  <div />
                )}
                <p
                  className={cn(
                    'text-xs',
                    reasonLength > 500 ? 'text-destructive' : 'text-muted-foreground',
                  )}
                >
                  {reasonLength}/500
                </p>
              </div>
            </div>

            <div className="flex gap-s3 justify-end">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/mypage')}
                className="cursor-pointer"
              >
                취소
              </Button>
              <Button
                type="submit"
                variant="destructive"
                disabled={isPending}
                className="cursor-pointer"
              >
                {isPending ? '처리 중...' : '탈퇴하기'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
