import { useEffect, useMemo, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  User,
  Lock,
  Mail,
  Phone,
  Building2,
  Eye,
  EyeOff,
  ChevronDown,
  ChevronLeft,
  Check,
  Loader2,
  ExternalLink,
  Wallet,
  Info,
  Key,
  Clock,
  Copy,
  CircleCheck,
  LogIn,
} from "lucide-react";
import {
  useSignup,
  useSignupWithTemporaryStudentId,
  useVerifyPreSignupCode,
  useSendPreSignupCode,
} from "@/api/model/password-authentication/password-authentication";
import type { PasswordSignupRequestEnrollmentStatus } from "@/api/model/models";
import { majorOptions } from "@/constants/majorOptions";
import { domainOptions } from "@/constants/domainOptions";
import { WISH_TITLE, wishOptions, wishToEnum } from "@/constants/wishOptions";
import {
  INTEREST_TITLE,
  interestOptions,
  interestToEnum,
} from "@/constants/interestOptions";
import {
  JOIN_ROUTE_TITLE,
  joinRouteOptions,
  joinRouteToEnum,
} from "@/constants/joinRouteOptions";
import {
  ENROLLMENT_STATUS_TITLE,
  enrollmentStatusOptions,
  enrollmentStatusToEnum,
} from "@/constants/enrollmentStatusOptions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { formatPhoneNumber } from "@/utils";
import { getErrorMessage, hasErrorCode } from "@/utils/error";
import { useSignupDuplicateCheck, useCountdown, useToast } from "@/hooks";

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
      .min(1, "학번을 입력해주세요.")
      .regex(/^\d{8}$/, "학번은 8자리 숫자여야 합니다."),
    name: z
      .string()
      .min(1, "이름을 입력해주세요.")
      .max(50, "이름은 50자 이내여야 합니다."),
    gender: z
      .enum(["MALE", "FEMALE"], {
        message: "성별을 선택해주세요.",
      })
      .optional()
      .refine((v) => v !== undefined, { message: "성별을 선택해주세요." }),
    grade: z
      .number({ message: "학년을 선택해주세요." })
      .min(1, "학년을 선택해주세요.")
      .max(4, "학년은 1~4 사이여야 합니다.")
      .optional()
      .refine((v) => v !== undefined, { message: "학년을 선택해주세요." }),
    enrollmentStatus: z.string().min(1, "재학/휴학 여부를 선택해주세요."),
    emailLocal: z.string().min(1, "이메일 아이디를 입력해주세요."),
    emailDomain: z.string().min(1, "도메인을 선택해주세요."),
    customDomain: z.string().optional(),
    phoneNumber: z
      .string()
      .min(1, "전화번호를 입력해주세요.")
      .regex(/^\d{3}-\d{4}-\d{4}$/, "올바른 전화번호를 입력해주세요."),
    department: z.string().min(1, "학과를 선택해주세요."),
    password: z
      .string()
      .min(8, "비밀번호는 8자 이상이어야 합니다.")
      .max(72, "비밀번호는 72자 이하여야 합니다.")
      .regex(
        /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/,
        "비밀번호는 영문과 숫자를 포함해야 합니다.",
      ),
    passwordConfirm: z.string().min(1, "비밀번호 확인을 입력해주세요."),
    wishes: z.array(z.string()).min(1, "희망 활동을 1개 이상 선택해주세요."),
    interests: z.array(z.string()).min(1, "관심 분야를 1개 이상 선택해주세요."),
    customInterest: z.string().optional(),
    joinRoute: z.string().min(1, "가입 경로를 선택해주세요."),
    customJoinRoute: z.string().optional(),
    nickname: z.string().max(50, "닉네임은 50자 이내여야 합니다.").optional(),
    introduction: z
      .string()
      .max(1000, "자기소개는 1000자 이내여야 합니다.")
      .optional(),
    privacyConsent: z.literal(true, {
      message: "개인정보 처리방침에 동의해주세요.",
    }),
    termsConsent: z.literal(true, {
      message: "이용약관에 동의해주세요.",
    }),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: "비밀번호가 일치하지 않습니다.",
    path: ["passwordConfirm"],
  });

type SignupFormData = z.infer<typeof signupSchema>;

// --- Component ---

