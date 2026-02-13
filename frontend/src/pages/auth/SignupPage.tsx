import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  User,
  Lock,
  Mail,
  Phone,

  Building2,
  FileText,
  Eye,
  EyeOff,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Check,
  Loader2,
  ExternalLink,
  Wallet,
  AlertTriangle,
  Info,
} from 'lucide-react';
import { useSignup } from '@/api/model/password-authentication/password-authentication';
import { majorOptions } from '@/constants/majorOptions';
import { domainOptions } from '@/constants/domainOptions';
import { WISH_TITLE, wishOptions, wishToEnum } from '@/constants/wishOptions';
import { INTEREST_TITLE, interestOptions, interestToEnum } from '@/constants/interestOptions';
import { JOIN_ROUTE_TITLE, joinRouteOptions, joinRouteToEnum } from '@/constants/joinRouteOptions';
import { ENROLLMENT_STATUS_TITLE, enrollmentStatusOptions, enrollmentStatusToEnum } from '@/constants/enrollmentStatusOptions';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import { formatPhoneNumber } from '@/utils';
import { getErrorMessage, hasErrorCode } from '@/utils/error';
import { useSignupDuplicateCheck } from '@/hooks';
import { customFetch } from '@/api/client';

// --- 임시 학번 기간 체크 ---

const isTempStudentIdPeriod = (() => {
  const month = new Date().getMonth() + 1;
  return month === 1 || month === 2;
})();

// --- Zod Schema ---

const signupSchema = z
  .object({
    studentId: z
      .string()
      .min(1, '학번을 입력해주세요.')
      .regex(/^\d{8}$/, '학번은 8자리 숫자여야 합니다.'),
    name: z
      .string()
      .min(1, '이름을 입력해주세요.')
      .max(50, '이름은 50자 이내여야 합니다.'),
    gender: z
      .enum(['MALE', 'FEMALE'], {
        message: '성별을 선택해주세요.',
      })
      .optional()
      .refine((v) => v !== undefined, { message: '성별을 선택해주세요.' }),
    grade: z
      .number({ message: '학년을 선택해주세요.' })
      .min(1, '학년을 선택해주세요.')
      .max(4, '학년은 1~4 사이여야 합니다.')
      .optional()
      .refine((v) => v !== undefined, { message: '학년을 선택해주세요.' }),
    enrollmentStatus: z.string().min(1, '재학/휴학 여부를 선택해주세요.'),
    emailLocal: z.string().min(1, '이메일 아이디를 입력해주세요.'),
    emailDomain: z.string().min(1, '도메인을 선택해주세요.'),
    customDomain: z.string().optional(),
    phoneNumber: z
      .string()
      .min(1, '전화번호를 입력해주세요.')
      .regex(/^\d{3}-\d{4}-\d{4}$/, '올바른 전화번호를 입력해주세요.'),
    department: z.string().min(1, '학과를 선택해주세요.'),
    password: z
      .string()
      .min(8, '비밀번호는 8자 이상이어야 합니다.')
      .max(72, '비밀번호는 72자 이하여야 합니다.')
      .regex(
        /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/,
        '비밀번호는 영문과 숫자를 포함해야 합니다.',
      ),
    passwordConfirm: z.string().min(1, '비밀번호 확인을 입력해주세요.'),
    wishes: z.array(z.string()).min(1, '희망 활동을 1개 이상 선택해주세요.'),
    interests: z.array(z.string()).min(1, '관심 분야를 1개 이상 선택해주세요.'),
    customInterest: z.string().optional(),
    joinRoute: z.string().min(1, '가입 경로를 선택해주세요.'),
    customJoinRoute: z.string().optional(),
    motivation: z.string().optional(),
    privacyConsent: z.literal(true, {
      message: '개인정보 처리방침에 동의해주세요.',
    }),
    termsConsent: z.literal(true, {
      message: '이용약관에 동의해주세요.',
    }),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['passwordConfirm'],
  });

type SignupFormData = z.infer<typeof signupSchema>;

// --- Step Config ---

const STEPS = [
  { title: '기본 정보', icon: User },
  { title: '연락처', icon: Mail },
  { title: '계정 보안', icon: Lock },
  { title: '기타', icon: FileText },
] as const;

const STEP_FIELDS: (keyof SignupFormData)[][] = [
  ['studentId', 'name', 'gender', 'grade', 'enrollmentStatus', 'privacyConsent', 'termsConsent'],
  ['emailLocal', 'emailDomain', 'customDomain', 'phoneNumber', 'department'],
  ['password', 'passwordConfirm'],
  ['wishes', 'interests', 'customInterest', 'joinRoute', 'customJoinRoute', 'motivation'],
];

// --- Component ---

