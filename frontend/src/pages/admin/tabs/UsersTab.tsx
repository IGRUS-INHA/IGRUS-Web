import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { keepPreviousData, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import Swal from "sweetalert2";
import {
  Search,
  User,
  UserCheck,
  MessageSquare,
  ChevronRight,
  FileText,
  CheckCircle,
  BookOpen,
  Pencil,
  LogOut,
  Save,
  Users,
  X,
} from "lucide-react";
import {
  useGetUserList,
  useGetUserDetail,
  useEditUserInfo,
  useChangeUserRole,
} from "@/api/model/admin-user-management/admin-user-management";
import type { ChangeUserRoleRequestRole } from "@/api/model/models/changeUserRoleRequestRole";
import { useGetPendingAssociates } from "@/api/model/admin-associate-approval/admin-associate-approval";
import { useGetAllInquiries } from "@/api/model/admin-inquiry/admin-inquiry";
import type { GetUserListRole } from "@/api/model/models/getUserListRole";
import { Button } from "@/components/ui/button";
import { majorOptions } from "@/constants/majorOptions";
import { useAuth, usePermission } from "@/hooks";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/stores";
import { formatPhoneNumber } from "@/utils";
import { getErrorMessage, hasErrorCode } from "@/utils/error";
import {
  userEditSchema,
  buildPatchPayload,
  type UserEditFormData,
} from "./userEditSchema";
import styles from "./UsersTab.module.css";

/* ===== Constants ===== */

const ROLE_OPTIONS: { value: GetUserListRole | ""; label: string }[] = [
  { value: "", label: "전체 역할" },
  { value: "ADMIN", label: "관리자" },
  { value: "OPERATOR", label: "운영진" },
  { value: "MEMBER", label: "정회원" },
  { value: "ASSOCIATE", label: "준회원" },
];

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "관리자",
  OPERATOR: "운영진",
  MEMBER: "정회원",
  ASSOCIATE: "준회원",
};

const ROLE_STYLE: Record<string, string> = {
  ADMIN: styles.roleAdmin,
  OPERATOR: styles.roleOperator,
  MEMBER: styles.roleRegular,
  ASSOCIATE: styles.roleAssociate,
};

const STATUS_STYLE: Record<string, string> = {
  ACTIVE: styles.statusActive,
  SUSPENDED: styles.statusSuspended,
  WITHDRAWN: styles.statusWithdrawn,
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "활동",
  SUSPENDED: "정지",
  WITHDRAWN: "탈퇴",
};

const ENROLLMENT_LABELS: Record<string, string> = {
  ENROLLED: "재학",
  GENERAL_LEAVE: "휴학 (일반)",
  MILITARY_LEAVE: "휴학 (군)",
};

const GENDER_LABELS: Record<string, string> = {
  MALE: "남",
  FEMALE: "여",
};

const INTERESTS_LABELS: Record<string, string> = {
  WEB: "웹 개발",
  APP: "앱 개발",
  AI_ML: "AI/ML",
  DATA_SCIENCE: "데이터 사이언스",
  SECURITY: "보안",
  GAME: "게임 개발",
  EMBEDDED: "임베디드",
  DEVOPS: "DevOps",
  BLOCKCHAIN: "블록체인",
  OTHER: "기타",
};

const WISHES_LABELS: Record<string, string> = {
  NETWORKING: "네트워킹",
  STUDY: "스터디",
  PROJECT: "프로젝트",
  MENTORING: "멘토링",
  CAREER: "취업/진로",
  COMPETITION: "대회/공모전",
  OTHER: "기타",
};

const JOIN_ROUTE_LABELS: Record<string, string> = {
  FRIEND: "친구 추천",
  SNS: "SNS",
  POSTER: "포스터/현수막",
  SEARCH: "검색",
  DEPARTMENT: "학과 안내",
  OTHER: "기타",
};

