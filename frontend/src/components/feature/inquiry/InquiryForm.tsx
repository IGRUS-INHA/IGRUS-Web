import {
  Send,
  MessageSquareText,
  User,
  Mail,
  Lock,
  ChevronDown,
  type LucideIcon,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { CreateMemberInquiryRequestType } from '@/api/model/models/createMemberInquiryRequestType';
import { cn } from '@/lib/utils';

const TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: CreateMemberInquiryRequestType.JOIN, label: '가입/입부 문의' },
  { value: CreateMemberInquiryRequestType.EVENT, label: '행사 참여' },
  { value: CreateMemberInquiryRequestType.REPORT, label: '신고' },
  { value: CreateMemberInquiryRequestType.ACCOUNT, label: '계정/설정' },
  { value: CreateMemberInquiryRequestType.OTHER, label: '기타' },
];

export interface InquiryFormData {
  type: string;
  title: string;
  content: string;
  /** 비회원 전용 */
  email?: string;
  name?: string;
  password?: string;
}

interface InquiryFormProps {
  variant?: 'member' | 'guest';
  onSubmit?: (data: InquiryFormData) => void;
  loading?: boolean;
}

export default function InquiryForm({
  variant = 'member',
  onSubmit,
  loading = false,
}: InquiryFormProps) {
  const isGuest = variant === 'guest';
  const typeOptions = TYPE_OPTIONS;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    onSubmit?.({
      type: formData.get('type') as string,
      title: formData.get('title') as string,
      content: formData.get('content') as string,
      ...(isGuest && {
        email: formData.get('email') as string,
        name: formData.get('name') as string,
        password: formData.get('password') as string,
      }),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-s5">
      {/* 비회원 본인 확인 필드 */}
      {isGuest && (
        <div className="space-y-s4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-s4">
            <FormField label="이름" icon={User}>
              <Input
                name="name"
                required
                maxLength={50}
                placeholder="이름을 입력해주세요"
                className="pl-10"
              />
            </FormField>
            <FormField label="이메일" icon={Mail}>
              <Input
                type="email"
                name="email"
                required
                placeholder="답변 받을 이메일"
                className="pl-10"
              />
            </FormField>
          </div>
          <FormField label="문의 조회 비밀번호" icon={Lock}>
            <Input
              type="password"
              name="password"
              required
              placeholder="문의 조회 시 사용할 비밀번호"
              className="pl-10"
            />
          </FormField>

          {/* 구분선 */}
          <div className="relative py-s1">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-border" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-card px-s4 typo-c1 text-muted-foreground">
                문의 내용
              </span>
            </div>
          </div>
        </div>
      )}

      {/* 문의 유형 */}
      <FormField label="문의 유형" icon={MessageSquareText}>
        <div className="relative">
          <select
            name="type"
            className={cn(
              'w-full h-9 rounded-r2 border border-input bg-transparent pl-10 pr-10 text-sm',
              'appearance-none cursor-pointer transition-all outline-none',
              'focus:border-ring focus:ring-ring/50 focus:ring-[3px]',
            )}
          >
            {typeOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            size={16}
            className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
          />
        </div>
      </FormField>

      {/* 제목 */}
      <FormField label="제목">
        <Input
          name="title"
          required
          maxLength={100}
          placeholder="문의 제목을 입력해주세요"
        />
      </FormField>

      {/* 내용 */}
      <FormField label="내용">
        <textarea
          name="content"
          required
          rows={6}
          placeholder="문의 내용을 상세히 작성해주세요..."
          className={cn(
            'w-full rounded-r2 border border-input bg-transparent px-s3 py-s2 text-sm',
            'placeholder:text-muted-foreground resize-none transition-all outline-none',
            'focus:border-ring focus:ring-ring/50 focus:ring-[3px]',
          )}
        />
      </FormField>

      {/* 제출 */}
      <Button
        type="submit"
        disabled={loading}
        className="flex w-full items-center justify-center gap-s2 rounded-r3 py-s5 font-bold"
      >
        {loading ? '제출 중...' : '문의 제출'}
        {!loading && <Send size={18} />}
      </Button>
    </form>
  );
}

/** 아이콘 + 라벨이 붙는 폼 필드 래퍼 */
function FormField({
  label,
  icon: Icon,
  children,
}: {
  label: string;
  icon?: LucideIcon;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label className="mb-s2 block text-muted-foreground typo-label">
        {label}
      </label>
      <div className="relative">
        {Icon && (
          <Icon
            size={18}
            className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none z-10"
          />
        )}
        {children}
      </div>
    </div>
  );
}
