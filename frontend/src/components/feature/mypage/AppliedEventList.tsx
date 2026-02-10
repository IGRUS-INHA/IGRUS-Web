import { useUIStore } from '@/stores';
import { Award } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface AppliedEvent {
  id: number | string;
  title: string;
  status: string;
}

interface AppliedEventListProps {
  events?: AppliedEvent[];
}

export default function AppliedEventList({ events = [] }: AppliedEventListProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const mockEvents: AppliedEvent[] = events.length > 0 ? events : [
    { id: 1, title: '월간 웹 개발 세미나', status: '신청 완료' },
  ];

  return (
    <Card className={`p-s6 rounded-[2.5rem] border ${isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'}`}>
      <h3 className="typo-h3 mb-s5 flex items-center gap-2">
        <Award size={20} className="text-primary" />
        신청한 행사
      </h3>
      <div className="space-y-s3">
        {mockEvents.map((event) => (
          <div key={event.id} className="p-s4 rounded-r4 border border-primary/30 bg-primary/5">
            <div className="flex justify-between items-center">
              <div>
                <h4 className="font-bold typo-b2">{event.title}</h4>
                <p className="typo-c1 text-primary mt-1 font-bold">상태: {event.status}</p>
              </div>
              <Button variant="ghost" size="sm" className="typo-c1 text-muted-foreground hover:text-destructive">
                취소
              </Button>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
}
