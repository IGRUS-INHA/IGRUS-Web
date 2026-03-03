import { useState } from "react";
import { Link } from "react-router-dom";
import {
  PenLine,
  Hash,
  Mail,
  Lock,
  Search,
  CheckCircle,
  Clock,
  AlertCircle,
  MessageSquare,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useLookupGuestInquiry } from "@/api/model/guest-inquiry/guest-inquiry";
import type { InquiryResponse } from "@/api/model/models/inquiryResponse";
import { cn } from "@/lib/utils";

const STATUS_MAP: Record<
  string,
  { icon: typeof CheckCircle; label: string; className: string }
> = {
  PENDING: {
    icon: AlertCircle,
    label: "대기중",
    className: "bg-warning/15 text-warning",
  },
  IN_PROGRESS: {
    icon: Clock,
    label: "처리중",
    className: "bg-primary/15 text-primary",
  },
  COMPLETED: {
    icon: CheckCircle,
    label: "완료",
    className: "bg-success/15 text-success",
  },
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return "";
  return new Date(dateString).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function InquiryLookupPage() {
  const [error, setError] = useState("");
  const [result, setResult] = useState<InquiryResponse | null>(null);

  const mutation = useLookupGuestInquiry();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError("");

    const formData = new FormData(e.currentTarget);
    const inquiryNumber = formData.get("inquiryNumber") as string;
    const email = formData.get("email") as string;
    const password = formData.get("password") as string;

    try {
      const res = await mutation.mutateAsync({
        data: { inquiryNumber, email, password },
      });
      setResult(res.data as InquiryResponse);
    } catch {
      setResult(null);
      setError("문의를 찾을 수 없습니다. 입력 정보를 확인해주세요.");
    }
  };

  return (
    <div className="mx-auto max-w-2xl py-s6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Header */}
      <div className="mb-s7">
        <div className="flex items-center justify-between gap-s4">
          <div>
            <h1 className="typo-h2 text-foreground">문의 조회</h1>
            <p className="mt-s1 typo-b2 text-muted-foreground">
              문의번호와 이메일, 비밀번호로 조회할 수 있습니다.
            </p>
          </div>

          <Link
            to="/inquiry"
            className="flex shrink-0 items-center gap-s2 text-muted-foreground typo-b2 transition hover:text-primary"
          >
            <PenLine size={15} />
            문의하기
          </Link>
        </div>

        <div className="mt-s5 h-px bg-border" />
      </div>

      {/* Lookup Form */}
      <div className="rounded-r4 border border-border bg-card p-s6 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-s4">
          <FormField label="문의번호" icon={Hash}>
            <Input
              name="inquiryNumber"
              required
              placeholder="INQ-XXXXXXXX"
              className="pl-10"
            />
          </FormField>

          <FormField label="이메일" icon={Mail}>
            <Input
              type="email"
              name="email"
              required
              placeholder="문의 시 입력한 이메일"
              className="pl-10"
            />
          </FormField>

          <FormField label="비밀번호" icon={Lock}>
            <Input
              type="password"
              name="password"
              required
              placeholder="문의 시 설정한 비밀번호"
              className="pl-10"
            />
          </FormField>

          {error && <p className="typo-c1 text-destructive">{error}</p>}

          <Button
            type="submit"
            disabled={mutation.isPending}
            className="flex w-full items-center justify-center gap-s2 rounded-r3 py-s5 font-bold"
          >
            {mutation.isPending ? "조회 중..." : "문의 조회"}
            {!mutation.isPending && <Search size={18} />}
          </Button>
        </form>
      </div>

      {/* Result */}
      {result && <InquiryResult inquiry={result} />}
    </div>
  );
}

/** 조회 결과 표시 */
function InquiryResult({ inquiry }: { inquiry: InquiryResponse }) {
  const status = inquiry.status ?? "PENDING";
  const statusInfo = STATUS_MAP[status] ?? STATUS_MAP.PENDING!;
  const StatusIcon = statusInfo!.icon;

  return (
    <div className="mt-s6 space-y-s4 animate-in fade-in slide-in-from-bottom-2 duration-300">
      {/* 문의 정보 카드 */}
      <div className="rounded-r4 border border-border bg-card p-s6 shadow-sm">
        {/* 상단: 유형 + 번호 + 상태 */}
        <div className="flex items-center justify-between gap-s4 mb-s4">
          <div className="flex items-center gap-s3">
            {inquiry.typeDescription && (
              <span className="typo-c1 font-semibold text-primary">
                {inquiry.typeDescription}
              </span>
            )}
            {inquiry.inquiryNumber && (
              <span className="typo-c1 text-muted-foreground">
                {inquiry.inquiryNumber}
              </span>
            )}
          </div>
          <div
            className={cn(
              "flex shrink-0 items-center gap-s1 rounded-full px-s3 py-s1 typo-c1 font-semibold",
              statusInfo!.className,
            )}
          >
            <StatusIcon size={13} />
            {statusInfo!.label}
          </div>
        </div>

        {/* 제목 */}
        <h3 className="typo-b1 font-bold mb-s2">{inquiry.title}</h3>

        {/* 내용 */}
        <p className="typo-b2 text-muted-foreground whitespace-pre-wrap mb-s4">
          {inquiry.content}
        </p>

        {/* 작성일 */}
        <p className="typo-c1 text-muted-foreground">
          {formatDate(inquiry.createdAt)}
        </p>
      </div>

      {/* 답변 카드 */}
      {inquiry.reply ? (
        <div className="rounded-r4 border border-primary/20 bg-primary/5 p-s6">
          <div className="flex items-center gap-s2 mb-s3">
            <MessageSquare size={16} className="text-primary" />
            <span className="typo-b2 font-semibold text-primary">답변</span>
            {inquiry.reply.repliedByName && (
              <span className="typo-c1 text-muted-foreground">
                {inquiry.reply.repliedByName}
              </span>
            )}
          </div>
          <p className="typo-b2 text-foreground whitespace-pre-wrap">
            {inquiry.reply.content}
          </p>
          {inquiry.reply.createdAt && (
            <p className="mt-s3 typo-c1 text-muted-foreground">
              {formatDate(inquiry.reply.createdAt)}
            </p>
          )}
        </div>
      ) : (
        <div className="rounded-r4 border border-border bg-muted/30 p-s5 text-center">
          <p className="typo-b2 text-muted-foreground">
            아직 답변이 등록되지 않았습니다.
          </p>
        </div>
      )}
    </div>
  );
}

/** 아이콘 + 라벨 폼 필드 래퍼 */
function FormField({
  label,
  icon: Icon,
  children,
}: {
  label: string;
  icon: typeof Search;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="mb-s2 block text-muted-foreground typo-label">
        {label}
      </label>
      <div className="relative">
        <Icon
          size={18}
          className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none z-10"
        />
        {children}
      </div>
    </div>
  );
}
