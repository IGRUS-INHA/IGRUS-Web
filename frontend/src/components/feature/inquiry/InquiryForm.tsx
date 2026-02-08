import { Send } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

interface InquiryFormData {
  type: string;
  title: string;
  content: string;
}

interface InquiryFormProps {
  onSubmit?: (data: InquiryFormData) => void;
  loading?: boolean;
}

export default function InquiryForm({ onSubmit, loading = false }: InquiryFormProps) {
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    onSubmit?.({
      type: formData.get('type') as string,
      title: formData.get('title') as string,
      content: formData.get('content') as string,
    });
  };

  return (
    <Card className="py-5 px-11 rounded-[2.5rem] border bg-card shadow-xl">
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-c1 font-bold text-muted-foreground uppercase tracking-widest mb-2">
            문의 유형
          </label>
          <select
            name="type"
            className="w-full rounded-r4 px-4 py-3 border bg-input border-border focus:outline-none focus:border-primary focus:ring-[3px] focus:ring-ring/50 appearance-none text-foreground"
          >
            <option value="signup">가입/입부 문의</option>
            <option value="event">행사 참여</option>
            <option value="report">신고</option>
            <option value="account">계정/설정</option>
            <option value="other">기타</option>
          </select>
        </div>
        <div>
          <label className="block text-c1 font-bold text-muted-foreground uppercase tracking-widest mb-2">
            제목
          </label>
          <Input
            type="text"
            name="title"
            className="w-full rounded-r4 px-4 py-3 border bg-input border-border"
            placeholder="문의 제목을 입력해주세요"
          />
        </div>
        <div>
          <label className="block text-c1 font-bold text-muted-foreground uppercase tracking-widest mb-2">
            내용
          </label>
          <textarea
            name="content"
            rows={6}
            className="w-full rounded-r4 px-4 py-3 border bg-input border-border focus:outline-none focus:border-primary focus:ring-[3px] focus:ring-ring/50 resize-none text-foreground placeholder:text-muted-foreground"
            placeholder="문의 내용을 상세히 작성해주세요..."
          />
        </div>
        <Button
          type="submit"
          disabled={loading}
          className="w-full py-4 rounded-r4 font-bold flex items-center justify-center gap-2 shadow-lg shadow-primary/20"
        >
          <Send size={18} /> {loading ? '제출 중...' : '문의 제출'}
        </Button>
      </form>
    </Card>
  );
}