const MOCK_ACTIVITIES = [
  {
    type: "post" as const,
    text: '게시글 작성 — "스프링부트 스터디 모집"',
    date: "2026-03-07",
  },
  {
    type: "attend" as const,
    text: "출석 체크 — 정기 모임",
    date: "2026-03-05",
  },
  {
    type: "study" as const,
    text: "스터디 참여 — 알고리즘 스터디",
    date: "2026-03-03",
  },
];

const TIMELINE_DOT_STYLE: Record<string, string> = {
  post: styles.timelineDotPost,
  attend: styles.timelineDotAttend,
  study: styles.timelineDotStudy,
};

const TIMELINE_ICONS = {
  post: <FileText size={12} />,
  attend: <CheckCircle size={12} />,
  study: <BookOpen size={12} />,
};

const PAGE_SIZE = 20;

/* ===== Sub-components ===== */

function BentoChartVisitors() {
  return (
    <div className={cn(styles.bentoCell, styles.bentoCellChart1)}>
      <div className={styles.bentoChartContent}>
        <div className={styles.bentoChartHeader}>
          <div>
            <div className={styles.bentoChartLabel}>Today&apos;s Visitors</div>
            <div className={styles.bentoChartTitle}>오늘 하루 접속자</div>
          </div>
          <div className={styles.bentoChartBadge}>
            <ChevronRight size={12} />
            목업 데이터
          </div>
        </div>
        <svg
          className={styles.bentoAreaChart}
          viewBox="0 0 480 160"
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient id="bentoAreaFill1" x1="0" y1="0" x2="0" y2="1">
              <stop
                offset="0%"
                stopColor="var(--color-primary)"
                stopOpacity="0.25"
              />
              <stop
                offset="100%"
                stopColor="var(--color-primary)"
                stopOpacity="0.02"
              />
            </linearGradient>
          </defs>
          <line
            x1="0"
            y1="40"
            x2="480"
            y2="40"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <line
            x1="0"
            y1="80"
            x2="480"
            y2="80"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <line
            x1="0"
            y1="120"
            x2="480"
            y2="120"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <path
            d="M0,150 C20,150 40,150 60,150 C80,150 100,150 120,139 C140,117 160,95 180,103 C200,100 220,20 240,38 C260,50 280,5 300,33 C320,47 340,5 360,30 C380,83 400,120 420,145 C440,150 460,150 480,150 L480,160 L0,160 Z"
            fill="url(#bentoAreaFill1)"
          />
          <path
            d="M0,150 C20,150 40,150 60,150 C80,150 100,150 120,139 C140,117 160,95 180,103 C200,100 220,20 240,38 C260,50 280,5 300,33 C320,47 340,5 360,30 C380,83 400,120 420,145 C440,150 460,150 480,150"
            fill="none"
            stroke="var(--color-primary)"
            strokeWidth="2.5"
            strokeLinecap="round"
          />
          <circle
            cx="340"
            cy="5"
            r="4"
            fill="var(--color-primary)"
            stroke="var(--color-card)"
            strokeWidth="2"
          />
          <text
            x="0"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            0시
          </text>
          <text
            x="120"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            6시
          </text>
          <text
            x="240"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            12시
          </text>
          <text
            x="360"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            18시
          </text>
          <text
            x="465"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
            textAnchor="end"
          >
            23시
          </text>
        </svg>
      </div>
    </div>
  );
}

