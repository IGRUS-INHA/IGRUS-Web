import { useState } from 'react';
import { useUIStore } from '@/stores/uiStore';
import {
  useGetMyInquiries,
  useCreateMemberInquiry,
} from '@/api/model/inquiry/inquiry';
import type { PageInquiryListResponse, CreateInquiryResponse } from '@/api/model/models';
import InquiryForm from '@/components/feature/inquiry/InquiryForm';
import InquiryListItem from '@/components/feature/inquiry/InquiryListItem';
import { cn } from '@/lib/utils';
import type { Inquiry } from '@/types/entities';

type ViewType = 'form' | 'history';

// 문의 유형 매핑 (UI → API)
const TYPE_MAPPING: Record<string, string> = {
  signup: 'JOIN',
  event: 'GENERAL',
  report: 'BUG_REPORT',
  account: 'GENERAL',
  other: 'GENERAL',
};

export default function InquiryPage() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [view, setView] = useState<ViewType>('form');
  const [currentPage, setCurrentPage] = useState(1);

  // 문의 목록 조회 (페이지네이션: 0-based)
  const { data: response, isLoading, refetch } = useGetMyInquiries({
    page: currentPage - 1,
    size: 10,
  });

  // 문의 작성
  const createMutation = useCreateMemberInquiry();

  const handleSubmit = async (data: { type: string; title: string; content: string }) => {
    try {
      // UI의 type을 API의 type으로 변환
      const apiType = TYPE_MAPPING[data.type] || 'GENERAL';

      await createMutation.mutateAsync({
        data: {
          title: data.title,
          content: data.content,
          type: apiType,
        },
      });

      alert('문의가 제출되었습니다.');

      // 문의 목록 새로고침
      refetch();

      // 문의 내역 보기로 전환
      setView('history');
    } catch (error) {
      console.error('문의 제출 실패:', error);
      alert('문의 제출에 실패했습니다. 다시 시도해주세요.');
    }
  };

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
    <div className="flex items-start justify-center pt-s7">
      <div className="max-w-3xl w-full animate-in slide-in-from-bottom-8 duration-500">
        {/* View Toggle Buttons */}
        <div className="flex gap-s4 mb-s5">
        <button
          type="button"
          onClick={() => setView('form')}
          className={cn(
            'flex-1 py-s3 rounded-r4 font-bold transition-all border cursor-pointer text-xs',
            view === 'form'
              ? 'bg-primary text-primary-foreground border-primary'
              : isDark
                ? 'bg-white/5 border-border text-muted-foreground'
                : 'bg-muted border-border text-muted-foreground'
          )}
        >
          새 문의 작성
        </button>
        <button
          type="button"
          onClick={() => setView('history')}
          className={cn(
            'flex-1 py-s3 rounded-r4 font-bold transition-all border cursor-pointer text-xs',
            view === 'history'
              ? 'bg-primary text-primary-foreground border-primary'
              : isDark
                ? 'bg-white/5 border-border text-muted-foreground'
                : 'bg-muted border-border text-muted-foreground'
          )}
        >
          문의 내역 보기
        </button>
      </div>

      {/* Content */}
      {view === 'form' ? (
        <InquiryForm onSubmit={handleSubmit} loading={createMutation.isPending} />
      ) : (
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
      )}
      </div>
    </div>
  );
}
