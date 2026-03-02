import { useState } from "react";
import {
  useGetAllInquiries,
  useGetInquiryDetail,
  useUpdateInquiryStatus,
  useCreateReply,
  useUpdateReply,
  useCreateMemo,
  useDeleteInquiry,
} from "@/api/model/inquiry/inquiry";
import type { GetAllInquiriesType } from "@/api/model/models/getAllInquiriesType";
import type { GetAllInquiriesStatus } from "@/api/model/models/getAllInquiriesStatus";
import type { UpdateInquiryStatusRequestStatus } from "@/api/model/models/updateInquiryStatusRequestStatus";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/board/Pagination";
import { useUIStore } from "@/stores";
import { cn } from "@/lib/utils";
import { useQueryClient } from "@tanstack/react-query";
import { X } from "lucide-react";

const TYPE_OPTIONS: { value: GetAllInquiriesType | ""; label: string }[] = [
  { value: "", label: "전체 유형" },
  { value: "JOIN", label: "가입" },
  { value: "EVENT", label: "행사" },
  { value: "REPORT", label: "신고" },
  { value: "ACCOUNT", label: "계정" },
  { value: "OTHER", label: "기타" },
];

const STATUS_FILTER_OPTIONS: {
  value: GetAllInquiriesStatus | "";
  label: string;
}[] = [
  { value: "", label: "전체 상태" },
  { value: "PENDING", label: "대기" },
  { value: "IN_PROGRESS", label: "처리중" },
  { value: "COMPLETED", label: "완료" },
];

const STATUS_CHANGE_OPTIONS: {
  value: UpdateInquiryStatusRequestStatus;
  label: string;
}[] = [
  { value: "PENDING", label: "대기" },
  { value: "IN_PROGRESS", label: "처리중" },
  { value: "COMPLETED", label: "완료" },
];

const STATUS_BADGE: Record<string, string> = {
  PENDING: "bg-warning/10 text-warning",
  IN_PROGRESS: "bg-info/10 text-info",
  ANSWERED: "bg-success/10 text-success",
  COMPLETED: "bg-success/10 text-success",
  CLOSED: "bg-muted text-muted-foreground",
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: "대기",
  IN_PROGRESS: "처리중",
  ANSWERED: "답변완료",
  COMPLETED: "완료",
  CLOSED: "종료",
};

