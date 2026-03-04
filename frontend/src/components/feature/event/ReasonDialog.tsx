import { useState } from "react";

interface ReasonDialogProps {
  title: string;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}

export default function ReasonDialog({
  title,
  onConfirm,
  onCancel,
}: ReasonDialogProps) {
  const [reason, setReason] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-r4 border bg-card border-border p-s6 shadow-xl space-y-s4">
        <h3 className="text-lg font-bold">{title}</h3>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="사유를 입력하세요"
          className="w-full h-24 px-s3 py-s2 rounded-r2 border border-border bg-background text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <div className="flex justify-end gap-s2">
          <button
            type="button"
            onClick={onCancel}
            className="px-s4 py-s2 rounded-r2 text-sm font-medium border border-border hover:bg-muted transition cursor-pointer"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => onConfirm(reason)}
            disabled={!reason.trim()}
            className="px-s4 py-s2 rounded-r2 text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
}
