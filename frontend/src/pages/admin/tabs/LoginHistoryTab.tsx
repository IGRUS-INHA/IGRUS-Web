import { useState } from 'react';
import { Search } from 'lucide-react';
import { useGetLoginHistories } from '@/api/model/admin-login-history/admin-login-history';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Pagination } from '@/components/board/Pagination';
import { cn } from '@/lib/utils';

export default function LoginHistoryTab() {
  const [studentId, setStudentId] = useState('');
  const [searchStudentId, setSearchStudentId] = useState('');
  const [successFilter, setSuccessFilter] = useState<'' | 'true' | 'false'>('');
  const [page, setPage] = useState(1);

  const { data: response, isLoading } = useGetLoginHistories({
    ...(searchStudentId && { studentId: searchStudentId }),
    ...(successFilter && { success: successFilter === 'true' }),
    page: page - 1,
    size: 30,
  });

  const data = response?.status === 200 ? response.data : undefined;
  const histories = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchStudentId(studentId);
    setPage(1);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-s6">
      {/* Filters */}
      <Card className="p-s5">
        <div className="flex flex-col lg:flex-row gap-s4">
          <form onSubmit={handleSearch} className="flex gap-s2 flex-1">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={16} />
              <Input
                placeholder="학번 검색"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                className="pl-9"
              />
            </div>
            <Button type="submit">검색</Button>
          </form>
          <select
            value={successFilter}
            onChange={(e) => { setSuccessFilter(e.target.value as '' | 'true' | 'false'); setPage(1); }}
            className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
          >
            <option value="">전체</option>
            <option value="true">성공</option>
            <option value="false">실패</option>
          </select>
        </div>
      </Card>

      {/* Table */}
      <Card className="p-s5 overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
              <th className="pb-s4 font-bold">학번</th>
              <th className="pb-s4 font-bold">결과</th>
              <th className="pb-s4 font-bold">IP 주소</th>
              <th className="pb-s4 font-bold">일시</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {(histories as Record<string, unknown>[]).map((h, idx) => (
              <tr key={idx}>
                <td className="py-s4 typo-b2 font-medium">{h.studentId as string}</td>
                <td className="py-s4">
                  <span className={cn(
                    'px-2 py-1 rounded-r2 typo-c2 font-bold',
                    h.success ? 'bg-success/10 text-success' : 'bg-destructive/10 text-destructive'
                  )}>
                    {h.success ? '성공' : '실패'}
                  </span>
                </td>
                <td className="py-s4 typo-b2 text-muted-foreground font-mono">{h.ipAddress as string}</td>
                <td className="py-s4 typo-b2 text-muted-foreground">
                  {h.loginAt ? new Date(h.loginAt as string).toLocaleString('ko-KR') : '-'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {histories.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">로그인 이력이 없습니다.</div>
        )}
      </Card>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