export default function InquiriesTab() {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const [typeFilter, setTypeFilter] = useState<GetAllInquiriesType | "">("");
  const [statusFilter, setStatusFilter] = useState<GetAllInquiriesStatus | "">(
    "",
  );
  const [page, setPage] = useState(1);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [memoContent, setMemoContent] = useState("");

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["/api/v1/inquiries"] });
    queryClient.invalidateQueries({ queryKey: ["/api/v1/admin/dashboard"] });
  };

  const { data: listRes, isLoading } = useGetAllInquiries({
    ...(typeFilter && { type: typeFilter }),
    ...(statusFilter && { status: statusFilter }),
    page: page - 1,
    size: 20,
  });

  const { data: detailRes } = useGetInquiryDetail(selectedId!, {
    query: { enabled: !!selectedId },
  });

  const { mutate: updateStatus } = useUpdateInquiryStatus({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "상태 변경 완료" });
        invalidate();
      },
      onError: () => {
        addToast({ type: "error", message: "상태 변경 실패" });
      },
    },
  });

  const { mutate: createReply, isPending: replying } = useCreateReply({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "답변 작성 완료" });
        setReplyContent("");
        invalidate();
        if (selectedId)
          queryClient.invalidateQueries({
            queryKey: [`/api/v1/inquiries/${selectedId}`],
          });
      },
      onError: () => {
        addToast({ type: "error", message: "답변 작성 실패" });
      },
    },
  });

  const { mutate: updateReply, isPending: updatingReply } = useUpdateReply({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "답변 수정 완료" });
        if (selectedId)
          queryClient.invalidateQueries({
            queryKey: [`/api/v1/inquiries/${selectedId}`],
          });
      },
      onError: () => {
        addToast({ type: "error", message: "답변 수정 실패" });
      },
    },
  });

  const { mutate: createMemo, isPending: creatingMemo } = useCreateMemo({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "메모 작성 완료" });
        setMemoContent("");
        if (selectedId)
          queryClient.invalidateQueries({
            queryKey: [`/api/v1/inquiries/${selectedId}`],
          });
      },
      onError: () => {
        addToast({ type: "error", message: "메모 작성 실패" });
      },
    },
  });

  const { mutate: deleteInquiry } = useDeleteInquiry({
    mutation: {
      onSuccess: () => {
        addToast({ type: "success", message: "문의 삭제 완료" });
        setSelectedId(null);
        invalidate();
      },
      onError: () => {
        addToast({ type: "error", message: "문의 삭제 실패" });
      },
    },
  });

  const listData = listRes?.status === 200 ? listRes.data : undefined;
  const inquiries = listData?.inquiries ?? [];
  const totalPages = listData?.totalPages ?? 0;
  const detail = detailRes?.status === 200 ? detailRes.data : undefined;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-s6">
      {/* Filters */}
      <div className="flex gap-s2">
        <select
          value={typeFilter}
          onChange={(e) => {
            setTypeFilter(e.target.value as GetAllInquiriesType | "");
            setPage(1);
          }}
          className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
        >
          {TYPE_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value as GetAllInquiriesStatus | "");
            setPage(1);
          }}
          className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
        >
          {STATUS_FILTER_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex gap-s6">
        {/* List */}
        <Card
          className={cn(
            "p-s5 overflow-x-auto",
            selectedId ? "flex-1" : "w-full",
          )}
        >
          <table className="w-full text-left">
            <thead>
              <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
                <th className="pb-s4 font-bold">번호</th>
                <th className="pb-s4 font-bold">유형</th>
                <th className="pb-s4 font-bold">제목</th>
                <th className="pb-s4 font-bold">작성자</th>
                <th className="pb-s4 font-bold">상태</th>
                <th className="pb-s4 font-bold">작성일</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {inquiries.map((inq) => (
                <tr
                  key={inq.id}
                  className={cn(
                    "cursor-pointer hover:bg-muted/50 transition",
                    selectedId === inq.id && "bg-muted/50",
                  )}
                  onClick={() => setSelectedId(inq.id!)}
                >
                  <td className="py-s4 typo-b2 font-medium">
                    {inq.inquiryNumber}
                  </td>
                  <td className="py-s4 typo-b2 text-muted-foreground">
                    {inq.typeDescription}
                  </td>
                  <td className="py-s4 typo-b2 font-bold max-w-[200px] truncate">
                    {inq.title}
                  </td>
                  <td className="py-s4 typo-b2 text-muted-foreground">
                    {inq.authorName}
                    {inq.guest ? " (비회원)" : ""}
                  </td>
                  <td className="py-s4">
                    <span
                      className={cn(
                        "px-2 py-1 rounded-r2 typo-c2 font-bold",
                        STATUS_BADGE[inq.status ?? ""],
                      )}
                    >
                      {STATUS_LABEL[inq.status ?? ""] ?? inq.status}
                    </span>
                  </td>
                  <td className="py-s4 typo-b2 text-muted-foreground">
                    {inq.createdAt
                      ? new Date(inq.createdAt).toLocaleDateString("ko-KR")
                      : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {inquiries.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">
              문의가 없습니다.
            </div>
          )}
        </Card>

        {/* Detail Panel */}
        {selectedId && detail && (
          <Card className="w-[400px] p-s5 space-y-s5 flex-shrink-0 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-start">
              <h3 className="text-h4 font-bold">{detail.title}</h3>
              <button
                type="button"
                onClick={() => setSelectedId(null)}
                className="text-muted-foreground hover:text-foreground cursor-pointer"
              >
                <X size={18} />
              </button>
            </div>

            <div className="text-sm text-muted-foreground space-y-s1">
              <p>문의번호: {detail.inquiryNumber}</p>
              <p>유형: {detail.typeDescription}</p>
              <p>
                작성자: {detail.authorName} ({detail.authorEmail})
              </p>
              <p>
                작성일:{" "}
                {detail.createdAt
                  ? new Date(detail.createdAt).toLocaleString("ko-KR")
                  : "-"}
              </p>
            </div>

            {/* Content */}
            <div className="p-s4 bg-muted/30 rounded-r3 typo-b2 whitespace-pre-wrap">
              {detail.content}
            </div>

            {/* Status change */}
            <div className="flex gap-s2 items-center">
              <span className="text-sm font-medium">상태:</span>
              <select
                value={detail.status ?? ""}
                onChange={(e) =>
                  updateStatus({
                    id: selectedId,
                    data: {
                      status: e.target
                        .value as UpdateInquiryStatusRequestStatus,
                    },
                  })
                }
                className="px-s2 py-1 rounded-r2 border border-border bg-background text-sm"
              >
                {STATUS_CHANGE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Reply */}
            <div className="space-y-s3">
              <h4 className="typo-label font-bold">답변</h4>
              {detail.reply ? (
                <div className="space-y-s2">
                  <div className="p-s3 bg-primary/5 rounded-r3 typo-b2 whitespace-pre-wrap">
                    {detail.reply.content}
                  </div>
                  <textarea
                    placeholder="답변 수정..."
                    defaultValue={detail.reply.content ?? ""}
                    onBlur={(e) => {
                      if (
                        e.target.value &&
                        e.target.value !== detail.reply?.content
                      ) {
                        updateReply({
                          id: selectedId,
                          data: { content: e.target.value },
                        });
                      }
                    }}
                    className="w-full p-s3 rounded-r2 border border-border bg-background text-sm min-h-[60px] resize-y"
                  />
                </div>
              ) : (
                <div className="space-y-s2">
                  <textarea
                    placeholder="답변 작성..."
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    className="w-full p-s3 rounded-r2 border border-border bg-background text-sm min-h-[80px] resize-y"
                  />
                  <Button
                    size="sm"
                    onClick={() =>
                      createReply({
                        id: selectedId,
                        data: { content: replyContent },
                      })
                    }
                    disabled={!replyContent || replying || updatingReply}
                  >
                    답변 등록
                  </Button>
                </div>
              )}
            </div>

            {/* Memos */}
            <div className="space-y-s3">
              <h4 className="typo-label font-bold">내부 메모</h4>
              {detail.memos?.map((memo) => (
                <div
                  key={memo.id}
                  className="p-s3 bg-warning/5 rounded-r3 typo-c1 border border-warning/20"
                >
                  <p className="whitespace-pre-wrap">{memo.content}</p>
                  <p className="typo-c2 text-muted-foreground mt-s1">
                    {memo.writtenByName} -{" "}
                    {memo.createdAt
                      ? new Date(memo.createdAt).toLocaleString("ko-KR")
                      : ""}
                  </p>
                </div>
              ))}
              <div className="flex gap-s2">
                <input
                  type="text"
                  placeholder="메모 추가..."
                  value={memoContent}
                  onChange={(e) => setMemoContent(e.target.value)}
                  className="flex-1 px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
                />
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    createMemo({
                      id: selectedId,
                      data: { content: memoContent },
                    })
                  }
                  disabled={!memoContent || creatingMemo}
                >
                  추가
                </Button>
              </div>
            </div>

            {/* Delete */}
            <Button
              variant="destructive"
              size="sm"
              onClick={() => {
                if (confirm("정말 삭제하시겠습니까?"))
                  deleteInquiry({ id: selectedId });
              }}
            >
              문의 삭제
            </Button>
          </Card>
        )}
      </div>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        onPageChange={setPage}
      />
    </div>
  );
}
