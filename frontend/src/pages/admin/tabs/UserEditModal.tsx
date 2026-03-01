import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import Swal from "sweetalert2";
import {
  useGetUserDetail,
  useEditUserInfo,
} from "@/api/model/admin-user-management/admin-user-management";
import type { AdminEditUserInfoRequest } from "@/api/model/models/adminEditUserInfoRequest";
import type { UserDetailResponse } from "@/api/model/models/userDetailResponse";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { majorOptions } from "@/constants/majorOptions";
import { cn } from "@/lib/utils";
import { formatPhoneNumber } from "@/utils";
import { getErrorMessage, hasErrorCode } from "@/utils/error";
import { useUIStore } from "@/stores";
import { useQueryClient } from "@tanstack/react-query";
import { userEditSchema, type UserEditFormData } from "./userEditSchema";

const ENROLLMENT_OPTIONS = [
  { value: "ENROLLED", label: "재학" },
  { value: "GENERAL_LEAVE", label: "휴학 (일반)" },
  { value: "MILITARY_LEAVE", label: "휴학 (군)" },
] as const;

const GENDER_OPTIONS = [
  { value: "MALE", label: "남" },
  { value: "FEMALE", label: "여" },
] as const;

const GRADE_OPTIONS = [1, 2, 3, 4] as const;

interface UserEditModalProps {
  userId: number;
  onClose: () => void;
}

function buildPatchPayload(
  original: UserDetailResponse,
  edited: UserEditFormData,
): AdminEditUserInfoRequest {
  const payload: AdminEditUserInfoRequest = {};
  if (edited.studentId !== original.studentId)
    payload.studentId = edited.studentId;
  if (edited.email !== original.email) payload.email = edited.email;
  if (edited.name !== original.name) payload.name = edited.name;
  if (edited.phoneNumber !== original.phoneNumber)
    payload.phoneNumber = edited.phoneNumber;
  if (edited.department !== original.department)
    payload.department = edited.department;
  if (edited.grade !== original.grade) payload.grade = edited.grade;
  if (edited.enrollmentStatus !== original.enrollmentStatus)
    payload.enrollmentStatus = edited.enrollmentStatus as NonNullable<
      AdminEditUserInfoRequest["enrollmentStatus"]
    >;
  if (edited.gender !== original.gender)
    payload.gender = edited.gender as NonNullable<
      AdminEditUserInfoRequest["gender"]
    >;
  return payload;
}

