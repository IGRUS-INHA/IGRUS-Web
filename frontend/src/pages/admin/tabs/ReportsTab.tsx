import { useGetPendingReports, useUpdateReportStatus } from '@/api/model/comment-report/comment-report';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useUIStore } from '@/stores';
import { cn } from '@/lib/utils';
import { useQueryClient } from '@tanstack/react-query';

export default function ReportsTab() {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const { data: response, isLoading } = useGetPendingReports();

  const { mutate: updateStatus, isPending } = useUpdateReportStatus({
    mutation: {
      onSuccess: () => {
        addToast({ type: 'success', message: '신고 처리 완료' });
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/comment-reports'] });
      },
      onError: () => {
        addToast({ type: 'error', message: '신고 처리 실패' });
      },
    },
  });

  const reports = response?.status === 200 ? response.data : [];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <Card className="p-s5 overflow-x-auto">
      <table className="w-full text-left">
        <thead>
          <tr className="text-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
            <th className="pb-s4 font-bold">ID</th>
            <th className="pb-s4 font-bold">댓글 내용</th>
            <th className="pb-s4 font-bold">신고자</th>
            <th className="pb-s4 font-bold">신고 사유</th>
            <th className="pb-s4 font-bold">상태</th>
            <th className="pb-s4 font-bold">신고일</th>
            <th className="pb-s4 font-bold text-right">작업</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {reports.map((r) => (
            <tr key={r.id}>
              <td className="py-s4 text-b2 font-medium">{r.id}</td>
              <td className="py-s4 text-b2 max-w-[200px] truncate">{r.commentContent}</td>
              <td className="py-s4 text-b2 text-muted-foreground">{r.reporterName}</td>
              <td className="py-s4 text-b2 text-muted-foreground max-w-[150px] truncate">{r.reason}</td>
              <td className="py-s4">
                <span className={cn(
                  'px-2 py-1 rounded-r2 text-c2 font-bold',
                  r.status === 'PENDING' ? 'bg-warning/10 text-warning'
                    : r.status === 'RESOLVED' ? 'bg-success/10 text-success'
                    : 'bg-muted text-muted-foreground'
                )}>
                  {r.status === 'PENDING' ? '대기' : r.status === 'RESOLVED' ? '처리됨' : '기각'}
                </span>
              </td>
              <td className="py-s4 text-b2 text-muted-foreground">
                {r.createdAt ? new Date(r.createdAt).toLocaleDateString('ko-KR') : '-'}
              </td>
              <td className="py-s4 text-right">
                {r.status === 'PENDING' && (
                  <div className="flex gap-s2 justify-end">
                    <Button
                      size="xs"
                      onClick={() => updateStatus({ reportId: r.id!, data: { status: 'RESOLVED' } })}
                      disabled={isPending}
                    >
                      처리
                    </Button>
                    <Button
                      size="xs"
                      variant="outline"
                      onClick={() => updateStatus({ reportId: r.id!, data: { status: 'DISMISSED' } })}
                      disabled={isPending}
                    >
                      기각
                    </Button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {reports.length === 0 && (
        <div className="text-center py-12 text-muted-foreground">대기 중인 신고가 없습니다.</div>
      )}
    </Card>
  );
}