function BentoChartRegistrations() {
  return (
    <div className={cn(styles.bentoCell, styles.bentoCellChart2)}>
      <div className={styles.bentoChartContent}>
        <div className={styles.bentoChartHeader}>
          <div>
            <div className={styles.bentoChartLabel}>Registrations</div>
            <div className={styles.bentoChartTitle}>가입자 추이</div>
          </div>
          <div
            className={cn(styles.bentoChartBadge, styles.bentoChartBadgeInfo)}
          >
            <ChevronRight size={12} />
            목업 데이터
          </div>
        </div>
        <svg
          className={styles.bentoAreaChart}
          viewBox="0 0 480 160"
          preserveAspectRatio="none"
        >
          <defs>
            <linearGradient id="bentoAreaFill2" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#17A2B8" stopOpacity="0.25" />
              <stop offset="100%" stopColor="#17A2B8" stopOpacity="0.02" />
            </linearGradient>
          </defs>
          <line
            x1="0"
            y1="40"
            x2="480"
            y2="40"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <line
            x1="0"
            y1="80"
            x2="480"
            y2="80"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <line
            x1="0"
            y1="120"
            x2="480"
            y2="120"
            stroke="var(--color-border)"
            strokeWidth="1"
            opacity="0.4"
          />
          <path
            d="M0,150 C34,85 69,150 103,20 C137,85 171,150 206,85 C240,150 274,85 309,20 C343,150 377,150 411,150 C446,150 480,150 480,150 L480,160 L0,160 Z"
            fill="url(#bentoAreaFill2)"
          />
          <path
            d="M0,150 C34,85 69,150 103,20 C137,85 171,150 206,85 C240,150 274,85 309,20 C343,150 377,150 411,150 C446,150 480,150 480,150"
            fill="none"
            stroke="#17A2B8"
            strokeWidth="2.5"
            strokeLinecap="round"
          />
          <circle
            cx="103"
            cy="20"
            r="4"
            fill="#17A2B8"
            stroke="var(--color-card)"
            strokeWidth="2"
          />
          <circle
            cx="309"
            cy="20"
            r="4"
            fill="#17A2B8"
            stroke="var(--color-card)"
            strokeWidth="2"
          />
          <text
            x="0"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            2/23
          </text>
          <text
            x="160"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            2/28
          </text>
          <text
            x="320"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
          >
            3/4
          </text>
          <text
            x="465"
            y="155"
            fill="var(--color-muted-foreground)"
            fontSize="10"
            fontWeight="500"
            textAnchor="end"
          >
            3/8
          </text>
        </svg>
      </div>
    </div>
  );
}

function NavButtonCell({
  area,
  icon,
  label,
  title,
  count,
  onClick,
  disabled,
}: {
  area: string;
  icon: React.ReactNode;
  label: string;
  title: string;
  count: number;
  onClick: () => void;
  disabled?: boolean;
}) {
  const areaClass =
    area === "stat1" ? styles.bentoCellStat1 : styles.bentoCellStat2;
  const iconBg =
    area === "stat1"
      ? "bg-[rgba(255,193,7,0.1)] text-[#d39e00]"
      : "bg-[rgba(220,53,69,0.1)] text-[#DC3545]";

  return (
    <div
      className={cn(
        styles.bentoCell,
        areaClass,
        disabled && styles.bentoCellDisabled,
      )}
    >
      <button
        type="button"
        className={cn(styles.navButton, !disabled && "cursor-pointer")}
        onClick={disabled ? undefined : onClick}
        disabled={disabled}
      >
        <div className={cn(styles.navButtonIcon, iconBg)}>{icon}</div>
        <div className={styles.navButtonLabel}>{label}</div>
        <div className={styles.navButtonTitle}>{count}</div>
        <div className={styles.navButtonCount}>{title}</div>
        {!disabled && (
          <div className={styles.navButtonArrow}>
            바로가기 <ChevronRight size={14} />
          </div>
        )}
      </button>
    </div>
  );
}

const EDIT_ENROLLMENT_OPTIONS = [
  { value: "ENROLLED", label: "재학" },
  { value: "GENERAL_LEAVE", label: "휴학(일반)" },
  { value: "MILITARY_LEAVE", label: "휴학(군)" },
] as const;

const EDIT_GENDER_OPTIONS = [
  { value: "MALE", label: "남" },
  { value: "FEMALE", label: "여" },
] as const;

const EDIT_ROLE_OPTIONS = [
  { value: "ASSOCIATE", label: "준회원" },
  { value: "MEMBER", label: "정회원" },
  { value: "OPERATOR", label: "운영진" },
  { value: "ADMIN", label: "관리자" },
] as const;

