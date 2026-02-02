import { useUIStore } from '@/stores';
import { Layers } from 'lucide-react';
import { Card } from '@/components/ui/card';

interface Activity {
  id: number | string;
  board: string;
  date: string;
  title: string;
}

interface ActivityListProps {
  activities?: Activity[];
}

export default function ActivityList({ activities = [] }: ActivityListProps) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const mockActivities: Activity[] = activities.length > 0 ? activities : [
    { id: 1, board: '자유게시판', date: '2일 전', title: 'Next.js 프로젝트 최적화 팁 공유합니다' },
    { id: 2, board: '정보공유', date: '5일 전', title: '웹 보안 입문자를 위한 추천 강의' },
    { id: 3, board: '자유게시판', date: '1주 전', title: '이번 주말 CTF 참가자 모집' },
  ];

  return (
    <Card className={`p-s6 rounded-[2.5rem] border ${isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'}`}>
      <h3 className="text-h3 mb-s5 flex items-center gap-2">
        <Layers size={20} className="text-primary" />
        최근 활동
      </h3>
      <div className="space-y-s3">
        {mockActivities.map((activity) => (
          <div
            key={activity.id}
            className={`p-s4 rounded-r4 border ${isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'}`}
          >
            <p className="text-c1 text-muted-foreground mb-1">
              {activity.board} • {activity.date}
            </p>
            <h4 className="font-bold text-b2">{activity.title}</h4>
          </div>
        ))}
      </div>
    </Card>
  );
}
