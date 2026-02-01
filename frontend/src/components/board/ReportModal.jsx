import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { REPORT, REPORT_REASON_LABELS } from '@/constants/board';

/**
 * 신고 모달 컴포넌트
 */
export function ReportModal({ isOpen, onClose, onSubmit, targetType = '게시글' }) {
  const [reason, setReason] = useState('');
  const [detail, setDetail] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason) return;

    setLoading(true);
    try {
      await onSubmit({ reason, detail });
      onClose();
      setReason('');
      setDetail('');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* 배경 */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />

      {/* 모달 */}
      <div className="relative z-10 w-full max-w-md rounded-lg bg-background p-6 shadow-lg">
        <h2 className="mb-4 text-lg font-semibold">{targetType} 신고</h2>

        <form onSubmit={handleSubmit}>
          {/* 신고 사유 선택 */}
          <div className="mb-4">
            <label className="mb-2 block text-sm font-medium">
              신고 사유 <span className="text-destructive">*</span>
            </label>
            <div className="space-y-2">
              {Object.entries(REPORT_REASON_LABELS).map(([value, label]) => (
                <label key={value} className="flex items-center gap-2">
                  <input
                    type="radio"
                    name="reason"
                    value={value}
                    checked={reason === value}
                    onChange={(e) => setReason(e.target.value)}
                    className="h-4 w-4"
                  />
                  <span className="text-sm">{label}</span>
                </label>
              ))}
            </div>
          </div>

          {/* 상세 내용 */}
          <div className="mb-6">
            <label className="mb-2 block text-sm font-medium">
              상세 내용 (선택)
            </label>
            <textarea
              value={detail}
              onChange={(e) => setDetail(e.target.value)}
              placeholder="추가로 알려주실 내용이 있다면 작성해주세요"
              rows={3}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
          </div>

          {/* 버튼 */}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" disabled={!reason || loading}>
              {loading ? '신고 중...' : '신고하기'}
            </Button>
          </div>
        </form>

        <p className="mt-4 text-xs text-muted-foreground">
          허위 신고 시 제재를 받을 수 있습니다.
        </p>
      </div>
    </div>
  );
}
