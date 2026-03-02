import { useState } from "react";
import { Link } from "react-router-dom";
import { PenLine, Inbox } from "lucide-react";
import { useGetMyInquiries } from "@/api/model/inquiry/inquiry";
import type { InquiryListPageResponse } from "@/api/model/models/inquiryListPageResponse";
import InquiryListItem from "@/components/feature/inquiry/InquiryListItem";

export default function InquiryHistoryPage() {
  const [currentPage] = useState(0);

  const { data: response, isLoading } = useGetMyInquiries({
    page: currentPage,
    size: 10,
  });

  const pageData = response?.data as InquiryListPageResponse | undefined;
  const inquiries = pageData?.inquiries ?? [];

  return (
    <div className="mx-auto flex h-full max-w-2xl flex-col justify-center py-s6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Header */}
      <div className="mb-s7">
        <div className="flex items-center justify-between gap-s4">
          <div>
            <h1 className="typo-h2 text-foreground">문의 내역</h1>
            <p className="mt-s1 typo-b2 text-muted-foreground">
              제출한 문의의 처리 현황을 확인하세요.
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

        {/* 액센트 라인 */}
        <div className="mt-s5 h-px bg-border" />
      </div>

      {/* List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-s8">
          <p className="typo-b2 text-muted-foreground">로딩 중...</p>
        </div>
      ) : inquiries.length === 0 ? (
        <div className="flex min-h-[30rem] flex-col items-center justify-center py-s8 text-center">
          <Inbox size={48} className="mb-s4 text-muted-foreground/40" />
          <p className="typo-b1 font-semibold text-muted-foreground">
            문의 내역이 없습니다
          </p>
          <p className="mt-s1 typo-b2 text-muted-foreground/70">
            궁금한 점이 있으면 문의를 남겨보세요.
          </p>
        </div>
      ) : (
        <div className="space-y-s3">
          {inquiries.map((inquiry) => (
            <InquiryListItem key={inquiry.id} inquiry={inquiry} />
          ))}
        </div>
      )}
    </div>
  );
}
