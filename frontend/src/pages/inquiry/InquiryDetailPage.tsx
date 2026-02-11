import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle,
  Clock,
  AlertCircle,
  MessageSquare,
  Paperclip,
  FileText,
} from 'lucide-react';
import { useGetMyInquiry } from '@/api/model/inquiry/inquiry';
import type { InquiryResponse } from '@/api/model/models/inquiryResponse';
import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

interface StatusConfig {
  icon: LucideIcon;
  label: string;
  className: string;
}

const STATUS_CONFIG: Record<string, StatusConfig> = {
  PENDING: {
    icon: AlertCircle,
    label: '대기중',
    className: 'bg-warning/15 text-warning',
  },
  IN_PROGRESS: {
    icon: Clock,
    label: '처리중',
    className: 'bg-primary/15 text-primary',
  },
  COMPLETED: {
    icon: CheckCircle,
    label: '완료',
    className: 'bg-success/15 text-success',
  },
};

const formatDate = (dateString?: string): string => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatFileSize = (bytes?: number): string => {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
};

export default function InquiryDetailPage() {
  const { inquiryId } = useParams<{ inquiryId: string }>();
  const navigate = useNavigate();

  const { data: response, isLoading } = useGetMyInquiry(Number(inquiryId), {
    query: { enabled: !!inquiryId },
  });

  const inquiry = response?.data as InquiryResponse | undefined;

  const handleBack = () => {
    navigate('/inquiry/history');
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-s8">
        <p className="typo-b2 text-muted-foreground">로딩 중...</p>
      </div>
    );
  }

  if (!inquiry) {
    return (
      <div className="flex flex-col items-center justify-center py-s8 gap-s4">
        <p className="typo-b1 text-muted-foreground">문의를 찾을 수 없습니다.</p>
        <button
          type="button"
          onClick={handleBack}
          className="text-primary typo-b2 hover:underline cursor-pointer"
        >
          문의 내역으로 돌아가기
        </button>
      </div>
    );
  }

  const status = inquiry.status ?? 'PENDING';
  const config: StatusConfig = STATUS_CONFIG[status] ?? STATUS_CONFIG.PENDING!;
  const StatusIcon = config.icon;

  return (
    <div className="mx-auto max-w-2xl py-s6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* 뒤로가기 */}
      <button
        onClick={handleBack}
        type="button"
        className="mb-s6 flex items-center gap-s2 typo-b2 font-bold text-muted-foreground transition-colors hover:text-foreground cursor-pointer"
      >
        <ArrowLeft size={18} /> 문의 내역으로
      </button>

      {/* 문의 정보 카드 */}
      <div className="rounded-r4 border border-border bg-card p-s6 shadow-sm">
        {/* 상단: 유형 + 번호 + 상태 */}
        <div className="flex items-center justify-between gap-s4 mb-s5">
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
              'flex shrink-0 items-center gap-s1 rounded-full px-s3 py-s1 typo-c1 font-semibold',
              config.className,
            )}
          >
            <StatusIcon size={13} />
            {config.label}
          </div>
        </div>

        {/* 제목 */}
        <h1 className="typo-h3 font-bold mb-s4">{inquiry.title}</h1>

        {/* 구분선 */}
        <div className="h-px bg-border mb-s5" />

        {/* 내용 */}
        <p className="typo-b1 text-foreground whitespace-pre-wrap leading-relaxed mb-s5">
          {inquiry.content}
        </p>

        {/* 첨부파일 */}
        {inquiry.attachments && inquiry.attachments.length > 0 && (
          <div className="mb-s5">
            <div className="flex items-center gap-s2 mb-s3">
              <Paperclip size={14} className="text-muted-foreground" />
              <span className="typo-c1 font-semibold text-muted-foreground">
                첨부파일 ({inquiry.attachments.length})
              </span>
            </div>
            <div className="space-y-s2">
              {inquiry.attachments.map((file) => (
                <a
                  key={file.id}
                  href={file.fileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-s2 rounded-r2 border border-border bg-muted/30 px-s4 py-s3 transition-colors hover:bg-muted/60"
                >
                  <FileText size={16} className="shrink-0 text-primary" />
                  <span className="typo-b2 truncate">{file.fileName}</span>
                  {file.fileSize && (
                    <span className="typo-c2 shrink-0 text-muted-foreground">
                      {formatFileSize(file.fileSize)}
                    </span>
                  )}
                </a>
              ))}
            </div>
          </div>
        )}

        {/* 작성 정보 */}
        <div className="flex items-center gap-s3 typo-c1 text-muted-foreground">
          {inquiry.authorName && <span>{inquiry.authorName}</span>}
          {inquiry.authorName && inquiry.createdAt && (
            <span className="text-border">|</span>
          )}
          {inquiry.createdAt && <span>{formatDate(inquiry.createdAt)}</span>}
        </div>
      </div>

      {/* 답변 영역 */}
      <div className="mt-s4">
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
    </div>
  );
}