export default function UserEditModal({ userId, onClose }: UserEditModalProps) {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const { data: response, isLoading } = useGetUserDetail(userId);
  const detail = response?.status === 200 ? response.data : undefined;

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserEditFormData>({
    resolver: zodResolver(userEditSchema),
    mode: "onTouched",
  });

  useEffect(() => {
    if (detail) {
      reset({
        studentId: detail.studentId ?? "",
        email: detail.email ?? "",
        name: detail.name ?? "",
        phoneNumber: detail.phoneNumber ?? "",
        department: detail.department ?? "",
        grade: detail.grade ?? 1,
        enrollmentStatus:
          (detail.enrollmentStatus as UserEditFormData["enrollmentStatus"]) ??
          "ENROLLED",
        gender: (detail.gender as UserEditFormData["gender"]) ?? "MALE",
      });
    }
  }, [detail, reset]);

  const { mutate: editUserInfo, isPending } = useEditUserInfo({
    mutation: {
      onSuccess: () => {
        addToast({
          type: "success",
          title: "정보 수정 완료",
          message: "회원 정보가 수정되었습니다.",
        });
        queryClient.invalidateQueries({ queryKey: ["/api/v1/admin/users"] });
        onClose();
      },
      onError: (error: unknown) => {
        if (hasErrorCode(error, "DUPLICATE_STUDENT_ID")) {
          setError("studentId", { message: "이미 가입된 학번입니다." });
        } else if (hasErrorCode(error, "DUPLICATE_EMAIL")) {
          setError("email", { message: "이미 사용 중인 이메일입니다." });
        } else if (hasErrorCode(error, "DUPLICATE_PHONE_NUMBER")) {
          setError("phoneNumber", {
            message: "이미 사용 중인 전화번호입니다.",
          });
        } else {
          addToast({
            type: "error",
            title: "정보 수정 실패",
            message: getErrorMessage(error),
          });
        }
      },
    },
  });

  const selectedGrade = watch("grade");
  const selectedEnrollment = watch("enrollmentStatus");
  const selectedGender = watch("gender");

  const onSubmit = async (data: UserEditFormData) => {
    if (!detail) return;

    const payload = buildPatchPayload(detail, data);

    if (Object.keys(payload).length === 0) {
      addToast({ type: "default", message: "변경된 항목이 없습니다." });
      return;
    }

    const result = await Swal.fire({
      icon: "question",
      title: "회원 정보 수정",
      text: `${detail.name}님의 정보를 수정하시겠습니까?`,
      showCancelButton: true,
      confirmButtonText: "수정",
      cancelButtonText: "취소",
      confirmButtonColor: "#2563EB",
      cancelButtonColor: "#6C757D",
      showClass: { popup: "", backdrop: "" },
      hideClass: { popup: "", backdrop: "" },
    });

    if (result.isConfirmed) {
      editUserInfo({ userId, data: payload });
    }
  };

  const renderContent = () => {
    if (isLoading) {
      return (
        <div className="flex items-center justify-center py-12 text-muted-foreground">
          로딩 중...
        </div>
      );
    }

    if (!detail) {
      return (
        <div className="flex items-center justify-center py-12 text-muted-foreground">
          사용자 정보를 불러올 수 없습니다.
        </div>
      );
    }

    return (
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-s4">
        {/* 학번 */}
        <FormField label="학번" error={errors.studentId?.message}>
          <Input {...register("studentId")} placeholder="20231234" />
        </FormField>

        {/* 이름 */}
        <FormField label="이름" error={errors.name?.message}>
          <Input {...register("name")} placeholder="홍길동" />
        </FormField>

        {/* 이메일 */}
        <FormField label="이메일" error={errors.email?.message}>
          <Input
            {...register("email")}
            type="email"
            placeholder="user@inha.edu"
          />
        </FormField>

        {/* 전화번호 */}
        <FormField label="전화번호" error={errors.phoneNumber?.message}>
          <Input
            {...register("phoneNumber", {
              onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                setValue("phoneNumber", formatPhoneNumber(e.target.value));
              },
            })}
            placeholder="010-1234-5678"
          />
        </FormField>

        {/* 학과 */}
        <FormField label="학과" error={errors.department?.message}>
          <select
            {...register("department")}
            className="w-full h-9 px-s3 py-s1 rounded-r2 border border-input bg-background text-sm"
          >
            <option value="">학과 선택</option>
            {majorOptions.map((group) => (
              <optgroup key={group.title} label={group.title}>
                {group.items.map((item) => (
                  <option key={item.key} value={item.value}>
                    {item.value}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        </FormField>

        {/* 학년 */}
        <FormField label="학년" error={errors.grade?.message}>
          <div className="flex gap-s2">
            {GRADE_OPTIONS.map((g) => (
              <button
                key={g}
                type="button"
                onClick={() => setValue("grade", g, { shouldValidate: true })}
                className={cn(
                  "flex-1 py-s2 rounded-r2 text-sm font-medium border transition-colors cursor-pointer",
                  selectedGrade === g
                    ? "bg-primary text-primary-foreground border-primary"
                    : "bg-background text-foreground border-input hover:bg-muted",
                )}
              >
                {g}학년
              </button>
            ))}
          </div>
        </FormField>

        {/* 성별 */}
        <FormField label="성별" error={errors.gender?.message}>
          <div className="flex gap-s2">
            {GENDER_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() =>
                  setValue("gender", opt.value, { shouldValidate: true })
                }
                className={cn(
                  "flex-1 py-s2 rounded-r2 text-sm font-medium border transition-colors cursor-pointer",
                  selectedGender === opt.value
                    ? "bg-primary text-primary-foreground border-primary"
                    : "bg-background text-foreground border-input hover:bg-muted",
                )}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </FormField>

        {/* 재학 상태 */}
        <FormField label="재학 상태" error={errors.enrollmentStatus?.message}>
          <div className="flex gap-s2">
            {ENROLLMENT_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() =>
                  setValue("enrollmentStatus", opt.value, {
                    shouldValidate: true,
                  })
                }
                className={cn(
                  "flex-1 py-s2 rounded-r2 text-sm font-medium border transition-colors cursor-pointer",
                  selectedEnrollment === opt.value
                    ? "bg-primary text-primary-foreground border-primary"
                    : "bg-background text-foreground border-input hover:bg-muted",
                )}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </FormField>

        {/* 버튼 */}
        <div className="flex justify-end gap-s3 pt-s4">
          <Button type="button" variant="outline" onClick={onClose}>
            취소
          </Button>
          <Button type="submit" disabled={isSubmitting || isPending}>
            {isPending ? "수정 중..." : "수정"}
          </Button>
        </div>
      </form>
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative z-10 w-full max-w-lg rounded-r3 bg-background p-s6 shadow-xl border border-border max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-s5">
          <h2 className="typo-h3 font-bold">회원 정보 수정</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-muted-foreground hover:text-foreground cursor-pointer"
          >
            <X size={20} />
          </button>
        </div>
        {renderContent()}
      </div>
    </div>
  );
}

function FormField({
  label,
  error,
  children,
}: {
  label: string;
  error: string | undefined;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="block typo-b2 font-medium mb-s1">{label}</label>
      {children}
      {error && <p className="text-destructive text-xs mt-s1">{error}</p>}
    </div>
  );
}
