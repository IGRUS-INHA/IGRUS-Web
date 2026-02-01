import { CheckCircle, Clock, AlertCircle } from 'lucide-react';
import { Card } from '@/components/ui/card';

// 카테고리 한글 변환
const CATEGORY_LABELS = {
  TECHNICAL: '기술',
  ACCOUNT: '계정',
  EVENT: '행사',
  GENERAL: '일반',
};

// 날짜 포맷팅
const formatDate = (dateString) => {
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

// 상태별 스타일 및 아이콘
const getStatusInfo = (status) => {
  switch (status) {
    case 'ANSWERED':
      return {
        icon: CheckCircle,
        label: '답변완료',
        className: 'bg-success/20 text-success',
      };
    case 'IN_PROGRESS':
      return {
        icon: Clock,
        label: '처리중',
        className: 'bg-primary/20 text-primary',
      };
    case 'PENDING':
    default:
      return {
        icon: AlertCircle,
        label: '대기중',
        className: 'bg-warning/20 text-warning',
      };
  }
};

export default function InquiryListItem({ inquiry }) {
  const statusInfo = getStatusInfo(inquiry.status);
  const StatusIcon = statusInfo.icon;

  return (
    <Card className="p-6 rounded-[2rem] border bg-card border-border shadow-sm hover:shadow-md transition-shadow cursor-pointer">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 mb-2">
            <span className="text-c2 text-muted-foreground font-bold uppercase tracking-widest">
              {CATEGORY_LABELS[inquiry.category] || inquiry.category}
            </span>
            <span className="text-c2 text-muted-foreground">
              {inquiry.inquiryNumber}
            </span>
          </div>
          <h4 className="font-bold text-b1 mb-2 truncate">{inquiry.title}</h4>
          <p className="text-c1 text-muted-foreground">
            작성일: {formatDate(inquiry.createdAt)}
          </p>
          {inquiry.answeredAt && (
            <p className="text-c1 text-muted-foreground">
              답변일: {formatDate(inquiry.answeredAt)}
            </p>
          )}
        </div>
        <div
          className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-c2 font-bold whitespace-nowrap ${statusInfo.className}`}
        >
          <StatusIcon size={14} />
          {statusInfo.label}
        </div>
      </div>
    </Card>
  );
}
