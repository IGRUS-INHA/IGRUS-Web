import { Link } from 'react-router-dom';
import { CheckCircle, Clock, AlertCircle } from 'lucide-react';
import type { InquiryListResponse } from '@/api/model/models/inquiryListResponse';
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
  const date = new Date(dateString);
  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

interface InquiryListItemProps {
  inquiry: InquiryListResponse;
}

export default function InquiryListItem({ inquiry }: InquiryListItemProps) {
  const status = inquiry.status ?? 'PENDING';
  const config: StatusConfig = STATUS_CONFIG[status] ?? {
    icon: AlertCircle,
    label: '대기중',
    className: 'bg-warning/15 text-warning',
  };
  const StatusIcon = config.icon;

  return (
    <Link
      to={`/inquiry/history/${inquiry.id}`}
      className="block rounded-r3 border border-border bg-card p-s5 transition-colors hover:border-primary/30"
    >
      <div className="flex items-start justify-between gap-s4">
        <div className="min-w-0 flex-1">
          {/* 유형 + 문의번호 */}
          <div className="mb-s2 flex items-center gap-s3">
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

          {/* 제목 */}
          <h4 className="typo-b1 font-bold truncate">{inquiry.title}</h4>

          {/* 작성일 */}
          <p className="mt-s1 typo-c1 text-muted-foreground">
            {formatDate(inquiry.createdAt)}
          </p>
        </div>

        {/* 상태 뱃지 */}
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
    </Link>
  );
}
