import { useState } from 'react';
import { useUIStore } from '@/stores/uiStore';
import {
  useGetMyInquiries,
  useCreateMemberInquiry,
} from '@/api/model/inquiry/inquiry';
// import type { PageInquiryListResponse } from '@/api/model/models'; // TODO: 백엔드 수정 후 사용
import InquiryForm from '@/components/feature/inquiry/InquiryForm';
import InquiryListItem from '@/components/feature/inquiry/InquiryListItem';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui';
import { cn } from '@/lib/utils';
import type { Inquiry } from '@/types/entities';
import type { InquiryType } from '@/types/common';

// 문의 유형 매핑 (UI → API)
const TYPE_MAPPING: Record<string, string> = {
  signup: 'JOIN',
  event: 'GENERAL',
  report: 'BUG_REPORT',
  account: 'GENERAL',
  other: 'GENERAL',
};

type ViewType = 'form' | 'history';

export default function InquiryPage() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [view, setView] = useState<ViewType>('form');

  // 문의 목록 조회
  const { data: response, isLoading, refetch } = useGetMyInquiries({
    page: 0,
    size: 20,
  });

  // 문의 작성
  const createMutation = useCreateMemberInquiry();

  const handleSubmit = async (data: { type: string; title: string; content: string }) => {
    try {
      const apiType = TYPE_MAPPING[data.type] || 'GENERAL';

      await createMutation.mutateAsync({
        data: {
          title: data.title,
          content: data.content,
          type: apiType as any, // TODO: 백엔드 OpenAPI 스펙 수정 필요
        },
      });

      alert('문의가 제출되었습니다.');
      await refetch();
      setView('history');
    } catch (error) {
      console.error('문의 제출 실패:', error);
      alert('문의 제출에 실패했습니다. 다시 시도해주세요.');
    }
  };

  // TODO: 백엔드 OpenAPI 스펙 수정 필요 (Blob 대신 실제 타입 필요)
  const inquiriesData = response?.data ? (response.data as any) : null;
  const inquiries: Inquiry[] = inquiriesData?.content?.map((item: any) => ({
    id: item.id?.toString() || '',
    inquiryNumber: item.inquiryNumber || `INQ-${item.id}`,
    category: (item.type || 'GENERAL') as InquiryType,
    title: item.title || '',
    content: '',
    status: item.status || 'PENDING',
    createdAt: item.createdAt || '',
    ...(item.hasReply && { answeredAt: item.createdAt || '', answer: '답변 완료' }),
  })) || [];

  return (
    <div className="flex items-center justify-center h-full">
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
            <Card className="py-5 px-11 rounded-[2.5rem] border-transparent bg-transparent shadow-none">
              <h3 className="text-h3 opacity-0 pointer-events-none">문의하기</h3>
              <div className="flex items-center justify-center min-h-[548px]">
                <Spinner size="lg" />
              </div>
            </Card>
          ) : inquiries.length === 0 ? (
            <Card className="py-5 px-11 rounded-[2.5rem] border-transparent bg-transparent shadow-none">
              <h3 className="text-h3 opacity-0 pointer-events-none">문의하기</h3>
              <div className="flex items-center justify-center min-h-[548px]">
                <div className="text-center text-muted-foreground">
                  문의 내역이 없습니다.
                </div>
              </div>
            </Card>
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
