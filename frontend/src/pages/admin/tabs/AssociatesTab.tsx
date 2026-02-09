import { useState } from 'react';
import {
  useGetPendingAssociates,
  useGetRejectedAssociates,
  useApproveAssociate,
  useRejectAssociate,
  useApproveBulk,
  useRejectBulk,
} from '@/api/model/admin-associate-approval/admin-associate-approval';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/board/Pagination';
import { useUIStore } from '@/stores';
import { cn } from '@/lib/utils';
import { useQueryClient } from '@tanstack/react-query';

type SubTab = 'pending' | 'rejected';

export default function AssociatesTab() {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const [subTab, setSubTab] = useState<SubTab>('pending');
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<number[]>([]);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectingId, setRejectingId] = useState<number | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/associates/pending'] });
    queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/associates/rejected'] });
    queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/users'] });
  };

  const { data: pendingRes, isLoading: pendingLoading } = useGetPendingAssociates(
    { page: page - 1, size: 20 },
    { query: { enabled: subTab === 'pending' } }
  );
  const { data: rejectedRes, isLoading: rejectedLoading } = useGetRejectedAssociates(
    { page: page - 1, size: 20 },
    { query: { enabled: subTab === 'rejected' } }
  );

  const { mutate: approveOne, isPending: approvingOne } = useApproveAssociate({
    mutation: {
      onSuccess: () => { addToast({ type: 'success', message: '승인 완료' }); invalidate(); },
      onError: () => { addToast({ type: 'error', message: '승인 실패' }); },
    },
  });

  const { mutate: rejectOne, isPending: rejectingOne } = useRejectAssociate({
    mutation: {
      onSuccess: () => { addToast({ type: 'success', message: '거절 완료' }); setRejectingId(null); setRejectReason(''); invalidate(); },
      onError: () => { addToast({ type: 'error', message: '거절 실패' }); },
    },
  });

  const { mutate: approveBulk, isPending: approvingBulk } = useApproveBulk({
    mutation: {
      onSuccess: (res) => {
        const d = res.status === 200 ? res.data : undefined;
        addToast({ type: 'success', message: `${d?.approvedCount ?? 0}명 일괄 승인 완료` });
        setSelected([]);
        invalidate();
      },
      onError: () => { addToast({ type: 'error', message: '일괄 승인 실패' }); },
    },
  });

  const { mutate: rejectBulk, isPending: rejectingBulk } = useRejectBulk({
    mutation: {
      onSuccess: (res) => {
        const d = res.status === 200 ? res.data : undefined;
        addToast({ type: 'success', message: `${d?.rejectedCount ?? 0}명 일괄 거절 완료` });
        setSelected([]);
        invalidate();
      },
      onError: () => { addToast({ type: 'error', message: '일괄 거절 실패' }); },
    },
  });

  const pendingData = pendingRes?.status === 200 ? pendingRes.data : undefined;
  const rejectedData = rejectedRes?.status === 200 ? rejectedRes.data : undefined;
  const pendingList = pendingData?.associates ?? [];
  const rejectedList = rejectedData?.content ?? [];
  const totalPages = subTab === 'pending' ? (pendingData?.totalPages ?? 0) : (rejectedData?.totalPages ?? 0);
  const isLoading = subTab === 'pending' ? pendingLoading : rejectedLoading;
  const isBusy = approvingOne || rejectingOne || approvingBulk || rejectingBulk;

  const toggleSelect = (userId: number) => {
    setSelected((prev) => prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId]);
  };

  const toggleAll = (userIds: number[]) => {
    setSelected((prev) => prev.length === userIds.length ? [] : userIds);
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
      {/* Sub tabs */}
      <div className="flex gap-s2">
        {(['pending', 'rejected'] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => { setSubTab(t); setPage(1); setSelected([]); }}
            className={cn(
              'px-s4 py-s2 rounded-r3 text-sm font-medium transition-all cursor-pointer',
              subTab === t ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'
            )}
          >
            {t === 'pending' ? '승인 대기' : '거절됨'}
          </button>
        ))}
      </div>

      {/* Bulk actions (pending only) */}
      {subTab === 'pending' && pendingList.length > 0 && (
        <div className="flex gap-s3 items-center">
          <span className="text-sm text-muted-foreground">{selected.length}명 선택</span>
          <Button
            size="sm"
            onClick={() => approveBulk({ data: { userIds: selected } })}
            disabled={selected.length === 0 || isBusy}
          >
            일괄 승인
          </Button>
          <Button
            size="sm"
            variant="destructive"
            onClick={() => {
              const reason = prompt('일괄 거절 사유를 입력하세요');
              if (reason) rejectBulk({ data: { userIds: selected, reason } });
            }}
            disabled={selected.length === 0 || isBusy}
          >
            일괄 거절
          </Button>
        </div>
      )}

      {/* Pending List */}
      {subTab === 'pending' && (
        <Card className="p-s5 overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
                <th className="pb-s4">
                  <input
                    type="checkbox"
                    checked={selected.length === pendingList.length && pendingList.length > 0}
                    onChange={() => toggleAll(pendingList.map((a) => a.userId!).filter(Boolean))}
                    className="accent-primary"
                  />
                </th>
                <th className="pb-s4 font-bold">학번</th>
                <th className="pb-s4 font-bold">이름</th>
                <th className="pb-s4 font-bold hidden lg:table-cell">학과</th>
                <th className="pb-s4 font-bold hidden lg:table-cell">가입 동기</th>
                <th className="pb-s4 font-bold">신청일</th>
                <th className="pb-s4 font-bold text-right">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {pendingList.map((a) => (
                <tr key={a.userId}>
                  <td className="py-s4">
                    <input
                      type="checkbox"
                      checked={selected.includes(a.userId!)}
                      onChange={() => toggleSelect(a.userId!)}
                      className="accent-primary"
                    />
                  </td>
                  <td className="py-s4 text-b2 font-medium">{a.studentId}</td>
                  <td className="py-s4 text-b2 font-bold">{a.name}</td>
                  <td className="py-s4 text-b2 text-muted-foreground hidden lg:table-cell">{a.department}</td>
                  <td className="py-s4 text-b2 text-muted-foreground hidden lg:table-cell max-w-[200px] truncate">{a.motivation}</td>
                  <td className="py-s4 text-b2 text-muted-foreground">
                    {a.createdAt ? new Date(a.createdAt).toLocaleDateString('ko-KR') : '-'}
                  </td>
                  <td className="py-s4 text-right">
                    {rejectingId === a.userId ? (
                      <div className="flex items-center gap-s2 justify-end">
                        <input
                          type="text"
                          placeholder="거절 사유"
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                          className="px-s2 py-1 rounded-r2 border border-border bg-background text-sm w-32"
                        />
                        <Button
                          size="xs"
                          variant="destructive"
                          onClick={() => rejectOne({ id: a.userId!, data: { reason: rejectReason } })}
                          disabled={!rejectReason || isBusy}
                        >
                          확인
                        </Button>
                        <Button size="xs" variant="outline" onClick={() => { setRejectingId(null); setRejectReason(''); }}>
                          취소
                        </Button>
                      </div>
                    ) : (
                      <div className="flex gap-s2 justify-end">
                        <Button size="xs" onClick={() => approveOne({ id: a.userId! })} disabled={isBusy}>
                          승인
                        </Button>
                        <Button size="xs" variant="destructive" onClick={() => setRejectingId(a.userId!)} disabled={isBusy}>
                          거절
                        </Button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {pendingList.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">승인 대기 중인 준회원이 없습니다.</div>
          )}
        </Card>
      )}

      {/* Rejected List */}
      {subTab === 'rejected' && (
        <Card className="p-s5 overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
                <th className="pb-s4 font-bold">학번</th>
                <th className="pb-s4 font-bold">이름</th>
                <th className="pb-s4 font-bold">거절 사유</th>
                <th className="pb-s4 font-bold">거절일</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {rejectedList.map((a) => (
                <tr key={a.userId}>
                  <td className="py-s4 text-b2 font-medium">{a.studentId}</td>
                  <td className="py-s4 text-b2 font-bold">{a.name}</td>
                  <td className="py-s4 text-b2 text-muted-foreground">{a.rejectionReason ?? '-'}</td>
                  <td className="py-s4 text-b2 text-muted-foreground">
                    {a.rejectedAt ? new Date(a.rejectedAt).toLocaleDateString('ko-KR') : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {rejectedList.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">거절된 준회원이 없습니다.</div>
          )}
        </Card>
      )}

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