export default function SignupPage() {
  const navigate = useNavigate();
  const [feeConfirmed, setFeeConfirmed] = useState(false);
  const [feeChecked, setFeeChecked] = useState(false);
  const [step, setStep] = useState(0);
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [serverError, setServerError] = useState<string>();
  const [passwordConfirmTouched, setPasswordConfirmTouched] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [useTempStudentId, setUseTempStudentId] = useState(false);
  const signupMutation = useSignup();
  const {
    studentId: studentIdCheck,
    email: emailCheck,
    phoneNumber: phoneNumberCheck,
    checkStudentId,
    checkEmail,
    checkPhoneNumber,
    resetStudentId,
    resetEmail,
    resetPhoneNumber,
  } = useSignupDuplicateCheck();

  const {
    register,
    handleSubmit,
    trigger,
    watch,
    setValue,
    getValues,
    clearErrors,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormData>({
    resolver: zodResolver(signupSchema),
    defaultValues: {
      studentId: '',
      name: '',
      gender: undefined as unknown as 'MALE' | 'FEMALE',
      grade: undefined as unknown as number,
      enrollmentStatus: '',
      emailLocal: '',
      emailDomain: 'inha.edu',
      customDomain: '',
      phoneNumber: '',
      department: '',
      password: '',
      passwordConfirm: '',
      wishes: [],
      interests: [],
      customInterest: '',
      joinRoute: '',
      customJoinRoute: '',
      motivation: '',
      privacyConsent: undefined as unknown as true,
      termsConsent: undefined as unknown as true,
    },
    mode: 'onTouched',
  });

  const watchedPassword = watch('password');
  const selectedWishes = watch('wishes') ?? [];
  const selectedInterests = watch('interests') ?? [];
  const selectedJoinRoute = watch('joinRoute') ?? '';
  const emailDomain = watch('emailDomain');
  const watchedGrade = watch('grade');

  // 학년 변경 시 임시 학번 체크 해제
  useEffect(() => {
    if (watchedGrade !== 1) {
      setUseTempStudentId(false);
    }
  }, [watchedGrade]);

  // 비밀번호 변경 시 비밀번호 확인 필드 재검증
  useEffect(() => {
    if (passwordConfirmTouched) {
      trigger('passwordConfirm');
    }
  }, [watchedPassword, passwordConfirmTouched, trigger]);

  const handleWishToggle = (wish: string) => {
    const current = getValues('wishes') ?? [];
    const updated = current.includes(wish)
      ? current.filter((w) => w !== wish)
      : [...current, wish];
    setValue('wishes', updated);
  };

  const handleInterestToggle = (interest: string) => {
    const current = getValues('interests') ?? [];
    if (interest === '기타') {
      if (current.includes('기타')) {
        setValue('interests', current.filter((i) => i !== '기타'));
        setValue('customInterest', '');
      } else {
        setValue('interests', [...current, '기타']);
      }
    } else {
      const updated = current.includes(interest)
        ? current.filter((i) => i !== interest)
        : [...current, interest];
      setValue('interests', updated);
    }
  };

  const handleJoinRouteSelect = (route: string) => {
    if (selectedJoinRoute === route) {
      setValue('joinRoute', '');
    } else {
      setValue('joinRoute', route);
    }
    if (route !== '기타') {
      setValue('customJoinRoute', '');
    }
  };

  const composeEmail = () => {
    const local = getValues('emailLocal');
    const domain = getValues('emailDomain');
    const custom = getValues('customDomain');
    const fullDomain = domain === 'custom' ? custom : domain;
    if (local && fullDomain) return `${local}@${fullDomain}`;
    return '';
  };

  const handleNext = async () => {
    const fields = STEP_FIELDS[step] ?? [];
    // 임시 학번 사용 시 studentId 검증 건너뜀
    const fieldsToValidate = (step === 0 && useTempStudentId)
      ? fields.filter((f) => f !== 'studentId')
      : fields;
    const valid = await trigger(fieldsToValidate);
    if (!valid) return;

    // Step 0: 학번 중복 체크 확인 (임시 학번 사용 시 건너뜀)
    if (step === 0 && !useTempStudentId) {
      const studentIdValue = getValues('studentId');
      if (/^\d{8}$/.test(studentIdValue)) {
        if (!studentIdCheck.isChecked) {
          checkStudentId(studentIdValue);
          return;
        }
        if (studentIdCheck.isDuplicate) {
          setError('studentId', { message: studentIdCheck.message ?? '이미 가입된 학번입니다.' });
          return;
        }
      }
    }

    // Step 1: 이메일, 전화번호 중복 체크 확인
    if (step === 1) {
      const fullEmail = composeEmail();
      if (fullEmail) {
        if (!emailCheck.isChecked) {
          checkEmail(fullEmail);
          return;
        }
        if (emailCheck.isDuplicate) {
          setError('emailLocal', { message: emailCheck.message ?? '이미 존재하는 이메일입니다.' });
          return;
        }
      }

      const phoneValue = getValues('phoneNumber');
      if (/^\d{3}-\d{4}-\d{4}$/.test(phoneValue)) {
        if (!phoneNumberCheck.isChecked) {
          checkPhoneNumber(phoneValue);
          return;
        }
        if (phoneNumberCheck.isDuplicate) {
          setError('phoneNumber', { message: phoneNumberCheck.message ?? '이미 등록된 전화번호입니다.' });
          return;
        }
      }
    }

    const nextStep = step + 1;
    if (nextStep < STEP_FIELDS.length) {
      clearErrors(STEP_FIELDS[nextStep]);
    }
    setStep((s) => Math.min(s + 1, STEPS.length - 1));
  };

  const handlePrev = () => {
    if (step === 0) {
      setFeeConfirmed(false);
    } else {
      setStep((s) => s - 1);
    }
  };

  const onSubmit = async (data: SignupFormData) => {
    setServerError(undefined);
    try {
      const domain =
        data.emailDomain === 'custom' ? data.customDomain : data.emailDomain;
      const fullEmail = `${data.emailLocal}@${domain}`;

      if (useTempStudentId) {
        // 임시 학번 회원가입
        const result = await customFetch<{ data: { temporaryStudentId?: string; email?: string } }>(
          '/api/v1/auth/password/signup/temporary',
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              password: data.password,
              name: data.name,
              email: fullEmail,
              phoneNumber: formatPhoneNumber(data.phoneNumber),
              department: data.department,
              motivation: data.motivation || undefined,
              gender: data.gender!,
              grade: data.grade!,
              enrollmentStatus: enrollmentStatusToEnum[data.enrollmentStatus],
              wishes: data.wishes.map((w) => wishToEnum[w]).filter(Boolean),
              interests: data.interests.map((i) => interestToEnum[i]).filter(Boolean),
              customInterest: data.interests.includes('기타') ? data.customInterest : undefined,
              joinRoute: joinRouteToEnum[data.joinRoute] ?? 'OTHER',
              customJoinRoute: data.joinRoute === '기타' ? data.customJoinRoute : undefined,
              privacyConsent: data.privacyConsent,
            }),
          },
        );

        const tempId = result.data.temporaryStudentId;
        alert(
          `회원가입이 완료되었습니다!\n\n임시 학번 [${tempId}]이 발급되었습니다.\n입력하신 이메일로 인증 메일과 임시 학번 안내가 발송되었습니다.\n이메일 인증 페이지로 이동합니다.`,
        );
        navigate('/verify-email', { state: { email: fullEmail } });
      } else {
        // 일반 회원가입
        await signupMutation.mutateAsync({
          data: {
            studentId: data.studentId,
            password: data.password,
            name: data.name,
            email: fullEmail,
            phoneNumber: formatPhoneNumber(data.phoneNumber),
            department: data.department,
            motivation: data.motivation || undefined,
            gender: data.gender!,
            grade: data.grade!,
            enrollmentStatus: enrollmentStatusToEnum[data.enrollmentStatus],
            wishes: data.wishes
              .map((w) => wishToEnum[w])
              .filter(Boolean),
            interests: data.interests
              .map((i) => interestToEnum[i])
              .filter(Boolean),
            customInterest: data.interests.includes('기타') ? data.customInterest : undefined,
            joinRoute: joinRouteToEnum[data.joinRoute] ?? 'OTHER',
            customJoinRoute: data.joinRoute === '기타' ? data.customJoinRoute : undefined,
            privacyConsent: data.privacyConsent,
          },
        });

        alert(
          '회원가입이 완료되었습니다!\n\n입력하신 이메일로 인증 메일이 발송되었습니다.\n이메일 인증 페이지로 이동합니다.',
        );
        navigate('/verify-email', { state: { email: fullEmail } });
      }
    } catch (error: unknown) {
      if (hasErrorCode(error, 'DUPLICATE_STUDENT_ID')) {
        setStep(0);
        setError('studentId', { message: '이미 가입된 학번입니다.' });
      } else if (hasErrorCode(error, 'DUPLICATE_EMAIL')) {
        setStep(1);
        setError('emailLocal', { message: '이미 존재하는 이메일입니다.' });
      } else if (hasErrorCode(error, 'DUPLICATE_PHONE_NUMBER')) {
        setStep(1);
        setError('phoneNumber', { message: '이미 등록된 전화번호입니다.' });
      } else if (hasErrorCode(error, 'TEMP_STUDENT_ID_NOT_AVAILABLE')) {
        setServerError('임시 학번 발급은 1월~2월에만 가능합니다.');
      } else {
        setServerError(getErrorMessage(error));
      }
    }
  };

  const isLastStep = step === STEPS.length - 1;

  return (
    <div className="flex items-center justify-center min-h-full py-s6 px-s4">
      <div className="w-full max-w-2xl animate-in slide-in-from-bottom-6 duration-500">
        {/* Header */}
        <div className="text-center mb-s7">
          <h1 className="typo-h2 text-foreground">IGRUS 회원가입</h1>
          <p className="typo-b2 text-muted-foreground mt-s1">
            동아리에 가입하여 다양한 활동에 참여하세요
          </p>
        </div>

        {/* Fee Payment Confirmation Gate */}
        {!feeConfirmed && (
          <div className="rounded-r4 border bg-card p-s6 shadow-sm">
            <div className="flex items-center gap-s2 mb-s5">
              <Wallet size={22} className="text-primary" />
              <h2 className="typo-h4 text-foreground">회비 납부 안내</h2>
            </div>

            <p className="typo-b2 text-foreground mb-s5">
              IGRUS의 활동에 참여하기 위해선 회비 <strong>2만원</strong>을 납부해주셔야 합니다.
            </p>

            <div className="bg-muted/50 border border-border rounded-r2 p-s4 mb-s5 space-y-s2">
              <div className="relative flex flex-col sm:block gap-s1">
                <span className="text-sm text-muted-foreground shrink-0">입금자명 양식</span>
                <span className="text-sm font-medium text-foreground sm:absolute sm:inset-0 sm:flex sm:items-baseline sm:justify-center">학번 2자리+이름 (ex. 26김아그)</span>
              </div>
              <div className="relative flex flex-col sm:block gap-s1">
                <span className="text-sm text-muted-foreground shrink-0">입금계좌</span>
                <span className="text-sm font-medium text-foreground sm:absolute sm:inset-0 sm:flex sm:items-baseline sm:justify-center">토스뱅크 1002-3803-2581</span>
              </div>
            </div>

            <div className="flex items-start gap-s2 mb-s5 rounded-r2 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 p-s3">
              <AlertTriangle size={16} className="text-amber-600 shrink-0 mt-0.5" />
              <p className="text-sm font-bold text-amber-700 dark:text-amber-400">
                입금자명 양식을 지키지 않으실 경우, 회비 납부 명단에서 누락될 수 있습니다.
                <br />
                정확한 형식으로 입금해 주시기 바랍니다.
              </p>
            </div>

            <div className="flex items-start gap-s2 mb-s5 rounded-r2 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 p-s3">
              <AlertTriangle size={16} className="text-amber-600 shrink-0 mt-0.5" />
              <p className="text-sm font-bold text-amber-700 dark:text-amber-400">
                입금 후 반드시 회원가입과 이메일 인증까지 완료해 주세요.
                <br />
                회원가입 또는 이메일 인증이 완료되지 않으면 가입이 정상 처리되지 않습니다.
              </p>
            </div>

            <label className="flex items-center gap-s3 cursor-pointer group mb-s5">
              <input
                type="checkbox"
                checked={feeChecked}
                onChange={(e) => setFeeChecked(e.target.checked)}
                className="cursor-pointer accent-primary"
              />
              <span className="text-sm font-medium text-muted-foreground group-hover:text-foreground transition-colors">
                회비 납부를 완료했습니다
              </span>
            </label>

            <Button
              type="button"
              disabled={!feeChecked}
              onClick={() => setFeeConfirmed(true)}
              className="w-full cursor-pointer"
            >
              회원가입 진행하기
              <ChevronRight size={18} />
            </Button>

            <p className="text-center text-sm text-muted-foreground mt-s5">
              이미 계정이 있으신가요?{' '}
              <Link
                to="/login"
                className="text-primary font-medium hover:underline"
              >
                로그인
              </Link>
            </p>
          </div>
        )}

        {/* Step Indicator */}
        {feeConfirmed && (
        <div className="flex items-center justify-between mb-s7 px-s2">
          {STEPS.map((s, i) => {
            const Icon = s.icon;
            const isActive = i === step;
            const isCompleted = i < step;

            return (
              <div key={s.title} className="flex items-center flex-1 last:flex-none">
                <div className="flex flex-col items-center gap-s1">
                  <button
                    type="button"
                    onClick={() => {
                      if (isCompleted) setStep(i);
                    }}
                    className={cn(
                      'w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 border-2',
                      isCompleted &&
                        'bg-primary border-primary text-primary-foreground cursor-pointer hover:bg-primary/90',
                      isActive && 'border-primary bg-primary/10 text-primary',
                      !isActive &&
                        !isCompleted &&
                        'border-border bg-muted text-muted-foreground',
                    )}
                  >
                    {isCompleted ? <Check size={18} /> : <Icon size={18} />}
                  </button>
                  <span
                    className={cn(
                      'typo-c1 font-medium whitespace-nowrap',
                      (isActive || isCompleted) && 'text-primary',
                      !isActive && !isCompleted && 'text-muted-foreground',
                    )}
                  >
                    {s.title}
                  </span>
                </div>

                {i < STEPS.length - 1 && (
                  <div
                    className={cn(
                      'flex-1 h-0.5 mx-s2 mt-[-20px] rounded-full transition-colors duration-300',
                      i < step ? 'bg-primary' : 'bg-border',
                    )}
                  />
                )}
              </div>
            );
          })}
        </div>
        )}

        {/* Form Card */}
        {feeConfirmed && (
        <div className="rounded-r4 border bg-card p-s6 shadow-sm">
          {serverError && (
            <div className="mb-s5 rounded-r2 bg-destructive/10 border border-destructive/20 p-s4 text-sm text-destructive">
              {serverError}
            </div>
          )}

          <form onSubmit={(e) => e.preventDefault()}>
            {/* Step 1: 기본 정보 */}
            <div className={cn('space-y-s4', step !== 0 && 'hidden')}>
              {!useTempStudentId && (
                <FormField
                  label="학번"
                  error={errors.studentId?.message || (studentIdCheck.isDuplicate ? studentIdCheck.message : undefined)}
                  success={studentIdCheck.isAvailable ? studentIdCheck.message : undefined}
                >
                  <div className="relative">
                    <User
                      size={18}
                      className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                    />
                    <Input
                      {...register('studentId', {
                        onBlur: () => {
                          const value = getValues('studentId');
                          if (/^\d{8}$/.test(value)) {
                            checkStudentId(value);
                          }
                        },
                        onChange: () => {
                          resetStudentId();
                        },
                      })}
                      placeholder="12345678"
                      maxLength={8}
                      className={cn('pl-10', (studentIdCheck.isChecking || studentIdCheck.isAvailable) && 'pr-10')}
                    />
                    {studentIdCheck.isChecking && (
                      <Loader2 size={16} className="absolute right-s3 top-1/2 -translate-y-1/2 animate-spin text-muted-foreground" />
                    )}
                    {studentIdCheck.isAvailable && !studentIdCheck.isChecking && (
                      <Check size={16} className="absolute right-s3 top-1/2 -translate-y-1/2 text-green-600" />
                    )}
                  </div>
                </FormField>
              )}

              {isTempStudentIdPeriod && (
                <label className="flex items-center gap-s3 cursor-pointer group rounded-r2 border border-border p-s3">
                  <input
                    type="checkbox"
                    checked={useTempStudentId}
                    onChange={(e) => {
                      setUseTempStudentId(e.target.checked);
                      if (e.target.checked) {
                        setValue('studentId', '');
                        clearErrors('studentId');
                        resetStudentId();
                        setValue('grade', 1, { shouldValidate: true });
                      }
                    }}
                    className="cursor-pointer accent-primary"
                  />
                  <span className="text-sm text-muted-foreground group-hover:text-foreground transition-colors">
                    신입생이라서 아직 학번이 나오지 않았어요
                  </span>
                </label>
              )}

              {useTempStudentId && (
                <div className="flex items-start gap-s2 rounded-r2 bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 p-s3">
                  <Info size={16} className="text-blue-600 shrink-0 mt-0.5" />
                  <p className="text-sm text-blue-700 dark:text-blue-400">
                    임시 학번이 자동으로 발급되어 이메일로 전송됩니다.
                    <br />
                    학번이 나오면 <strong>마이페이지</strong>에서 실제 학번으로 변경해주세요.
                  </p>
                </div>
              )}

              <FormField label="이름" error={errors.name?.message}>
                <div className="relative">
                  <User
                    size={18}
                    className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                  />
                  <Input
                    {...register('name')}
                    placeholder="홍길동"
                    className="pl-10"
                  />
                </div>
              </FormField>

              <FormField label="성별" error={errors.gender?.message}>
                <div className="grid grid-cols-2 gap-s3">
                  {(['MALE', 'FEMALE'] as const).map((g) => (
                    <button
                      key={g}
                      type="button"
                      onClick={() =>
                        setValue('gender', g, { shouldValidate: true })
                      }
                      className={cn(
                        'h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer',
                        watch('gender') === g
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'bg-muted border-border text-foreground hover:border-primary/50',
                      )}
                    >
                      {g === 'MALE' ? '남성' : '여성'}
                    </button>
                  ))}
                </div>
              </FormField>

              <FormField label="학년" error={errors.grade?.message}>
                <div className="grid grid-cols-4 gap-s2">
                  {[1, 2, 3, 4].map((g) => (
                    <button
                      key={g}
                      type="button"
                      onClick={() =>
                        setValue('grade', g, { shouldValidate: true })
                      }
                      className={cn(
                        'h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer',
                        watch('grade') === g
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'bg-muted border-border text-foreground hover:border-primary/50',
                      )}
                    >
                      {g}학년
                    </button>
                  ))}
                </div>
              </FormField>

              <FormField label={ENROLLMENT_STATUS_TITLE} error={errors.enrollmentStatus?.message}>
                <div className="grid grid-cols-3 gap-s2">
                  {enrollmentStatusOptions.map((status) => (
                    <button
                      key={status}
                      type="button"
                      onClick={() =>
                        setValue('enrollmentStatus', status, { shouldValidate: true })
                      }
                      className={cn(
                        'h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer',
                        watch('enrollmentStatus') === status
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'bg-muted border-border text-foreground hover:border-primary/50',
                      )}
                    >
                      {status}
                    </button>
                  ))}
                </div>
              </FormField>

              <div className="space-y-s3 rounded-r2 border border-border p-s4">
                <FormField error={errors.privacyConsent?.message}>
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-s3 cursor-pointer group">
                      <input
                        type="checkbox"
                        {...register('privacyConsent')}
                        className="cursor-pointer accent-primary"
                      />
                      <span className="text-sm text-muted-foreground group-hover:text-foreground transition-colors">
                        개인정보 처리방침에 동의합니다 (필수)
                      </span>
                    </label>
                    <Link to="/privacy" target="_blank" className="text-muted-foreground hover:text-foreground transition-colors">
                      <ExternalLink size={16} />
                    </Link>
                  </div>
                </FormField>

                <FormField error={errors.termsConsent?.message}>
                  <div className="flex items-center justify-between">
                    <label className="flex items-center gap-s3 cursor-pointer group">
                      <input
                        type="checkbox"
                        {...register('termsConsent')}
                        className="cursor-pointer accent-primary"
                      />
                      <span className="text-sm text-muted-foreground group-hover:text-foreground transition-colors">
                        이용약관에 동의합니다 (필수)
                      </span>
                    </label>
                    <Link to="/terms" target="_blank" className="text-muted-foreground hover:text-foreground transition-colors">
                      <ExternalLink size={16} />
                    </Link>
                  </div>
                </FormField>
              </div>
            </div>

            {/* Step 2: 연락처 & 학과 */}
            <div className={cn('space-y-s4', step !== 1 && 'hidden')}>
              <FormField
                label="이메일"
                error={errors.emailLocal?.message || errors.customDomain?.message || (emailCheck.isDuplicate ? emailCheck.message : undefined)}
                success={emailCheck.isAvailable ? emailCheck.message : undefined}
              >
                <div className="flex items-center gap-s2">
                  <div className="relative flex-1">
                    <Mail
                      size={18}
                      className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                    />
                    <Input
                      {...register('emailLocal', {
                        onBlur: () => {
                          const fullEmail = composeEmail();
                          if (fullEmail) checkEmail(fullEmail);
                        },
                        onChange: () => {
                          resetEmail();
                        },
                      })}
                      placeholder="아이디"
                      className="pl-10"
                    />
                  </div>
                  <span className="text-muted-foreground font-bold shrink-0">@</span>
                  <div className="relative flex-1">
                    <select
                      {...register('emailDomain', {
                        onChange: () => {
                          resetEmail();
                          // 도메인이 커스텀이 아니고 로컬파트가 있으면 즉시 체크
                          setTimeout(() => {
                            const fullEmail = composeEmail();
                            if (fullEmail && getValues('emailDomain') !== 'custom') {
                              checkEmail(fullEmail);
                            }
                          }, 0);
                        },
                      })}
                      className={cn(
                        'w-full h-9 rounded-r2 border border-input bg-background text-foreground px-s3 text-sm',
                        'appearance-none cursor-pointer transition-all outline-none',
                        'focus:border-ring focus:ring-ring/50 focus:ring-[3px]',
                      )}
                    >
                      {domainOptions.map((d) => (
                        <option key={d.value} value={d.value} className="bg-background text-foreground">
                          {d.label}
                        </option>
                      ))}
                    </select>
                    <ChevronDown
                      size={16}
                      className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
                    />
                  </div>
                </div>
                {emailDomain === 'custom' && (
                  <Input
                    {...register('customDomain', {
                      validate: (value) => {
                        if (getValues('emailDomain') === 'custom' && (!value || value.length === 0)) {
                          return '도메인을 입력해주세요.';
                        }
                        return true;
                      },
                      onBlur: () => {
                        const fullEmail = composeEmail();
                        if (fullEmail) checkEmail(fullEmail);
                      },
                      onChange: () => {
                        resetEmail();
                      },
                    })}
                    placeholder="도메인 입력 (예: example.com)"
                    className="mt-s2"
                  />
                )}
                {emailCheck.isChecking && (
                  <div className="flex items-center gap-s1 mt-s1">
                    <Loader2 size={14} className="animate-spin text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">확인 중...</span>
                  </div>
                )}
              </FormField>

              <FormField
                label="전화번호"
                error={errors.phoneNumber?.message || (phoneNumberCheck.isDuplicate ? phoneNumberCheck.message : undefined)}
                success={phoneNumberCheck.isAvailable ? phoneNumberCheck.message : undefined}
              >
                <div className="relative">
                  <Phone
                    size={18}
                    className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                  />
                  <Input
                    {...register('phoneNumber', {
                      onBlur: () => {
                        const value = getValues('phoneNumber');
                        if (/^\d{3}-\d{4}-\d{4}$/.test(value)) {
                          checkPhoneNumber(value);
                        }
                      },
                      onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                        const digits = e.target.value.replace(/\D/g, '').slice(0, 11);
                        setValue('phoneNumber', formatPhoneNumber(digits));
                        resetPhoneNumber();
                      },
                    })}
                    placeholder="010-1234-5678"
                    maxLength={13}
                    className={cn('pl-10', (phoneNumberCheck.isChecking || phoneNumberCheck.isAvailable) && 'pr-10')}
                  />
                  {phoneNumberCheck.isChecking && (
                    <Loader2 size={16} className="absolute right-s3 top-1/2 -translate-y-1/2 animate-spin text-muted-foreground" />
                  )}
                  {phoneNumberCheck.isAvailable && !phoneNumberCheck.isChecking && (
                    <Check size={16} className="absolute right-s3 top-1/2 -translate-y-1/2 text-green-600" />
                  )}
                </div>
              </FormField>

              <FormField label="학과" error={errors.department?.message}>
                <div className="relative">
                  <Building2
                    size={18}
                    className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                  />
                  <select
                    {...register('department')}
                    className={cn(
                      'w-full h-9 rounded-r2 border border-input bg-background text-foreground pl-10 pr-10 text-sm',
                      'appearance-none cursor-pointer transition-all outline-none',
                      'focus:border-ring focus:ring-ring/50 focus:ring-[3px]',
                      !watch('department') && 'text-muted-foreground',
                    )}
                  >
                    <option value="" className="bg-background text-foreground">학과를 선택하세요</option>
                    {majorOptions.map((college) => (
                      <optgroup key={college.title} label={college.title}>
                        {college.items.map((dept) => (
                          <option key={dept.key} value={dept.value} className="bg-background text-foreground">
                            {dept.value}
                          </option>
                        ))}
                      </optgroup>
                    ))}
                  </select>
                  <ChevronDown
                    size={16}
                    className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
                  />
                </div>
              </FormField>
            </div>

            {/* Step 3: 계정 보안 */}
            <div className={cn('space-y-s4', step !== 2 && 'hidden')}>
              <div className="rounded-r2 bg-muted/50 border border-border p-s4">
                <p className="typo-c1 text-muted-foreground">
                  비밀번호는{' '}
                  <strong className="text-foreground">
                    영문, 숫자를 포함하여 8자 이상
                  </strong>
                  으로 설정해주세요.
                </p>
              </div>

              <FormField label="비밀번호" error={errors.password?.message}>
                <div className="relative">
                  <Lock
                    size={18}
                    className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                  />
                  <Input
                    {...register('password')}
                    type={showPassword ? 'text' : 'password'}
                    placeholder="비밀번호 입력"
                    className="pl-10 pr-10"
                  />
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </FormField>

              <FormField
                label="비밀번호 확인"
                error={errors.passwordConfirm?.message}
                success={passwordConfirmTouched && !errors.passwordConfirm && watch('passwordConfirm') ? '비밀번호가 일치합니다.' : undefined}
              >
                <div className="relative">
                  <Lock
                    size={18}
                    className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                  />
                  <Input
                    {...register('passwordConfirm', {
                      onBlur: () => {
                        setPasswordConfirmTouched(true);
                        trigger('passwordConfirm');
                      },
                    })}
                    type={showPasswordConfirm ? 'text' : 'password'}
                    placeholder="비밀번호 재입력"
                    className="pl-10 pr-10"
                  />
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                    className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
                  >
                    {showPasswordConfirm ? (
                      <EyeOff size={18} />
                    ) : (
                      <Eye size={18} />
                    )}
                  </button>
                </div>
              </FormField>
            </div>

            {/* Step 4: 가입 동기 */}
            <div className={cn('space-y-s4', step !== 3 && 'hidden')}>
              <FormField label={WISH_TITLE} error={submitted ? errors.wishes?.message : undefined}>
                <div className="flex flex-wrap gap-s2">
                  {wishOptions.map((wish) => (
                    <button
                      key={wish}
                      type="button"
                      onClick={() => handleWishToggle(wish)}
                      className={cn(
                        'px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer',
                        selectedWishes.includes(wish)
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'border-border bg-muted text-foreground hover:border-primary/50',
                      )}
                    >
                      {wish}
                    </button>
                  ))}
                </div>
              </FormField>

              <FormField label={INTEREST_TITLE} error={submitted ? errors.interests?.message : undefined}>
                <div className="flex flex-wrap gap-s2">
                  {interestOptions.map((interest) => (
                    <button
                      key={interest}
                      type="button"
                      onClick={() => handleInterestToggle(interest)}
                      className={cn(
                        'px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer',
                        selectedInterests.includes(interest)
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'border-border bg-muted text-foreground hover:border-primary/50',
                      )}
                    >
                      {interest}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={() => handleInterestToggle('기타')}
                    className={cn(
                      'px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer',
                      selectedInterests.includes('기타')
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'border-border bg-muted text-foreground hover:border-primary/50',
                    )}
                  >
                    기타
                  </button>
                </div>
                {selectedInterests.includes('기타') && (
                  <Input
                    {...register('customInterest')}
                    placeholder="관심 분야를 입력해주세요"
                    className="mt-s2"
                    onKeyDown={(e) => e.key === 'Enter' && e.preventDefault()}
                  />
                )}
              </FormField>

              <FormField label={JOIN_ROUTE_TITLE} error={submitted ? errors.joinRoute?.message : undefined}>
                <div className="flex flex-wrap gap-s2">
                  {joinRouteOptions.map((route) => (
                    <button
                      key={route}
                      type="button"
                      onClick={() => handleJoinRouteSelect(route)}
                      className={cn(
                        'px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer',
                        selectedJoinRoute === route
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'border-border bg-muted text-foreground hover:border-primary/50',
                      )}
                    >
                      {route}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={() => handleJoinRouteSelect('기타')}
                    className={cn(
                      'px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer',
                      selectedJoinRoute === '기타'
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'border-border bg-muted text-foreground hover:border-primary/50',
                    )}
                  >
                    기타
                  </button>
                </div>
                {selectedJoinRoute === '기타' && (
                  <Input
                    {...register('customJoinRoute')}
                    placeholder="가입 경로를 입력해주세요"
                    className="mt-s2"
                    onKeyDown={(e) => e.key === 'Enter' && e.preventDefault()}
                  />
                )}
              </FormField>

              <FormField label="가입 동기 (선택)">
                <div className="relative">
                  <FileText
                    size={18}
                    className="absolute left-s3 top-s3 text-muted-foreground"
                  />
                  <textarea
                    {...register('motivation')}
                    placeholder="동아리 가입 동기를 작성해주세요"
                    rows={4}
                    className={cn(
                      'w-full rounded-r2 border border-input bg-transparent pl-10 pr-s3 py-s2 text-sm',
                      'placeholder:text-muted-foreground resize-none transition-all outline-none',
                      'focus:border-ring focus:ring-ring/50 focus:ring-[3px]',
                    )}
                  />
                </div>
              </FormField>

            </div>

            {/* Navigation Buttons */}
            <div className="flex items-center gap-s3 mt-s6">
              <Button
                type="button"
                variant="outline"
                onClick={handlePrev}
                className="flex-1 cursor-pointer"
              >
                <ChevronLeft size={18} />
                이전
              </Button>

              {!isLastStep ? (
                <Button
                  type="button"
                  onClick={handleNext}
                  className="flex-1 cursor-pointer"
                >
                  다음
                  <ChevronRight size={18} />
                </Button>
              ) : (
                <Button
                  type="button"
                  disabled={isSubmitting}
                  onClick={() => {
                    setSubmitted(true);
                    if (useTempStudentId) {
                      setValue('studentId', '00000000');
                    }
                    handleSubmit(onSubmit)();
                  }}
                  className="flex-1 cursor-pointer"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 size={18} className="animate-spin" />
                      가입 중...
                    </>
                  ) : (
                    <>
                      회원가입
                      <Check size={18} />
                    </>
                  )}
                </Button>
              )}
            </div>
          </form>

          {/* Login Link */}
          <p className="text-center text-sm text-muted-foreground mt-s5">
            이미 계정이 있으신가요?{' '}
            <Link
              to="/login"
              className="text-primary font-medium hover:underline"
            >
              로그인
            </Link>
          </p>
        </div>
        )}
      </div>
    </div>
  );
}

// --- Sub Components ---

function FormField({
  label,
  error,
  success,
  mutedError,
  children,
}: {
  label?: string | undefined;
  error?: string | undefined;
  success?: string | undefined;
  mutedError?: boolean | undefined;
  children: React.ReactNode;
}) {
  return (
    <div>
      {label && (
        <label className="block text-sm font-medium text-foreground mb-s2">
          {label}
        </label>
      )}
      {children}
      {error && <p className={cn('mt-s1 text-sm', mutedError ? 'text-muted-foreground' : 'text-destructive')}>{error}</p>}
      {!error && success && <p className="mt-s1 text-sm text-green-600">{success}</p>}
    </div>
  );
}
