import { useState } from "react";
import { useUIStore } from "@/stores";
import {
  User,
  Mail,
  Calendar,
  Edit3,
  Shield,
  Lock,
  LogOut,
  UserX,
  Building2,
  Phone,
  Pencil,
  Check,
  X,
  Loader2,
} from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ROLE_LABELS } from "@/constants";
import { formatPhoneNumber } from "@/utils";
import { cn } from "@/lib/utils";
import { customFetch } from "@/api/client";
import { getErrorMessage, hasErrorCode } from "@/utils/error";
import type { User as UserType } from "@/types/entities";
import type { MyProfileResponse } from "@/api/model/models/myProfileResponse";
import type { UpdateProfileRequest } from "@/api/model/models/updateProfileRequest";

interface ProfileHeaderProps {
  user: UserType;
  profile?: MyProfileResponse | undefined;
  onChangePassword?: () => void;
  onLogout?: () => void;
  onWithdraw?: () => void;
  onUpdateProfile?: (data: UpdateProfileRequest) => Promise<void>;
  onStudentIdUpdated?: () => void;
  isUpdating?: boolean;
}

type EditingField = "email" | "phone" | undefined;

export default function ProfileHeader({
  user,
  profile,
  onChangePassword,
  onLogout,
  onWithdraw,
  onUpdateProfile,
  onStudentIdUpdated,
  isUpdating,
}: ProfileHeaderProps) {
  const { theme } = useUIStore();
  const isDark = theme === "dark";
  const [editingField, setEditingField] = useState<EditingField>();
  const [editValue, setEditValue] = useState("");

  // 학번 변경 상태
  const [isEditingStudentId, setIsEditingStudentId] = useState(false);
  const [newStudentId, setNewStudentId] = useState("");
  const [studentIdPassword, setStudentIdPassword] = useState("");
  const [studentIdError, setStudentIdError] = useState("");
  const [isUpdatingStudentId, setIsUpdatingStudentId] = useState(false);

  const hasTemporaryStudentId = profile?.hasTemporaryStudentId === true;

  const startEditingStudentId = () => {
    setNewStudentId("");
    setStudentIdPassword("");
    setStudentIdError("");
    setIsEditingStudentId(true);
  };

  const cancelEditingStudentId = () => {
    setIsEditingStudentId(false);
    setNewStudentId("");
    setStudentIdPassword("");
    setStudentIdError("");
  };

  const handleSaveStudentId = async () => {
    setStudentIdError("");

    if (!newStudentId.trim()) {
      setStudentIdError("학번을 입력해주세요.");
      return;
    }
    if (!/^\d{8}$/.test(newStudentId.trim())) {
      setStudentIdError("학번은 8자리 숫자여야 합니다.");
      return;
    }
    if (!studentIdPassword) {
      setStudentIdError("비밀번호를 입력해주세요.");
      return;
    }

    setIsUpdatingStudentId(true);
    try {
      await customFetch("/api/v1/mypage/student-id", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          password: studentIdPassword,
          newStudentId: newStudentId.trim(),
        }),
      });
      cancelEditingStudentId();
      onStudentIdUpdated?.();
    } catch (error: unknown) {
      if (hasErrorCode(error, "INVALID_CREDENTIALS")) {
        setStudentIdError("비밀번호가 일치하지 않습니다.");
      } else if (hasErrorCode(error, "DUPLICATE_STUDENT_ID")) {
        setStudentIdError("이미 사용 중인 학번입니다.");
      } else if (hasErrorCode(error, "INVALID_STUDENT_ID")) {
        setStudentIdError("유효하지 않은 학번 형식입니다.");
      } else if (hasErrorCode(error, "STUDENT_ID_NOT_TEMPORARY")) {
        setStudentIdError("임시 학번이 아닌 경우 학번을 변경할 수 없습니다.");
      } else {
        setStudentIdError(getErrorMessage(error));
      }
    } finally {
      setIsUpdatingStudentId(false);
    }
  };

  const startEditing = (field: "email" | "phone") => {
    if (field === "email") {
      setEditValue(profile?.email ?? user.email ?? "");
    } else {
      setEditValue(profile?.phoneNumber ?? "");
    }
    setEditingField(field);
    setEditError("");
  };

  const cancelEditing = () => {
    setEditingField(undefined);
    setEditValue("");
    setEditError("");
  };

  const [editError, setEditError] = useState("");

  const validate = (): string => {
    if (!editingField) return "";
    const value = editValue.trim();
    if (!value)
      return editingField === "email"
        ? "이메일을 입력해주세요."
        : "전화번호를 입력해주세요.";
    if (editingField === "email") {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value))
        return "올바른 이메일 형식이 아닙니다.";
    } else {
      if (!/^01[0-9]-?\d{3,4}-?\d{4}$/.test(value))
        return "올바른 전화번호 형식이 아닙니다. (예: 01012345678)";
    }
    return "";
  };

  const handleSave = async () => {
    if (!onUpdateProfile || !editingField) return;

    const error = validate();
    if (error) {
      setEditError(error);
      return;
    }

    const data: UpdateProfileRequest =
      editingField === "email"
        ? { email: editValue.trim() }
        : { phoneNumber: editValue.trim().replace(/-/g, "") };

    await onUpdateProfile(data);
    setEditingField(undefined);
    setEditValue("");
    setEditError("");
  };

  return (
    <Card
      className={`p-s6 lg:p-s7 rounded-[2.5rem] border flex flex-col md:flex-row items-center gap-s6 ${
        isDark
          ? "bg-gradient-to-br from-card to-background border-border"
          : "bg-card border-border shadow-xl shadow-black/5"
      }`}
    >
      <div className="relative">
        <div className="w-32 h-32 rounded-[2.5rem] bg-primary/20 border border-primary/50 flex items-center justify-center">
          <User size={64} className="text-primary" />
        </div>
        {__FEATURE_PROFILE_EDIT__ && (
          <button
            type="button"
            className="absolute -bottom-2 -right-2 w-10 h-10 bg-foreground text-background rounded-r4 flex items-center justify-center hover:bg-primary transition border-2 border-background cursor-pointer"
          >
            <Edit3 size={16} />
          </button>
        )}
      </div>

      <div className="flex-1 text-center md:text-left">
        <div className="flex flex-col md:flex-row items-center gap-s3 mb-s3">
          <h2 className="typo-h1">{profile?.name ?? user.name}</h2>
          <span className="px-3 py-1 bg-primary/20 text-primary rounded-full typo-c2 font-bold uppercase tracking-widest border border-primary/30">
            {ROLE_LABELS[user.role]}
          </span>
        </div>
        {/* 학번 / 학과 / 가입일 */}
        <div className="flex flex-wrap justify-center md:justify-start gap-x-s5 gap-y-s2 text-muted-foreground typo-b2">
          <div className="flex items-center gap-2">
            <Shield size={16} className="text-primary" />
            {profile?.studentId ?? user.studentId}
            {hasTemporaryStudentId && (
              <span className="px-1.5 py-0.5 text-[10px] font-bold bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-400 rounded border border-amber-200 dark:border-amber-700">
                임시
              </span>
            )}
            {hasTemporaryStudentId && !isEditingStudentId && (
              <button
                type="button"
                onClick={startEditingStudentId}
                className="cursor-pointer ml-1 p-1 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
              >
                <Pencil size={14} />
              </button>
            )}
          </div>
          {profile?.department && (
            <div className="flex items-center gap-2">
              <Building2 size={16} className="text-primary" />
              {profile.department}
            </div>
          )}
          <div className="flex items-center gap-2 basis-full sm:basis-auto justify-center md:justify-start">
            <Calendar size={16} className="text-primary" />
            가입일{" "}
            {profile?.createdAt
              ? new Date(profile.createdAt).toLocaleDateString("ko-KR")
              : user.joinedDate}
          </div>
        </div>

        {/* 학번 변경 인라인 편집 */}
        {isEditingStudentId && (
          <div className="mt-s3 p-s3 rounded-r2 border border-border bg-muted/50 space-y-s2">
            <p className="text-sm font-medium text-foreground">학번 변경</p>
            <Input
              value={newStudentId}
              onChange={(e) => {
                setNewStudentId(e.target.value);
                setStudentIdError("");
              }}
              placeholder="새 학번 (8자리 숫자)"
              maxLength={8}
              disabled={isUpdatingStudentId}
            />
            <Input
              type="password"
              value={studentIdPassword}
              onChange={(e) => {
                setStudentIdPassword(e.target.value);
                setStudentIdError("");
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") void handleSaveStudentId();
                if (e.key === "Escape") cancelEditingStudentId();
              }}
              placeholder="현재 비밀번호"
              disabled={isUpdatingStudentId}
            />
            {studentIdError && (
              <p className="text-destructive typo-c2">{studentIdError}</p>
            )}
            <div className="flex gap-s2">
              <Button
                type="button"
                size="sm"
                onClick={() => void handleSaveStudentId()}
                disabled={isUpdatingStudentId}
                className="cursor-pointer"
              >
                {isUpdatingStudentId ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : (
                  <Check size={14} />
                )}
                저장
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={cancelEditingStudentId}
                disabled={isUpdatingStudentId}
                className="cursor-pointer"
              >
                <X size={14} />
                취소
              </Button>
            </div>
          </div>
        )}

        {/* 이메일 / 전화번호 (인라인 수정) */}
        <div className="mt-s2 space-y-s2 text-muted-foreground typo-b2">
          {/* 이메일 */}
          <div className="flex items-start justify-center md:justify-start gap-2">
            <Mail size={16} className="text-primary shrink-0 mt-1.5" />
            {editingField === "email" ? (
              <div>
                <div className="flex items-center gap-1">
                  <input
                    type="email"
                    value={editValue}
                    onChange={(e) => {
                      setEditValue(e.target.value);
                      setEditError("");
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") void handleSave();
                      if (e.key === "Escape") cancelEditing();
                    }}
                    className={cn(
                      "px-2 py-1 typo-b2 bg-background border rounded-md focus:outline-none focus:ring-1 focus:ring-primary w-56",
                      editError ? "border-destructive" : "border-border",
                    )}
                    autoFocus
                    disabled={isUpdating}
                  />
                  <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-primary/10 text-primary transition-colors disabled:opacity-50"
                  >
                    {isUpdating ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <Check size={14} />
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-destructive/10 text-destructive transition-colors disabled:opacity-50"
                  >
                    <X size={14} />
                  </button>
                </div>
                {editError && (
                  <p className="text-destructive typo-c2 mt-1">{editError}</p>
                )}
              </div>
            ) : (
              <>
                <span>{profile?.email ?? user.email}</span>
                {onUpdateProfile && (
                  <button
                    type="button"
                    onClick={() => startEditing("email")}
                    className="cursor-pointer ml-1 p-1 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
                  >
                    <Pencil size={14} />
                  </button>
                )}
              </>
            )}
          </div>

          {/* 전화번호 */}
          <div className="flex items-start justify-center md:justify-start gap-2">
            <Phone size={16} className="text-primary shrink-0 mt-1.5" />
            {editingField === "phone" ? (
              <div>
                <div className="flex items-center gap-1">
                  <input
                    type="tel"
                    value={editValue}
                    onChange={(e) => {
                      setEditValue(e.target.value);
                      setEditError("");
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") void handleSave();
                      if (e.key === "Escape") cancelEditing();
                    }}
                    placeholder="01012345678"
                    className={cn(
                      "px-2 py-1 typo-b2 bg-background border rounded-md focus:outline-none focus:ring-1 focus:ring-primary w-44",
                      editError ? "border-destructive" : "border-border",
                    )}
                    autoFocus
                    disabled={isUpdating}
                  />
                  <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-primary/10 text-primary transition-colors disabled:opacity-50"
                  >
                    {isUpdating ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <Check size={14} />
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={isUpdating}
                    className="cursor-pointer p-1 rounded-md hover:bg-destructive/10 text-destructive transition-colors disabled:opacity-50"
                  >
                    <X size={14} />
                  </button>
                </div>
                {editError && (
                  <p className="text-destructive typo-c2 mt-1">{editError}</p>
                )}
              </div>
            ) : (
              <>
                <span>
                  {profile?.phoneNumber
                    ? formatPhoneNumber(profile.phoneNumber)
                    : "-"}
                </span>
                {onUpdateProfile && (
                  <button
                    type="button"
                    onClick={() => startEditing("phone")}
                    className="cursor-pointer ml-1 p-1 rounded-md hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
                  >
                    <Pencil size={14} />
                  </button>
                )}
              </>
            )}
          </div>
        </div>

        {(onChangePassword || onLogout || onWithdraw) && (
          <div className="flex flex-wrap justify-center md:justify-start gap-s3 mt-s4">
            {onChangePassword && (
              <Button
                type="button"
                onClick={onChangePassword}
                variant="outline"
                className={`flex items-center gap-s2 rounded-r3 text-xs font-bold ${
                  isDark
                    ? "border-border hover:bg-white/5"
                    : "border-border hover:bg-muted"
                }`}
              >
                <Lock size={14} /> 비밀번호 변경
              </Button>
            )}
            {onLogout && (
              <Button
                type="button"
                onClick={onLogout}
                variant="outline"
                className="flex items-center gap-s2 rounded-r3 text-xs font-bold border-destructive/30 text-destructive hover:bg-destructive/10"
              >
                <LogOut size={14} /> 로그아웃
              </Button>
            )}
            {onWithdraw && (
              <Button
                type="button"
                onClick={onWithdraw}
                variant="outline"
                className="flex items-center gap-s2 rounded-r3 text-xs font-bold border-destructive/30 text-destructive hover:bg-destructive/10"
              >
                <UserX size={14} /> 회원 탈퇴
              </Button>
            )}
          </div>
        )}
      </div>
    </Card>
  );
}
