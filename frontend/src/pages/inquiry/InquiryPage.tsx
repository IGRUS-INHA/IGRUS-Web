import { Link, useNavigate } from 'react-router-dom';
import { Clock, Search } from 'lucide-react';
import { useCreateMemberInquiry, useCreateGuestInquiry } from '@/api/model/inquiry/inquiry';
import { useAuth } from '@/hooks/useAuth';
import InquiryForm from '@/components/feature/inquiry/InquiryForm';
import type { InquiryFormData } from '@/components/feature/inquiry/InquiryForm';
import type { CreateMemberInquiryRequestType } from '@/api/model/models/createMemberInquiryRequestType';
import type { CreateGuestInquiryRequestType } from '@/api/model/models/createGuestInquiryRequestType';

export default function InquiryPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const memberMutation = useCreateMemberInquiry();
  const guestMutation = useCreateGuestInquiry();

  const handleMemberSubmit = async (data: InquiryFormData) => {
    try {
      await memberMutation.mutateAsync({
        data: {
          title: data.title,
          content: data.content,
          type: data.type as CreateMemberInquiryRequestType,
        },
      });
      alert('문의가 제출되었습니다.');
      navigate('/inquiry/history');
    } catch {
      alert('문의 제출에 실패했습니다. 다시 시도해주세요.');
    }
  };

  const handleGuestSubmit = async (data: InquiryFormData) => {
    try {
      await guestMutation.mutateAsync({
        data: {
          title: data.title,
          content: data.content,
          type: data.type as CreateGuestInquiryRequestType,
          email: data.email!,
          name: data.name!,
          password: data.password!,
        },
      });
      alert('문의가 제출되었습니다. 문의번호와 이메일, 비밀번호로 조회할 수 있습니다.');
      navigate('/inquiry/lookup');
    } catch {
      alert('문의 제출에 실패했습니다. 다시 시도해주세요.');
    }
  };

  return (
    <div className="mx-auto flex h-full max-w-2xl flex-col justify-center py-s6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Header */}
      <div className="mb-s7">
        <div className="flex items-center justify-between gap-s4">
          <div>
            <h1 className="typo-h2 text-foreground">문의하기</h1>
            <p className="mt-s1 typo-b2 text-muted-foreground">
              궁금한 점이나 건의사항을 남겨주세요.
            </p>
          </div>

          {/* 문의 조회 링크 */}
          {isAuthenticated ? (
            <Link
              to="/inquiry/history"
              className="flex shrink-0 items-center gap-s2 text-muted-foreground typo-b2 transition hover:text-primary"
            >
              <Clock size={15} />
              문의 내역
            </Link>
          ) : (
            <Link
              to="/inquiry/lookup"
              className="flex shrink-0 items-center gap-s2 text-muted-foreground typo-b2 transition hover:text-primary"
            >
              <Search size={15} />
              문의 조회
            </Link>
          )}
        </div>

        {/* 액센트 라인 */}
        <div className="mt-s5 h-px bg-border" />
      </div>

      {/* Form Card */}
      <div className="rounded-r4 border border-border bg-card p-s6 shadow-sm">
        {isAuthenticated ? (
          <InquiryForm
            variant="member"
            onSubmit={handleMemberSubmit}
            loading={memberMutation.isPending}
          />
        ) : (
          <InquiryForm
            variant="guest"
            onSubmit={handleGuestSubmit}
            loading={guestMutation.isPending}
          />
        )}
      </div>
    </div>
  );
}
