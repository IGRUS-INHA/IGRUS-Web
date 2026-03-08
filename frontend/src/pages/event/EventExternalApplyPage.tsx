import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft } from "lucide-react";
import { useRegisterEventExternal } from "@/api/model/event-external-registration/event-external-registration";
import {
  getErrorMessage,
  isConflictError,
  isNotFoundError,
} from "@/utils/error";

const externalApplySchema = z.object({
  name: z
    .string()
    .min(1, "이름을 입력하세요")
    .max(50, "50자 이내로 입력하세요"),
  studentId: z
    .string()
    .min(1, "학번을 입력하세요")
    .max(20, "20자 이내로 입력하세요"),
  phone: z
    .string()
    .min(1, "연락처를 입력하세요")
    .max(20, "20자 이내로 입력하세요"),
  department: z
    .string()
    .min(1, "학과를 입력하세요")
    .max(100, "100자 이내로 입력하세요"),
});

type ExternalApplyForm = z.infer<typeof externalApplySchema>;

export default function EventExternalApplyPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const numericId = Number(eventId);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ExternalApplyForm>({
    resolver: zodResolver(externalApplySchema),
  });

  const { mutate: registerExternal, isPending } = useRegisterEventExternal({
    mutation: {
      onSuccess: () => {
        alert("행사 신청이 완료되었습니다.");
        navigate("/events");
      },
      onError: (error: unknown) => {
        if (isConflictError(error)) {
          alert("이미 신청한 행사입니다. (동일 학번 또는 연락처로 중복 신청)");
        } else if (isNotFoundError(error)) {
          alert("행사를 찾을 수 없습니다.");
        } else {
          alert(getErrorMessage(error));
        }
      },
    },
  });

  const onSubmit = (data: ExternalApplyForm) => {
    registerExternal({
      eventId: numericId,
      data: {
        name: data.name,
        studentId: data.studentId,
        phone: data.phone,
        department: data.department,
      },
    });
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300 max-w-lg mx-auto">
      {/* 브레드크럼 */}
      <button
        type="button"
        onClick={() => navigate("/events")}
        className="flex items-center gap-s2 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer mb-s6"
      >
        <ArrowLeft size={16} />
        행사 목록으로
      </button>

      {/* 헤더 */}
      <div className="mb-s6">
        <p className="text-xs font-bold text-primary tracking-widest mb-s1">
          EXTERNAL REGISTRATION
        </p>
        <h1 className="text-2xl font-bold mb-s2">외부인 행사 신청</h1>
        <p className="text-sm text-muted-foreground">
          비회원도 신청 가능한 행사입니다. 아래 정보를 입력하여 신청해 주세요.
        </p>
      </div>

      {/* 신청 폼 */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="divide-y divide-border">
            {/* 이름 */}
            <div className="px-s6 py-s5">
              <label className="typo-c1 text-muted-foreground mb-s2 block">
                이름 <span className="text-destructive">*</span>
              </label>
              <input
                {...register("name")}
                type="text"
                placeholder="홍길동"
                className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors"
              />
              {errors.name && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.name.message}
                </p>
              )}
            </div>

            {/* 학번 */}
            <div className="px-s6 py-s5">
              <label className="typo-c1 text-muted-foreground mb-s2 block">
                학번 <span className="text-destructive">*</span>
              </label>
              <input
                {...register("studentId")}
                type="text"
                placeholder="12345678"
                className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors"
              />
              {errors.studentId && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.studentId.message}
                </p>
              )}
            </div>

            {/* 연락처 */}
            <div className="px-s6 py-s5">
              <label className="typo-c1 text-muted-foreground mb-s2 block">
                연락처 <span className="text-destructive">*</span>
              </label>
              <input
                {...register("phone")}
                type="tel"
                placeholder="010-0000-0000"
                className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors"
              />
              {errors.phone && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.phone.message}
                </p>
              )}
            </div>

            {/* 학과 */}
            <div className="px-s6 py-s5">
              <label className="typo-c1 text-muted-foreground mb-s2 block">
                학과 <span className="text-destructive">*</span>
              </label>
              <input
                {...register("department")}
                type="text"
                placeholder="컴퓨터공학과"
                className="w-full rounded-lg px-s4 py-s3 border border-border bg-muted/50 text-sm focus:outline-none focus:border-primary transition-colors"
              />
              {errors.department && (
                <p className="typo-c1 text-destructive mt-s1">
                  {errors.department.message}
                </p>
              )}
            </div>
          </div>

          {/* 제출 버튼 */}
          <div className="px-s6 py-s5 border-t border-border">
            <button
              type="submit"
              disabled={isPending}
              className="w-full py-s3 rounded-r4 font-bold bg-primary text-primary-foreground hover:bg-primary/90 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isPending ? "신청 중..." : "신청 제출"}
            </button>
            <p className="typo-c1 text-muted-foreground text-center mt-s3">
              학번 또는 연락처 기준으로 중복 신청이 방지됩니다.
            </p>
          </div>
        </form>
      </div>
    </div>
  );
}