const EDIT_GRADE_OPTIONS = [1, 2, 3, 4] as const;

function DetailPanel({
  userId,
  isAdmin,
}: {
  userId: number;
  isAdmin: boolean;
}) {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const { data: response, isLoading } = useGetUserDetail(userId);
  const detail = response?.status === 200 ? response.data : undefined;

  const [isEditing, setIsEditing] = useState(false);
  const [selectedRole, setSelectedRole] = useState<
    ChangeUserRoleRequestRole | undefined
  >(undefined);

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

  // Sync form & role when detail loads
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
      setSelectedRole(detail.role as ChangeUserRoleRequestRole);
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
        setIsEditing(false);
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

  const { mutate: changeUserRole, isPending: isRoleChangePending } =
    useChangeUserRole({
      mutation: {
        onSuccess: () => {
          addToast({
            type: "success",
            title: "권한 변경 완료",
            message: "회원 권한이 변경되었습니다.",
          });
          queryClient.invalidateQueries({ queryKey: ["/api/v1/admin/users"] });
          setIsEditing(false);
        },
        onError: (error: unknown) => {
          addToast({
            type: "error",
            title: "권한 변경 실패",
            message: getErrorMessage(error),
          });
        },
      },
    });

  const selectedGrade = watch("grade");
  const selectedEnrollment = watch("enrollmentStatus");
  const selectedGender = watch("gender");

  const onSubmit = async (data: UserEditFormData) => {
    if (!detail) return;

    const payload = buildPatchPayload(detail, data);
    const hasInfoChanges = Object.keys(payload).length > 0;
    const hasRoleChange =
      selectedRole !== undefined && selectedRole !== detail.role;

    if (!hasInfoChanges && !hasRoleChange) {
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
      if (hasInfoChanges) {
        editUserInfo({ userId, data: payload });
      }
      if (hasRoleChange) {
        changeUserRole({ userId, data: { role: selectedRole } });
      }
    }
  };

  const handleCancel = () => {
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
      setSelectedRole(detail.role as ChangeUserRoleRequestRole);
    }
    setIsEditing(false);
  };

  if (isLoading) {
    return (
      <div className={styles.detailPanel}>
        <div className={styles.detailEmpty}>
          <div className={styles.detailEmptyText}>로딩 중...</div>
        </div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className={styles.detailPanel}>
        <div className={styles.detailEmpty}>
          <div className={styles.detailEmptyText}>
            사용자 정보를 불러올 수 없습니다.
          </div>
        </div>
      </div>
    );
  }

  const roleLabel = ROLE_LABELS[detail.role ?? ""] ?? detail.role ?? "-";
  const isSaving = isSubmitting || isPending || isRoleChangePending;

  return (
    <div className={styles.detailPanel} key={userId}>
      {/* Top gradient section */}
      <div className={styles.detailTop}>
        <div className={styles.detailAvatar}>
          <User size={28} />
        </div>
        <div className={styles.detailName}>{detail.name}</div>
        <div className={styles.detailRoleBadge}>{roleLabel}</div>
      </div>

      {/* Body */}
      <div className={styles.detailBody}>
        <div className={styles.detailSectionLabel}>Information</div>
        <form
          id={`edit-form-${userId}`}
          onSubmit={handleSubmit(onSubmit)}
          className={cn(styles.infoGrid, isEditing && styles.infoGridEditing)}
        >
          {/* Edit-only: 이름 */}
          {isEditing && (
            <EditableInfoItem
              label="이름"
              viewValue={detail.name ?? "-"}
              editContent={<input {...register("name")} placeholder="홍길동" />}
              error={errors.name?.message}
              isEditing={isEditing}
            />
          )}

          {/* Edit-only: 권한 */}
          {isEditing && (
            <EditableInfoItem
              label="권한"
              viewValue={roleLabel}
              editContent={
                <div className={styles.editBtnGroup}>
                  {EDIT_ROLE_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() =>
                        setSelectedRole(opt.value as ChangeUserRoleRequestRole)
                      }
                      className={cn(
                        selectedRole === opt.value && styles.editBtnGroupActive,
                      )}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              }
              isEditing={isEditing}
            />
          )}

          {/* 학번 */}
          <EditableInfoItem
            label="학번"
            viewValue={detail.studentId ?? "-"}
            editContent={
              <input {...register("studentId")} placeholder="20231234" />
            }
            error={errors.studentId?.message}
            isEditing={isEditing}
          />

          {/* 학과 */}
          <EditableInfoItem
            label="학과"
            viewValue={detail.department ?? "-"}
            editContent={
              <select {...register("department")}>
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
            }
            error={errors.department?.message}
            isEditing={isEditing}
          />

          {/* 이메일 */}
          <EditableInfoItem
            label="이메일"
            viewValue={detail.email ?? "-"}
            editContent={
              <input
                {...register("email")}
                type="email"
                placeholder="user@inha.edu"
              />
            }
            error={errors.email?.message}
            isEditing={isEditing}
            fullWidth
          />

          {/* 전화번호 */}
          <EditableInfoItem
            label="전화번호"
            viewValue={detail.phoneNumber ?? "-"}
            editContent={
              <input
                {...register("phoneNumber", {
                  onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                    setValue("phoneNumber", formatPhoneNumber(e.target.value));
                  },
                })}
                placeholder="010-1234-5678"
              />
            }
            error={errors.phoneNumber?.message}
            isEditing={isEditing}
          />

          {/* 학년 */}
          <EditableInfoItem
            label="학년"
            viewValue={detail.grade ? `${detail.grade}학년` : "-"}
            editContent={
              <div className={styles.editBtnGroup}>
                {EDIT_GRADE_OPTIONS.map((g) => (
                  <button
                    key={g}
                    type="button"
                    onClick={() =>
                      setValue("grade", g, { shouldValidate: true })
                    }
                    className={cn(
                      selectedGrade === g && styles.editBtnGroupActive,
                    )}
                  >
                    {g}학년
                  </button>
                ))}
              </div>
            }
            error={errors.grade?.message}
            isEditing={isEditing}
          />

          {/* 성별 */}
          <EditableInfoItem
            label="성별"
            viewValue={GENDER_LABELS[detail.gender ?? ""] ?? "-"}
            editContent={
              <div className={styles.editBtnGroup}>
                {EDIT_GENDER_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() =>
                      setValue("gender", opt.value, { shouldValidate: true })
                    }
                    className={cn(
                      selectedGender === opt.value && styles.editBtnGroupActive,
                    )}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            }
            error={errors.gender?.message}
            isEditing={isEditing}
          />

          {/* 재학 상태 */}
          <EditableInfoItem
            label="재학 상태"
            viewValue={ENROLLMENT_LABELS[detail.enrollmentStatus ?? ""] ?? "-"}
            editContent={
              <div className={styles.editBtnGroup}>
                {EDIT_ENROLLMENT_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() =>
                      setValue("enrollmentStatus", opt.value, {
                        shouldValidate: true,
                      })
                    }
                    className={cn(
                      selectedEnrollment === opt.value &&
                        styles.editBtnGroupActive,
                    )}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            }
            error={errors.enrollmentStatus?.message}
            isEditing={isEditing}
          />

          {/* 읽기 전용 필드 */}
          <InfoItem
            label="가입일"
            value={
              detail.createdAt
                ? new Date(detail.createdAt).toLocaleDateString("ko-KR")
                : "-"
            }
          />
          <InfoItem
            label="상태"
            value={STATUS_LABELS[detail.status ?? ""] ?? "-"}
          />
          <InfoItem
            label="가입 동기"
            value={detail.motivation ?? "-"}
            fullWidth
          />
          <InfoItem
            label="관심 분야"
            value={
              detail.interests?.length
                ? detail.interests
                    .map((i) => INTERESTS_LABELS[i] ?? i)
                    .join(", ") +
                  (detail.customInterest ? `, ${detail.customInterest}` : "")
                : "-"
            }
            fullWidth
          />
          <InfoItem
            label="가입 목적"
            value={
              detail.wishes?.length
                ? detail.wishes.map((w) => WISHES_LABELS[w] ?? w).join(", ")
                : "-"
            }
            fullWidth
          />
          <InfoItem
            label="가입 경로"
            value={
              detail.joinRoute
                ? (JOIN_ROUTE_LABELS[detail.joinRoute] ?? detail.joinRoute) +
                  (detail.customJoinRoute ? ` (${detail.customJoinRoute})` : "")
                : "-"
            }
            fullWidth
          />
        </form>

        {/* Recent Activity (Mock) */}
        <div className={styles.detailSectionLabel}>Recent Activity</div>
        <div className={styles.timeline}>
          {MOCK_ACTIVITIES.map((activity, i) => (
            <div className={styles.timelineItem} key={i}>
              <div
                className={cn(
                  styles.timelineDot,
                  TIMELINE_DOT_STYLE[activity.type],
                )}
              >
                {TIMELINE_ICONS[activity.type]}
              </div>
              <div className={styles.timelineContent}>
                <div className={styles.timelineText}>{activity.text}</div>
                <div className={styles.timelineDate}>{activity.date}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      {isAdmin && (
        <div className={styles.detailActions}>
          {isEditing ? (
            <>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCancel}
                disabled={isSaving}
              >
                <X size={14} />
                취소
              </Button>
              <Button
                type="submit"
                form={`edit-form-${userId}`}
                size="sm"
                disabled={isSaving}
              >
                <Save size={14} />
                {isSaving ? "저장 중..." : "저장"}
              </Button>
            </>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setIsEditing(true)}
              >
                <Pencil size={14} />
                정보수정
              </Button>
              <Button type="button" variant="destructive" size="sm">
                <LogOut size={14} />
                강제 탈퇴
              </Button>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function EditableInfoItem({
  label,
  viewValue,
  editContent,
  error,
  isEditing,
  fullWidth,
}: {
  label: string;
  viewValue: string;
  editContent: React.ReactNode;
  error?: string;
  isEditing: boolean;
  fullWidth?: boolean;
}) {
  return (
    <div className={cn(styles.infoItem, fullWidth && styles.infoItemFullWidth)}>
      <div className={styles.infoLabel}>{label}</div>
      {isEditing ? (
        <div>
          {editContent}
          {error && <p className="text-destructive text-xs mt-s1">{error}</p>}
        </div>
      ) : (
        <div className={styles.infoValue}>{viewValue}</div>
      )}
    </div>
  );
}

function InfoItem({
  label,
  value,
  fullWidth,
}: {
  label: string;
  value: string;
  fullWidth?: boolean;
}) {
  return (
    <div className={cn(styles.infoItem, fullWidth && styles.infoItemFullWidth)}>
      <div className={styles.infoLabel}>{label}</div>
      <div className={styles.infoValue}>{value}</div>
    </div>
  );
}

function EmptyDetailPanel() {
  return (
    <div className={styles.detailPanel} style={{ animation: "none" }}>
      <div className={styles.detailEmpty}>
        <div className={styles.detailEmptyIcon}>
          <Users size={48} />
        </div>
        <div className={styles.detailEmptyText}>
          좌측 목록에서 회원을 선택하면
          <br />
          상세 정보가 표시됩니다.
        </div>
      </div>
    </div>
  );
}

/* ===== Main Component ===== */

export default function UsersTab() {
  const [, setSearchParams] = useSearchParams();
  const { user: currentUser } = useAuth();
  const { canAccessAdmin } = usePermission();

  // 승인 대기 준회원 수 (size=1로 최소 데이터만, totalElements만 사용)
  const { data: pendingAssociatesRes } = useGetPendingAssociates({
    page: 0,
    size: 1,
  });
  const pendingAssociateCount =
    pendingAssociatesRes?.status === 200
      ? (pendingAssociatesRes.data?.totalElements ?? 0)
      : 0;

  // 대기 중 문의 수 (status=PENDING, size=1)
  const { data: pendingInquiriesRes } = useGetAllInquiries({
    status: "PENDING",
    page: 0,
    size: 1,
  });
  const pendingInquiryCount =
    pendingInquiriesRes?.status === 200
      ? (pendingInquiriesRes.data?.totalElements ?? 0)
      : 0;
  const [keyword, setKeyword] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [roleFilter, setRoleFilter] = useState<GetUserListRole | "">("");
  const [page, setPage] = useState(1);
  const [selectedUserId, setSelectedUserId] = useState<number | undefined>(
    undefined,
  );
  const [isCompact, setIsCompact] = useState(() => window.innerWidth < 1200);

  useEffect(() => {
    const mql = window.matchMedia("(max-width: 1199px)");
    const handler = (e: MediaQueryListEvent) => setIsCompact(e.matches);
    mql.addEventListener("change", handler);
    return () => mql.removeEventListener("change", handler);
  }, []);

  // Lock body scroll when compact detail modal is open
  useEffect(() => {
    if (isCompact && selectedUserId !== undefined) {
      document.body.style.overflow = "hidden";
      return () => {
        document.body.style.overflow = "";
      };
    }
  }, [isCompact, selectedUserId]);

  const {
    data: response,
    isLoading,
    isFetching,
  } = useGetUserList(
    {
      ...(searchKeyword && { keyword: searchKeyword }),
      ...(roleFilter && { role: roleFilter }),
      page: page - 1,
      size: PAGE_SIZE,
    },
    { query: { placeholderData: keepPreviousData } },
  );

  const data = response?.status === 200 ? response.data : undefined;
  const users = data?.users ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;
  const isAdmin = currentUser?.role === "ADMIN";

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchKeyword(keyword);
    setPage(1);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  // Pagination range
  const maxVisiblePages = 5;
  const startPage = Math.max(
    1,
    Math.min(
      page - Math.floor(maxVisiblePages / 2),
      totalPages - maxVisiblePages + 1,
    ),
  );
  const endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
  const visiblePages = Array.from(
    { length: endPage - startPage + 1 },
    (_, i) => startPage + i,
  );

  return (
    <div className="space-y-s5">
      {/* ===== Bento Grid ===== */}
      <div className={styles.bentoGrid}>
        <BentoChartVisitors />
        <NavButtonCell
          area="stat1"
          icon={<UserCheck size={24} />}
          label="Associate Approval"
          title="준회원 승인"
          count={pendingAssociateCount}
          onClick={() => setSearchParams({ tab: "associates" })}
          disabled={!canAccessAdmin()}
        />
        <BentoChartRegistrations />
        <NavButtonCell
          area="stat2"
          icon={<MessageSquare size={24} />}
          label="Inquiry Management"
          title="문의 관리"
          count={pendingInquiryCount}
          onClick={() => setSearchParams({ tab: "inquiries" })}
        />
      </div>

      {/* ===== Master-Detail ===== */}
      <div className={styles.masterDetail}>
        {/* Left: Table Panel */}
        <div className={styles.tablePanel}>
          {/* Toolbar */}
          <div className={styles.tableToolbar}>
            <form onSubmit={handleSearch} className={styles.searchBox}>
              <Search
                size={16}
                className="text-muted-foreground flex-shrink-0"
              />
              <input
                type="text"
                placeholder="이름, 학번으로 검색..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </form>
            <span className="text-sm font-semibold text-muted-foreground whitespace-nowrap">
              전체 {totalElements.toLocaleString()}명
            </span>
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value as GetUserListRole | "");
                setPage(1);
              }}
              className="px-s3 py-s2 rounded-r2 border border-border bg-card text-sm font-medium cursor-pointer"
            >
              {ROLE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>

          {/* Table */}
          <div
            style={{
              overflowX: "auto",
              flex: 1,
              opacity: isFetching ? 0.6 : 1,
              transition: "opacity 0.15s ease",
            }}
          >
            <table className={styles.memberTable}>
              <thead>
                <tr>
                  <th>No.</th>
                  <th>회원</th>
                  <th>학번</th>
                  <th>역할</th>
                  <th>상태</th>
                  <th>전화번호</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u, index) => {
                  const isSelected = selectedUserId === u.userId;
                  return (
                    <tr
                      key={u.userId}
                      className={isSelected ? "selected" : ""}
                      onClick={() => setSelectedUserId(u.userId)}
                    >
                      <td>
                        <span className="text-sm text-muted-foreground">
                          {(page - 1) * PAGE_SIZE + index + 1}
                        </span>
                      </td>
                      <td>
                        <div className={styles.memberCell}>
                          <div className={styles.memberNameCol}>
                            <span className={styles.memberNameText}>
                              {u.name}
                            </span>
                            <span className={styles.memberDeptText}>
                              {u.department ?? "-"}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className="text-sm font-medium">
                          {u.studentId}
                        </span>
                      </td>
                      <td>
                        <span
                          className={cn(
                            styles.roleBadge,
                            ROLE_STYLE[u.role ?? ""],
                          )}
                        >
                          {ROLE_LABELS[u.role ?? ""] ?? u.role}
                        </span>
                      </td>
                      <td>
                        <span
                          className={cn(
                            styles.statusBadge,
                            STATUS_STYLE[u.status ?? ""],
                          )}
                        >
                          {STATUS_LABELS[u.status ?? ""] ?? u.status}
                        </span>
                      </td>
                      <td>
                        <span className="text-sm">{u.phoneNumber ?? "-"}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            {users.length === 0 && (
              <div className="text-center py-12 text-muted-foreground">
                회원이 없습니다.
              </div>
            )}
          </div>

          {/* Footer */}
          <div className={styles.tableFooter}>
            <span>
              {totalElements.toLocaleString()}명 중 {(page - 1) * PAGE_SIZE + 1}
              -{Math.min(page * PAGE_SIZE, totalElements)} 표시
            </span>
            <div className={styles.tablePagination}>
              <button
                type="button"
                className={cn(styles.pageBtn, "cursor-pointer")}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                disabled={page <= 1}
              >
                &lsaquo;
              </button>
              {visiblePages.map((p) => (
                <button
                  key={p}
                  type="button"
                  className={cn(
                    styles.pageBtn,
                    "cursor-pointer",
                    p === page && styles.pageBtnActive,
                  )}
                  onClick={() => setPage(p)}
                >
                  {p}
                </button>
              ))}
              <button
                type="button"
                className={cn(styles.pageBtn, "cursor-pointer")}
                onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                disabled={page >= totalPages}
              >
                &rsaquo;
              </button>
            </div>
          </div>
        </div>

        {/* Right: Detail Panel (desktop only) */}
        {!isCompact &&
          (selectedUserId ? (
            <DetailPanel userId={selectedUserId} isAdmin={isAdmin} />
          ) : (
            <EmptyDetailPanel />
          ))}
      </div>

      {/* Detail Modal (compact screens) */}
      {isCompact && selectedUserId !== undefined && (
        <div
          className={styles.detailModal}
          onClick={() => setSelectedUserId(undefined)}
        >
          <div
            className={styles.detailModalContent}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className={cn(styles.detailModalClose, "cursor-pointer")}
              onClick={() => setSelectedUserId(undefined)}
            >
              <X size={20} />
            </button>
            <DetailPanel userId={selectedUserId} isAdmin={isAdmin} />
          </div>
        </div>
      )}
    </div>
  );
}
