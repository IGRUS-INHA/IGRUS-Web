import { useState } from 'react';
import { useUIStore } from '@/stores/uiStore';
import { useGetMyInquiries } from '@/api/model/inquiry/inquiry';
import type { PageInquiryListResponse } from '@/api/model/models';
import InquiryListItem from '@/components/feature/inquiry/InquiryListItem';
import { cn } from '@/lib/utils';
import type { Inquiry } from '@/types/entities';

export default function InquiryHistoryPage() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [currentPage] = useState(1);

  const { data: response, isLoading } = useGetMyInquiries({
    page: currentPage - 1,
    size: 10,
  });

  // Blob 타입 우회하여 실제 데이터 추출
  const inquiriesData = response?.data ? (response.data as unknown as PageInquiryListResponse) : null;
  const inquiries: Inquiry[] = inquiriesData?.content?.map((item) => ({
    id: item.id?.toString() || '',
    inquiryNumber: `INQ-${item.id}`,
    category: item.type || 'GENERAL',
    title: item.title || '',
    content: item.content || '',
    status: item.status || 'PENDING',
    createdAt: item.createdAt || '',
    answeredAt: item.reply?.createdAt,
    answer: item.reply?.content,
  })) || [];

  return (
    <div className="animate-in fade-in duration-500">
      <section>
        <div className="mb-s6">
          <h3
            className={cn(
              'text-2xl font-bold transition-colors',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            문의 내역
          </h3>
          <p className="text-gray-500 text-sm">
            제출한 문의의 처리 현황을 확인하세요.
          </p>
        </div>

        <div className="space-y-s4">
          {isLoading ? (
            <div className="flex items-center justify-center min-h-[27rem]">
              <div className="text-center text-muted-foreground">로딩 중...</div>
            </div>
          ) : inquiries.length === 0 ? (
            <div className="flex items-center justify-center min-h-[27rem]">
              <div className="text-center text-muted-foreground">
                문의 내역이 없습니다.
              </div>
            </div>
          ) : (
            inquiries.map((inquiry) => (
              <InquiryListItem key={inquiry.id} inquiry={inquiry} />
            ))
          )}
        </div>
      </section>
    </div>
  );
}