export default function SignupPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [copied, setCopied] = useState(false);
  const [memberType, setMemberType] = useState<"member" | "guest">();
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [serverError, setServerError] = useState<string>();
  const [passwordConfirmTouched, setPasswordConfirmTouched] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [useTempStudentId, setUseTempStudentId] = useState(false);
  const [signupCompleted, setSignupCompleted] = useState(false);
  const [completedTempStudentId, setCompletedTempStudentId] = useState<
    string | null
  >(null);
  const signupMutation = useSignup();
  const signupTempMutation = useSignupWithTemporaryStudentId();
  const verifyEmailMutation = useVerifyPreSignupCode();
  const resendVerificationMutation = useSendPreSignupCode();

  // 이메일 인증 상태
  const [emailVerified, setEmailVerified] = useState(false);
  const [verificationCode, setVerificationCode] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [verificationToken, setVerificationToken] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [verificationError, setVerificationError] = useState<string>();

  // 인증 코드 유효시간 타이머 (10분) / 재발송 쿨다운 (10초)
  const codeTimer = useCountdown({ initialSeconds: 600 });
  const resendCooldown = useCountdown({ initialSeconds: 10 });
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
      studentId: "",
      name: "",
      gender: undefined as unknown as "MALE" | "FEMALE",
      grade: undefined as unknown as number,
      enrollmentStatus: "",
      emailLocal: "",
      emailDomain: "inha.edu",
      customDomain: "",
      phoneNumber: "",
      department: "",
      password: "",
      passwordConfirm: "",
      wishes: [],
      interests: [],
      customInterest: "",
      joinRoute: "",
      customJoinRoute: "",
      nickname: "",
      introduction: "",
      privacyConsent: undefined as unknown as true,
      termsConsent: undefined as unknown as true,
    },
    mode: "onTouched",
  });

  const watchedPassword = watch("password");
  const selectedWishes = watch("wishes") ?? [];
  const selectedInterests = watch("interests") ?? [];
  const selectedJoinRoute = watch("joinRoute") ?? "";
  const emailDomain = watch("emailDomain");
  const emailLocalValue = watch("emailLocal");
  const customDomainValue = watch("customDomain");
  const watchedGrade = watch("grade");

  const currentFullEmail = useMemo(() => {
    const domain = emailDomain === "custom" ? customDomainValue : emailDomain;
    return emailLocalValue && domain ? `${emailLocalValue}@${domain}` : "";
  }, [emailLocalValue, emailDomain, customDomainValue]);

  // 학년 변경 시 임시 학번 체크 해제
  useEffect(() => {
    if (watchedGrade !== 1) {
      setUseTempStudentId(false);
    }
  }, [watchedGrade]);

  // 이메일 변경 시 인증 상태 초기화
  useEffect(() => {
    if (verifiedEmail && currentFullEmail !== verifiedEmail) {
      setEmailVerified(false);
      setVerificationCode("");
      setVerificationToken("");
      setCodeSent(false);
      setVerificationError(undefined);
      codeTimer.stop();
      resendCooldown.stop();
    }
  }, [currentFullEmail, verifiedEmail, codeTimer, resendCooldown]);

  // 비밀번호 변경 시 비밀번호 확인 필드 재검증
  useEffect(() => {
    if (passwordConfirmTouched) {
      trigger("passwordConfirm");
    }
  }, [watchedPassword, passwordConfirmTouched, trigger]);

  const handleWishToggle = (wish: string) => {
    const current = getValues("wishes") ?? [];
    const updated = current.includes(wish)
      ? current.filter((w) => w !== wish)
      : [...current, wish];
    setValue("wishes", updated);
  };

  const handleInterestToggle = (interest: string) => {
    const current = getValues("interests") ?? [];
    if (interest === "기타") {
      if (current.includes("기타")) {
        setValue(
          "interests",
          current.filter((i) => i !== "기타"),
        );
        setValue("customInterest", "");
      } else {
        setValue("interests", [...current, "기타"]);
      }
    } else {
      const updated = current.includes(interest)
        ? current.filter((i) => i !== interest)
        : [...current, interest];
      setValue("interests", updated);
    }
  };

  const handleJoinRouteSelect = (route: string) => {
    if (selectedJoinRoute === route) {
      setValue("joinRoute", "");
    } else {
      setValue("joinRoute", route);
    }
    if (route !== "기타") {
      setValue("customJoinRoute", "");
    }
  };

  const handleSendCode = async () => {
    const valid = await trigger(["emailLocal", "emailDomain", "customDomain"]);
    if (!valid) return;
    if (emailVerified && currentFullEmail === verifiedEmail) return;
    if (emailCheck.isDuplicate) return;
    if (sendingCode) return;

    setSendingCode(true);
    setVerificationError(undefined);
    try {
      await resendVerificationMutation.mutateAsync({
        data: { email: currentFullEmail },
      });
      setCodeSent(true);
      codeTimer.restart();
      resendCooldown.restart();
    } catch (error) {
      setVerificationError(getErrorMessage(error));
    } finally {
      setSendingCode(false);
    }
  };

  const handleVerifyCode = async () => {
    setVerifyingCode(true);
    setVerificationError(undefined);
    try {
      const response = await verifyEmailMutation.mutateAsync({
        data: { email: currentFullEmail, code: verificationCode },
      });
      const responseData = response.data as unknown as {
        verificationToken?: string;
      };
      setVerificationToken(responseData.verificationToken ?? "");
      setEmailVerified(true);
      setVerifiedEmail(currentFullEmail);
      codeTimer.stop();
    } catch (error) {
      if (hasErrorCode(error, "VERIFICATION_CODE_EXPIRED")) {
        setVerificationError("인증 코드가 만료되었습니다. 재발송해주세요.");
      } else if (hasErrorCode(error, "VERIFICATION_ATTEMPTS_EXCEEDED")) {
        setVerificationError("인증 시도 횟수를 초과했습니다. 재발송해주세요.");
      } else if (hasErrorCode(error, "VERIFICATION_CODE_INVALID")) {
        setVerificationError("인증 코드가 일치하지 않습니다.");
      } else {
        setVerificationError(getErrorMessage(error));
      }
    } finally {
      setVerifyingCode(false);
    }
  };

  const handleResendCode = async () => {
    setSendingCode(true);
    setVerificationError(undefined);
    try {
      await resendVerificationMutation.mutateAsync({
        data: { email: currentFullEmail },
      });
      resendCooldown.restart();
      codeTimer.restart();
      setVerificationCode("");
    } catch (error) {
      setVerificationError(getErrorMessage(error));
    } finally {
      setSendingCode(false);
    }
  };

  const handleCopyAccount = async () => {
    await navigator.clipboard.writeText("토스뱅크 1002-3803-2581");
    setCopied(true);
    toast.success("클립보드에 복사되었습니다.");
    setTimeout(() => setCopied(false), 2000);
  };

  const composeEmail = () => {
    const local = getValues("emailLocal");
    const domain = getValues("emailDomain");
    const custom = getValues("customDomain");
    const fullDomain = domain === "custom" ? custom : domain;
    if (local && fullDomain) return `${local}@${fullDomain}`;
    return "";
  };

  const onSubmit = async (data: SignupFormData) => {
    setServerError(undefined);
    if (!emailVerified) {
      setVerificationError("이메일 인증을 완료해주세요.");
      return;
    }
    try {
      const domain =
        data.emailDomain === "custom" ? data.customDomain : data.emailDomain;
      const fullEmail = `${data.emailLocal}@${domain}`;

      const commonFields = {
        password: data.password,
        name: data.name,
        email: fullEmail,
        phoneNumber: formatPhoneNumber(data.phoneNumber),
        department: data.department,
        gender: data.gender!,
        grade: data.grade!,
        enrollmentStatus: (enrollmentStatusToEnum[data.enrollmentStatus] ??
          "ENROLLED") as PasswordSignupRequestEnrollmentStatus,
        wishes: data.wishes
          .map((w) => wishToEnum[w])
          .filter((v): v is NonNullable<typeof v> => Boolean(v)),
        interests: data.interests
          .map((i) => interestToEnum[i])
          .filter((v): v is NonNullable<typeof v> => Boolean(v)),
        customInterest: data.interests.includes("기타")
          ? data.customInterest
          : undefined,
        joinRoute: joinRouteToEnum[data.joinRoute] ?? "OTHER",
        customJoinRoute:
          data.joinRoute === "기타" ? data.customJoinRoute : undefined,
        nickname: data.nickname || undefined,
        introduction: data.introduction || undefined,
        privacyConsent: data.privacyConsent,
        verificationToken,
      };

      if (useTempStudentId) {
        // 임시 학번 회원가입
        const result = await signupTempMutation.mutateAsync({
          data: commonFields,
        });

        const responseData = result.data as unknown as {
          temporaryStudentId?: string;
        };
        setCompletedTempStudentId(responseData.temporaryStudentId ?? null);
        setSignupCompleted(true);
      } else {
        // 일반 회원가입
        await signupMutation.mutateAsync({
          data: {
            studentId: data.studentId,
            ...commonFields,
          },
        });

        setSignupCompleted(true);
      }
    } catch (error: unknown) {
      if (hasErrorCode(error, "DUPLICATE_STUDENT_ID")) {
        setError("studentId", { message: "이미 가입된 학번입니다." });
      } else if (hasErrorCode(error, "DUPLICATE_EMAIL")) {
        setError("emailLocal", { message: "이미 존재하는 이메일입니다." });
      } else if (hasErrorCode(error, "DUPLICATE_PHONE_NUMBER")) {
        setError("phoneNumber", { message: "이미 등록된 전화번호입니다." });
      } else if (hasErrorCode(error, "TEMP_STUDENT_ID_NOT_AVAILABLE")) {
        setServerError("임시 학번 발급은 1월~2월에만 가능합니다.");
      } else if (hasErrorCode(error, "RE_REGISTRATION_NOT_ALLOWED")) {
        setServerError("탈퇴 후 재가입 제한 기간(5일)이 지나지 않았습니다.");
      } else {
        setServerError(getErrorMessage(error));
      }
    }
  };

  const slackInviteUrl = import.meta.env.VITE_SLACK_INVITE_URL;

  if (signupCompleted) {
    return (
      <div className="flex items-center justify-center min-h-full py-s6 px-s4">
        <div className="w-full max-w-lg animate-in slide-in-from-bottom-6 duration-500">
          {/* 완료 아이콘 및 메시지 */}
          <div className="text-center mb-s7">
            <div className="mx-auto w-16 h-16 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center mb-s5">
              <CircleCheck
                size={36}
                className="text-green-600 dark:text-green-400"
              />
            </div>
            <h1 className="typo-h2 text-foreground">회원가입 완료!</h1>
            <p className="typo-b2 text-muted-foreground mt-s2">
              IGRUS 회원가입이 성공적으로 완료되었습니다.
            </p>
            {completedTempStudentId && (
              <div className="mt-s4 inline-flex items-center gap-s2 bg-primary/10 text-primary rounded-r2 px-s4 py-s2">
                <Key size={16} />
                <span className="typo-b2 font-medium">
                  임시 학번: {completedTempStudentId}
                </span>
              </div>
            )}
          </div>

          {/* 슬랙 안내 섹션 */}
          {slackInviteUrl && (
            <div className="rounded-r4 border bg-card p-s6 shadow-sm mb-s5">
              <h2 className="typo-h4 text-foreground mb-s4">
                슬랙 채널에 참여하세요
              </h2>
              <p className="typo-b2 text-muted-foreground mb-s5">
                IGRUS의 주요 소통은 슬랙에서 이루어집니다.
                <br />
                아래 버튼을 눌러 슬랙 워크스페이스에 참여해 주세요.
              </p>
              <a
                href={slackInviteUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="block"
              >
                <Button type="button" className="w-full cursor-pointer">
                  IGRUS 슬랙 참여하기
                  <ExternalLink size={16} />
                </Button>
              </a>
            </div>
          )}

          {/* 로그인 이동 버튼 */}
          <Button
            type="button"
            variant={slackInviteUrl ? "outline" : "default"}
            className="w-full cursor-pointer"
            onClick={() => navigate("/login")}
          >
            <LogIn size={16} />
            로그인하러 가기
          </Button>
        </div>
      </div>
    );
  }

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

        {/* Membership Type Selection */}
        {!memberType && (
          <div className="rounded-r4 border bg-card p-s6 shadow-sm space-y-s4">
            <button
              type="button"
              onClick={() => {
                setMemberType("member");
                setValue("wishes", []);
                setValue("interests", []);
                setValue("joinRoute", "");
              }}
              className="w-full rounded-r2 border border-border bg-muted p-s5 text-left hover:border-primary/50 transition-all cursor-pointer"
            >
              <div className="flex items-center gap-s2 mb-s1">
                <Wallet size={18} className="text-primary" />
                <span className="typo-h4 text-foreground">회원으로 가입</span>
              </div>
              <p className="text-sm text-muted-foreground">
                회비를 납부하고 동아리 활동에 참여합니다
              </p>
            </button>

            <button
              type="button"
              onClick={() => {
                setMemberType("guest");
                // 비회원은 기타 섹션을 입력받지 않으므로 스키마·API 필수값을 "기타"로 채움
                // (wishes의 "기타"는 enum 매핑에서 걸러져 빈 배열로 전송됨)
                setValue("wishes", ["기타"]);
                setValue("interests", ["기타"]);
                setValue("joinRoute", "기타");
              }}
              className="w-full rounded-r2 border border-border bg-muted p-s5 text-left hover:border-primary/50 transition-all cursor-pointer"
            >
              <div className="flex items-center gap-s2 mb-s1">
                <User size={18} className="text-primary" />
                <span className="typo-h4 text-foreground">비회원으로 가입</span>
              </div>
              <p className="text-sm text-muted-foreground">
                회비 납부 없이 가입합니다
              </p>
            </button>

            <p className="text-center text-sm text-muted-foreground">
              이미 계정이 있으신가요?{" "}
              <Link
                to="/login"
                className="text-primary font-medium hover:underline"
              >
                로그인
              </Link>
            </p>
          </div>
        )}

        {/* Form Card */}
        {memberType && (
          <div className="rounded-r4 border bg-card p-s6 shadow-sm">
            {serverError && (
              <div className="mb-s5 rounded-r2 bg-destructive/10 border border-destructive/20 p-s4 text-sm text-destructive">
                {serverError}
              </div>
            )}

            <form onSubmit={(e) => e.preventDefault()}>
              <div className="space-y-s5">
                {!useTempStudentId && (
                  <FormField
                    label="학번"
                    error={
                      errors.studentId?.message ||
                      (studentIdCheck.isDuplicate
                        ? studentIdCheck.message
                        : undefined)
                    }
                    success={
                      studentIdCheck.isAvailable
                        ? studentIdCheck.message
                        : undefined
                    }
                  >
                    <div className="relative">
                      <User
                        size={18}
                        className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                      />
                      <Input
                        {...register("studentId", {
                          onBlur: () => {
                            const value = getValues("studentId");
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
                        className={cn(
                          "pl-10",
                          (studentIdCheck.isChecking ||
                            studentIdCheck.isAvailable) &&
                            "pr-10",
                        )}
                      />
                      {studentIdCheck.isChecking && (
                        <Loader2
                          size={16}
                          className="absolute right-s3 top-1/2 -translate-y-1/2 animate-spin text-muted-foreground"
                        />
                      )}
                      {studentIdCheck.isAvailable &&
                        !studentIdCheck.isChecking && (
                          <Check
                            size={16}
                            className="absolute right-s3 top-1/2 -translate-y-1/2 text-green-600"
                          />
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
                          setValue("studentId", "");
                          clearErrors("studentId");
                          resetStudentId();
                          setValue("grade", 1, { shouldValidate: true });
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
                      학번이 나오면 <strong>마이페이지</strong>에서 실제
                      학번으로 변경해주세요.
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
                      {...register("name")}
                      placeholder="홍길동"
                      className="pl-10"
                    />
                  </div>
                </FormField>

                <div className="rounded-r2 bg-muted/50 border border-border p-s4">
                  <p className="typo-c1 text-muted-foreground">
                    비밀번호는{" "}
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
                      {...register("password")}
                      type={showPassword ? "text" : "password"}
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
                  success={
                    passwordConfirmTouched &&
                    !errors.passwordConfirm &&
                    watch("passwordConfirm")
                      ? "비밀번호가 일치합니다."
                      : undefined
                  }
                >
                  <div className="relative">
                    <Lock
                      size={18}
                      className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                    />
                    <Input
                      {...register("passwordConfirm", {
                        onBlur: () => {
                          setPasswordConfirmTouched(true);
                          trigger("passwordConfirm");
                        },
                      })}
                      type={showPasswordConfirm ? "text" : "password"}
                      placeholder="비밀번호 재입력"
                      className="pl-10 pr-10"
                    />
                    <button
                      type="button"
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() =>
                        setShowPasswordConfirm(!showPasswordConfirm)
                      }
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

                <FormField label="성별" error={errors.gender?.message}>
                  <div className="grid grid-cols-2 gap-s3">
                    {(["MALE", "FEMALE"] as const).map((g) => (
                      <button
                        key={g}
                        type="button"
                        onClick={() =>
                          setValue("gender", g, { shouldValidate: true })
                        }
                        className={cn(
                          "h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer",
                          watch("gender") === g
                            ? "bg-primary text-primary-foreground border-primary"
                            : "bg-muted border-border text-foreground hover:border-primary/50",
                        )}
                      >
                        {g === "MALE" ? "남성" : "여성"}
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
                          setValue("grade", g, { shouldValidate: true })
                        }
                        className={cn(
                          "h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer",
                          watch("grade") === g
                            ? "bg-primary text-primary-foreground border-primary"
                            : "bg-muted border-border text-foreground hover:border-primary/50",
                        )}
                      >
                        {g}학년
                      </button>
                    ))}
                  </div>
                </FormField>

                <FormField
                  label={ENROLLMENT_STATUS_TITLE}
                  error={errors.enrollmentStatus?.message}
                >
                  <div className="grid grid-cols-3 gap-s2">
                    {enrollmentStatusOptions.map((status) => (
                      <button
                        key={status}
                        type="button"
                        onClick={() =>
                          setValue("enrollmentStatus", status, {
                            shouldValidate: true,
                          })
                        }
                        className={cn(
                          "h-10 rounded-r2 border text-sm font-medium transition-all cursor-pointer",
                          watch("enrollmentStatus") === status
                            ? "bg-primary text-primary-foreground border-primary"
                            : "bg-muted border-border text-foreground hover:border-primary/50",
                        )}
                      >
                        {status}
                      </button>
                    ))}
                  </div>
                </FormField>

                <FormField
                  label="이메일"
                  error={
                    errors.emailLocal?.message ||
                    errors.customDomain?.message ||
                    (emailCheck.isDuplicate ? emailCheck.message : undefined)
                  }
                  success={
                    emailCheck.isAvailable ? emailCheck.message : undefined
                  }
                >
                  <div className="flex items-center gap-s2">
                    <div className="relative flex-1">
                      <Mail
                        size={18}
                        className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                      />
                      <Input
                        {...register("emailLocal", {
                          onBlur: () => {
                            const fullEmail = composeEmail();
                            if (fullEmail) checkEmail(fullEmail);
                          },
                          onChange: () => {
                            resetEmail();
                          },
                        })}
                        placeholder="아이디"
                        disabled={emailVerified}
                        className="pl-10"
                      />
                    </div>
                    <span className="text-muted-foreground font-bold shrink-0">
                      @
                    </span>
                    <div className="relative flex-1">
                      <select
                        {...register("emailDomain", {
                          onChange: () => {
                            resetEmail();
                            // 도메인이 커스텀이 아니고 로컬파트가 있으면 즉시 체크
                            setTimeout(() => {
                              const fullEmail = composeEmail();
                              if (
                                fullEmail &&
                                getValues("emailDomain") !== "custom"
                              ) {
                                checkEmail(fullEmail);
                              }
                            }, 0);
                          },
                        })}
                        disabled={emailVerified}
                        className={cn(
                          "w-full h-9 rounded-r2 border border-input bg-background text-foreground px-s3 text-sm",
                          "appearance-none cursor-pointer transition-all outline-none",
                          "focus:border-ring focus:ring-ring/50 focus:ring-[3px]",
                        )}
                      >
                        {domainOptions.map((d) => (
                          <option
                            key={d.value}
                            value={d.value}
                            className="bg-background text-foreground"
                          >
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
                  {emailDomain === "custom" && (
                    <Input
                      {...register("customDomain", {
                        validate: (value) => {
                          if (
                            getValues("emailDomain") === "custom" &&
                            (!value || value.length === 0)
                          ) {
                            return "도메인을 입력해주세요.";
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
                      disabled={emailVerified}
                      className="mt-s2"
                    />
                  )}
                  {emailCheck.isChecking && (
                    <div className="flex items-center gap-s1 mt-s1">
                      <Loader2
                        size={14}
                        className="animate-spin text-muted-foreground"
                      />
                      <span className="text-sm text-muted-foreground">
                        확인 중...
                      </span>
                    </div>
                  )}

                  {/* 이메일 인증하기 버튼 (버튼을 눌러야 인증 코드 발송) */}
                  {!codeSent && !emailVerified && (
                    <div className="mt-s3 space-y-s2">
                      <Button
                        type="button"
                        variant="outline"
                        onClick={handleSendCode}
                        disabled={
                          sendingCode ||
                          emailCheck.isChecking ||
                          emailCheck.isDuplicate
                        }
                        className="w-full cursor-pointer"
                      >
                        {sendingCode ? (
                          <>
                            <Loader2 size={16} className="animate-spin" />
                            인증 코드 발송 중...
                          </>
                        ) : (
                          "이메일 인증하기"
                        )}
                      </Button>
                      {verificationError && (
                        <p className="text-sm text-destructive">
                          {verificationError}
                        </p>
                      )}
                    </div>
                  )}

                  {/* 인증 코드 입력 UI */}
                  {codeSent && !emailVerified && (
                    <div className="mt-s3 space-y-s3">
                      <div className="flex items-center gap-s2">
                        <div className="relative flex-1">
                          <Key
                            size={18}
                            className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                          />
                          <Input
                            type="text"
                            placeholder="6자리 인증 코드"
                            value={verificationCode}
                            onChange={(e) =>
                              setVerificationCode(
                                e.target.value.replace(/\D/g, "").slice(0, 6),
                              )
                            }
                            maxLength={6}
                            className="pl-10 tracking-widest"
                          />
                        </div>
                        <Button
                          type="button"
                          onClick={handleVerifyCode}
                          disabled={
                            verifyingCode ||
                            verificationCode.length !== 6 ||
                            codeTimer.isExpired
                          }
                          className="shrink-0 cursor-pointer"
                        >
                          {verifyingCode ? (
                            <Loader2 size={16} className="animate-spin" />
                          ) : (
                            "인증 확인"
                          )}
                        </Button>
                      </div>

                      <p className="text-sm text-muted-foreground">
                        이메일이 오지 않으면 스팸 메일함을 확인해주세요.
                      </p>

                      <div className="flex items-center gap-s5">
                        {codeTimer.isRunning && (
                          <div
                            className={cn(
                              "flex items-center gap-s1 text-sm transition-colors",
                              codeTimer.remaining <= 60
                                ? "text-destructive"
                                : codeTimer.remaining <= 180
                                  ? "text-amber-500"
                                  : "text-primary",
                            )}
                          >
                            <Clock size={14} />
                            <span>남은 시간 {codeTimer.formatted}</span>
                          </div>
                        )}
                        {codeTimer.isExpired && (
                          <p className="text-sm text-destructive">
                            인증 코드가 만료되었습니다.
                          </p>
                        )}
                        <button
                          type="button"
                          onClick={handleResendCode}
                          disabled={sendingCode || !resendCooldown.isExpired}
                          className="text-sm text-muted-foreground hover:text-primary transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {sendingCode
                            ? "재발송 중..."
                            : !resendCooldown.isExpired
                              ? `재발송 (${resendCooldown.remaining}초)`
                              : "인증 코드 재발송"}
                        </button>
                      </div>

                      {verificationError && (
                        <p className="text-sm text-destructive">
                          {verificationError}
                        </p>
                      )}
                    </div>
                  )}

                  {/* 인증 완료 */}
                  {emailVerified && (
                    <div className="flex items-center gap-s1 mt-s3 text-green-600">
                      <Check size={16} />
                      <span className="text-sm font-medium">
                        이메일 인증이 완료되었습니다
                      </span>
                    </div>
                  )}
                </FormField>

                <FormField
                  label="전화번호"
                  error={
                    errors.phoneNumber?.message ||
                    (phoneNumberCheck.isDuplicate
                      ? phoneNumberCheck.message
                      : undefined)
                  }
                  success={
                    phoneNumberCheck.isAvailable
                      ? phoneNumberCheck.message
                      : undefined
                  }
                >
                  <div className="relative">
                    <Phone
                      size={18}
                      className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
                    />
                    <Input
                      {...register("phoneNumber", {
                        onBlur: () => {
                          const value = getValues("phoneNumber");
                          if (/^\d{3}-\d{4}-\d{4}$/.test(value)) {
                            checkPhoneNumber(value);
                          }
                        },
                        onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                          const digits = e.target.value
                            .replace(/\D/g, "")
                            .slice(0, 11);
                          setValue("phoneNumber", formatPhoneNumber(digits));
                          resetPhoneNumber();
                        },
                      })}
                      placeholder="010-1234-5678"
                      maxLength={13}
                      className={cn(
                        "pl-10",
                        (phoneNumberCheck.isChecking ||
                          phoneNumberCheck.isAvailable) &&
                          "pr-10",
                      )}
                    />
                    {phoneNumberCheck.isChecking && (
                      <Loader2
                        size={16}
                        className="absolute right-s3 top-1/2 -translate-y-1/2 animate-spin text-muted-foreground"
                      />
                    )}
                    {phoneNumberCheck.isAvailable &&
                      !phoneNumberCheck.isChecking && (
                        <Check
                          size={16}
                          className="absolute right-s3 top-1/2 -translate-y-1/2 text-green-600"
                        />
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
                      {...register("department")}
                      className={cn(
                        "w-full h-9 rounded-r2 border border-input bg-background text-foreground pl-10 pr-10 text-sm",
                        "appearance-none cursor-pointer transition-all outline-none",
                        "focus:border-ring focus:ring-ring/50 focus:ring-[3px]",
                        !watch("department") && "text-muted-foreground",
                      )}
                    >
                      <option
                        value=""
                        className="bg-background text-foreground"
                      >
                        학과를 선택하세요
                      </option>
                      {majorOptions.map((college) => (
                        <optgroup key={college.title} label={college.title}>
                          {college.items.map((dept) => (
                            <option
                              key={dept.key}
                              value={dept.value}
                              className="bg-background text-foreground"
                            >
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

                <div
                  className={cn(
                    "space-y-s5",
                    memberType !== "member" && "hidden",
                  )}
                >
                  <FormField
                    label={WISH_TITLE}
                    error={submitted ? errors.wishes?.message : undefined}
                  >
                    <div className="flex flex-wrap gap-s2">
                      {wishOptions.map((wish) => (
                        <button
                          key={wish}
                          type="button"
                          onClick={() => handleWishToggle(wish)}
                          className={cn(
                            "px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer",
                            selectedWishes.includes(wish)
                              ? "bg-primary text-primary-foreground border-primary"
                              : "border-border bg-muted text-foreground hover:border-primary/50",
                          )}
                        >
                          {wish}
                        </button>
                      ))}
                    </div>
                  </FormField>

                  <FormField
                    label={INTEREST_TITLE}
                    error={submitted ? errors.interests?.message : undefined}
                  >
                    <div className="flex flex-wrap gap-s2">
                      {interestOptions.map((interest) => (
                        <button
                          key={interest}
                          type="button"
                          onClick={() => handleInterestToggle(interest)}
                          className={cn(
                            "px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer",
                            selectedInterests.includes(interest)
                              ? "bg-primary text-primary-foreground border-primary"
                              : "border-border bg-muted text-foreground hover:border-primary/50",
                          )}
                        >
                          {interest}
                        </button>
                      ))}
                      <button
                        type="button"
                        onClick={() => handleInterestToggle("기타")}
                        className={cn(
                          "px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer",
                          selectedInterests.includes("기타")
                            ? "bg-primary text-primary-foreground border-primary"
                            : "border-border bg-muted text-foreground hover:border-primary/50",
                        )}
                      >
                        기타
                      </button>
                    </div>
                    {selectedInterests.includes("기타") && (
                      <Input
                        {...register("customInterest")}
                        placeholder="관심 분야를 입력해주세요"
                        className="mt-s2"
                        onKeyDown={(e) =>
                          e.key === "Enter" && e.preventDefault()
                        }
                      />
                    )}
                  </FormField>

                  <FormField
                    label={JOIN_ROUTE_TITLE}
                    error={submitted ? errors.joinRoute?.message : undefined}
                  >
                    <div className="flex flex-wrap gap-s2">
                      {joinRouteOptions.map((route) => (
                        <button
                          key={route}
                          type="button"
                          onClick={() => handleJoinRouteSelect(route)}
                          className={cn(
                            "px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer",
                            selectedJoinRoute === route
                              ? "bg-primary text-primary-foreground border-primary"
                              : "border-border bg-muted text-foreground hover:border-primary/50",
                          )}
                        >
                          {route}
                        </button>
                      ))}
                      <button
                        type="button"
                        onClick={() => handleJoinRouteSelect("기타")}
                        className={cn(
                          "px-s3 py-s2 rounded-full border text-sm transition-all cursor-pointer",
                          selectedJoinRoute === "기타"
                            ? "bg-primary text-primary-foreground border-primary"
                            : "border-border bg-muted text-foreground hover:border-primary/50",
                        )}
                      >
                        기타
                      </button>
                    </div>
                    {selectedJoinRoute === "기타" && (
                      <Input
                        {...register("customJoinRoute")}
                        placeholder="가입 경로를 입력해주세요"
                        className="mt-s2"
                        onKeyDown={(e) =>
                          e.key === "Enter" && e.preventDefault()
                        }
                      />
                    )}
                  </FormField>

                  <FormField
                    label="닉네임 (선택)"
                    error={errors.nickname?.message}
                  >
                    <Input
                      {...register("nickname")}
                      placeholder="공개 프로필에 표시될 이름 (미설정 시 이름으로 표시)"
                      onKeyDown={(e) => e.key === "Enter" && e.preventDefault()}
                    />
                  </FormField>
                </div>

                <div className="space-y-s3 rounded-r2 border border-border p-s4">
                  <FormField error={errors.privacyConsent?.message}>
                    <div className="flex items-center justify-between">
                      <label className="flex items-center gap-s3 cursor-pointer group">
                        <input
                          type="checkbox"
                          {...register("privacyConsent")}
                          className="cursor-pointer accent-primary"
                        />
                        <span className="text-sm text-muted-foreground group-hover:text-foreground transition-colors">
                          개인정보 처리방침에 동의합니다 (필수)
                        </span>
                      </label>
                      <Link
                        to="/privacy"
                        target="_blank"
                        className="text-muted-foreground hover:text-foreground transition-colors"
                      >
                        <ExternalLink size={16} />
                      </Link>
                    </div>
                  </FormField>

                  <FormField error={errors.termsConsent?.message}>
                    <div className="flex items-center justify-between">
                      <label className="flex items-center gap-s3 cursor-pointer group">
                        <input
                          type="checkbox"
                          {...register("termsConsent")}
                          className="cursor-pointer accent-primary"
                        />
                        <span className="text-sm text-muted-foreground group-hover:text-foreground transition-colors">
                          이용약관에 동의합니다 (필수)
                        </span>
                      </label>
                      <Link
                        to="/terms"
                        target="_blank"
                        className="text-muted-foreground hover:text-foreground transition-colors"
                      >
                        <ExternalLink size={16} />
                      </Link>
                    </div>
                  </FormField>
                </div>
              </div>

              {/* 회비 납부 안내 (회원 전용) */}
              {memberType === "member" && (
                <div className="mt-s7 rounded-r2 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 p-s4 space-y-s2">
                  <div className="flex items-center gap-s2">
                    <Wallet size={16} className="text-amber-600 shrink-0" />
                    <p className="text-sm font-bold text-amber-700 dark:text-amber-400">
                      회비 2만원을 납부해주세요
                    </p>
                  </div>
                  <p className="text-sm text-amber-700 dark:text-amber-400">
                    입금자명 양식: 학번 2자리+이름 (ex. 26김아그)
                  </p>
                  <p className="text-sm text-amber-700 dark:text-amber-400 flex items-center">
                    입금계좌: 토스뱅크 1002-3803-2581
                    <button
                      type="button"
                      onClick={handleCopyAccount}
                      className="inline-flex items-center ml-s2 hover:text-primary transition-colors cursor-pointer"
                      title="계좌번호 복사"
                    >
                      {copied ? (
                        <Check size={14} className="text-green-600" />
                      ) : (
                        <Copy size={14} />
                      )}
                    </button>
                  </p>
                  <p className="text-sm text-amber-700 dark:text-amber-400">
                    입금자명 양식을 지키지 않으실 경우, 회비 납부 명단에서
                    누락될 수 있습니다.
                  </p>
                </div>
              )}

              {/* Submit */}
              <Button
                type="button"
                disabled={isSubmitting}
                onClick={() => {
                  setSubmitted(true);
                  if (useTempStudentId) {
                    setValue("studentId", "00000000");
                  }
                  handleSubmit(onSubmit)();
                }}
                className="w-full mt-s6 cursor-pointer"
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

              <button
                type="button"
                onClick={() => setMemberType(undefined)}
                className="flex items-center justify-center gap-s1 w-full mt-s4 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
              >
                <ChevronLeft size={16} />
                가입 유형 다시 선택
              </button>
            </form>

            {/* Login Link */}
            <p className="text-center text-sm text-muted-foreground mt-s5">
              이미 계정이 있으신가요?{" "}
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
      {error && (
        <p
          className={cn(
            "mt-s3 text-sm",
            mutedError ? "text-muted-foreground" : "text-destructive",
          )}
        >
          {error}
        </p>
      )}
      {!error && success && (
        <p className="mt-s3 text-sm text-green-600">{success}</p>
      )}
    </div>
  );
}
