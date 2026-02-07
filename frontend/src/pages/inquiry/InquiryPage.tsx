import { Link, useNavigate } from 'react-router-dom';
import { useUIStore } from '@/stores/uiStore';
import { useCreateMemberInquiry } from '@/api/model/inquiry/inquiry';
import InquiryForm from '@/components/feature/inquiry/InquiryForm';
import { cn } from '@/lib/utils';

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
  const navigate = useNavigate();

  // 문의 작성
  const createMutation = useCreateMemberInquiry();

  const handleSubmit = async (data: { type: string; title: string; content: string }) => {
    try {
      const apiType = TYPE_MAPPING[data.type] || 'GENERAL';

      await createMutation.mutateAsync({
        data: {
          title: data.title,
          content: data.content,
          type: apiType,
        },
      });

      alert('문의가 제출되었습니다.');
      navigate('/inquiry/history');
    } catch (error) {
      console.error('문의 제출 실패:', error);
      alert('문의 제출에 실패했습니다. 다시 시도해주세요.');
    }
  };

  return (
    <div className="animate-in fade-in duration-500">
      {/* Section Header */}
      <section>
        <div className="flex justify-between items-center mb-s6">
          <div>
            <h3
              className={cn(
                'text-2xl font-bold transition-colors',
                isDark ? 'text-white' : 'text-black'
              )}
            >
              문의하기
            </h3>
            <p className="text-gray-500 text-sm">
              궁금한 점이나 건의사항을 남겨주세요.
            </p>
          </div>
          <Link
            to="/inquiry/history"
            className="text-sm text-gray-400 hover:text-[#03A69E] transition"
          >
            문의 내역 보기
          </Link>
        </div>

        {/* Inquiry Form */}
        <InquiryForm onSubmit={handleSubmit} loading={createMutation.isPending} />
      </section>
    </div>
  );
}
