import { useState, FormEvent, ChangeEvent } from "react";
import { Button } from "@/components/ui/button";
import { REPORT_REASON_LABELS } from "@/constants/board";

interface ReportData {
  reason: string;
  detail: string;
}

interface ReportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ReportData) => Promise<void>;
  targetType?: string;
}

/**
 * 신고 모달 컴포넌트
 */
export function ReportModal({
  isOpen,
  onClose,
  onSubmit,
  targetType = "게시글",
}: ReportModalProps) {
  const [reason, setReason] = useState("");
  const [detail, setDetail] = useState("");
  const [loading, setLoading] = useState(false);

  if (!isOpen) return undefined;

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!reason) return;

    setLoading(true);
    try {
      await onSubmit({ reason, detail });
      onClose();
      setReason("");
      setDetail("");
    } finally {
      setLoading(false);
    }
  };

  const handleReasonChange = (e: ChangeEvent<HTMLInputElement>) => {
    setReason(e.target.value);
  };

  const handleDetailChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    setDetail(e.target.value);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* 배경 */}
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />

      {/* 모달 */}
      <div className="relative z-10 w-full max-w-md rounded-r2 bg-background p-s6 shadow-lg">
        <h2 className="mb-s4 text-lg font-semibold">{targetType} 신고</h2>

        <form onSubmit={handleSubmit}>
          {/* 신고 사유 선택 */}
          <div className="mb-s4">
            <label className="mb-s2 block text-sm font-medium">
              신고 사유 <span className="text-destructive">*</span>
            </label>
            <div className="space-y-s2">
              {Object.entries(REPORT_REASON_LABELS).map(([value, label]) => (
                <label key={value} className="flex items-center gap-s2">
                  <input
                    type="radio"
                    name="reason"
                    value={value}
                    checked={reason === value}
                    onChange={handleReasonChange}
                    className="h-4 w-4"
                  />
                  <span className="text-sm">{label}</span>
                </label>
              ))}
            </div>
          </div>

          {/* 상세 내용 */}
          <div className="mb-s5">
            <label className="mb-s2 block text-sm font-medium">
              상세 내용 (선택)
            </label>
            <textarea
              value={detail}
              onChange={handleDetailChange}
              placeholder="추가로 알려주실 내용이 있다면 작성해주세요"
              rows={3}
              className="w-full rounded-r2 border border-input bg-background px-s3 py-s2 text-sm"
            />
          </div>

          {/* 버튼 */}
          <div className="flex justify-end gap-s2">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" disabled={!reason || loading}>
              {loading ? "신고 중..." : "신고하기"}
            </Button>
          </div>
        </form>

        <p className="mt-s4 text-xs text-muted-foreground">
          허위 신고 시 제재를 받을 수 있습니다.
        </p>
      </div>
    </div>
  );
}
