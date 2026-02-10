import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import type { User } from '@/types/entities';

interface UserTableUser extends Partial<User> {
  id: string;
  studentId?: string;
  name: string;
  status?: string;
  role: string;
}

interface UserTableProps {
  users?: UserTableUser[];
  title?: string;
  onExport?: () => void;
  onViewAll?: () => void;
  onEdit?: (user: UserTableUser) => void;
}

export default function UserTable({
  users = [],
  title = '부원 관리',
  onExport,
  onViewAll,
  onEdit,
}: UserTableProps) {
  return (
    <Card className="p-s6 rounded-[2.5rem] border bg-card border-border shadow-sm">
      <div className="flex justify-between items-center mb-s5">
        <h3 className="typo-h3">{title}</h3>
        <div className="flex gap-s3">
          {onViewAll && (
            <Button variant="outline" className="rounded-r3 typo-c1 font-bold" onClick={onViewAll}>
              전체 보기
            </Button>
          )}
          {onExport && (
            <Button className="rounded-r3 typo-c1 font-bold shadow-lg shadow-primary/20" onClick={onExport}>
              CSV 내보내기
            </Button>
          )}
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
              <th className="pb-s4 font-bold">학번</th>
              <th className="pb-s4 font-bold">이름</th>
              <th className="pb-s4 font-bold">상태</th>
              <th className="pb-s4 font-bold">권한</th>
              <th className="pb-s4 font-bold text-right">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.map((user) => (
              <tr key={user.id} className="group">
                <td className="py-s4 typo-b2 font-medium">{user.studentId ?? user.id}</td>
                <td className="py-s4 typo-b2 font-bold">{user.name}</td>
                <td className="py-s4">
                  <span
                    className={`px-2 py-1 rounded-r2 typo-c2 font-bold ${
                      user.status === 'Active'
                        ? 'bg-success/10 text-success'
                        : 'bg-destructive/10 text-destructive'
                    }`}
                  >
                    {user.status === 'Active' ? '활성' : '정지'}
                  </span>
                </td>
                <td className="py-s4 typo-b2 text-muted-foreground">{user.role}</td>
                <td className="py-s4 text-right">
                  <button
                    type="button"
                    className="text-primary hover:underline typo-c1 font-bold cursor-pointer"
                    onClick={() => onEdit?.(user)}
                  >
                    수정
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
