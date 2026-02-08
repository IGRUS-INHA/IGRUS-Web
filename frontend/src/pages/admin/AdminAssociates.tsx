import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useUIStore } from '@/stores';
import { cn } from '@/lib/utils';
import { UserCheck, CheckCircle, XCircle, Users, Loader2, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/board/Pagination';
import {
  useGetPendingAssociates,
  useApproveAssociate,
  useApproveBulk,
  useRejectAssociate,
  useRejectBulk,
  useGetRejectedAssociates,
  getGetPendingAssociatesQueryKey,
  getGetRejectedAssociatesQueryKey,
} from '@/api/model/admin-associate-approval/admin-associate-approval';
import type {
  AssociateInfoResponse,
  BulkApprovalResultResponse,
  BulkRejectionResultResponse,
} from '@/api/model/models';

const PAGE_SIZE = 20;

type TabType = 'pending' | 'rejected';

function formatDate(dateString: string | undefined): string {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
}

type ValidAssociate = AssociateInfoResponse & { userId: number };

function hasValidUserId(associate: AssociateInfoResponse): associate is ValidAssociate {
  return associate.userId !== undefined;
}

export default function AdminAssociates() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<TabType>('pending');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [page, setPage] = useState(1);
  const [rejectedPage, setRejectedPage] = useState(1);
  const [isProcessing, setIsProcessing] = useState(false);

  // 거절 모달 state
  const [rejectModal, setRejectModal] = useState<{
    isOpen: boolean;
    targetIds: number[];
    isBulk: boolean;
    targetName?: string;
  }>({ isOpen: false, targetIds: [], isBulk: false });
  const [rejectReason, setRejectReason] = useState('');

  // API - 승인 대기 목록
  const { data: pendingResponse, isLoading: isPendingLoading, isError: isPendingError } = useGetPendingAssociates(
    { page: page - 1, size: PAGE_SIZE },
    { query: { enabled: activeTab === 'pending' } }
  );

  // API - 거절됨 목록
  const { data: rejectedResponse, isLoading: isRejectedLoading, isError: isRejectedError } = useGetRejectedAssociates(
    { page: rejectedPage - 1, size: PAGE_SIZE },
    { query: { enabled: activeTab === 'rejected' } }
  );

  // Mutations
  const approveMutation = useApproveAssociate();
  const bulkApproveMutation = useApproveBulk();
  const rejectMutation = useRejectAssociate();
  const bulkRejectMutation = useRejectBulk();

  // 승인 대기 데이터
  const pendingData = pendingResponse?.data;
  const associates = pendingData?.associates ?? [];
  const pendingTotal = pendingData?.totalElements ?? 0;
  const pendingTotalPages = pendingData?.totalPages ?? 0;

  // 거절됨 데이터
  const rejectedData = rejectedResponse?.data;
  const rejectedList = rejectedData?.content ?? [];
  const rejectedTotal = rejectedData?.totalElements ?? 0;
  const rejectedTotalPages = rejectedData?.totalPages ?? 0;

  const validAssociates = associates.filter(hasValidUserId);

  const isAllSelected = validAssociates.length > 0
    && validAssociates.every((a) => selectedIds.has(a.userId));

  const isLoading = activeTab === 'pending' ? isPendingLoading : isRejectedLoading;
  const isError = activeTab === 'pending' ? isPendingError : isRejectedError;

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    setSelectedIds(new Set());
  };

  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(validAssociates.map((a) => a.userId)));
    }
  };

  const handleSelectOne = (userId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  };

  const invalidateQueries = () => {
    queryClient.invalidateQueries({ queryKey: getGetPendingAssociatesQueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetRejectedAssociatesQueryKey() });
  };

  const handleApproveOne = async (associate: ValidAssociate) => {
    if (!window.confirm(`${associate.name} 님을 정회원으로 승인하시겠습니까?`)) return;
    setIsProcessing(true);
    try {
      await approveMutation.mutateAsync({ id: associate.userId });
      invalidateQueries();
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(associate.userId);
        return next;
      });
      alert(`${associate.name} 님이 정회원으로 승인되었습니다.`);
    } catch {
      alert('승인에 실패했습니다. 다시 시도해주세요.');
    }
    setIsProcessing(false);
  };

  const handleApproveBulk = async () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`${selectedIds.size}명을 정회원으로 승인하시겠습니까?`)) return;
    setIsProcessing(true);
    try {
      const result = await bulkApproveMutation.mutateAsync({
        data: { userIds: Array.from(selectedIds) },
      });
      const resultData = result.data as BulkApprovalResultResponse;
      invalidateQueries();
      setSelectedIds(new Set());
      if (resultData.failedCount && resultData.failedCount > 0) {
        alert(`승인 완료: ${resultData.approvedCount}명 성공, ${resultData.failedCount}명 실패`);
      } else {
        alert(`${resultData.approvedCount}명이 정회원으로 승인되었습니다.`);
      }
    } catch {
      alert('일괄 승인에 실패했습니다. 다시 시도해주세요.');
    }
    setIsProcessing(false);
  };

  // 거절 모달
  const openRejectModal = (targetIds: number[], isBulk: boolean, targetName?: string) => {
    setRejectModal({ isOpen: true, targetIds, isBulk, targetName });
    setRejectReason('');
  };

  const closeRejectModal = () => {
    setRejectModal({ isOpen: false, targetIds: [], isBulk: false });
    setRejectReason('');
  };

  const handleRejectConfirm = async () => {
    if (!rejectReason.trim()) return;
    setIsProcessing(true);
    try {
      if (rejectModal.isBulk) {
        const result = await bulkRejectMutation.mutateAsync({
          data: { userIds: rejectModal.targetIds, reason: rejectReason.trim() },
        });
        const resultData = result.data as BulkRejectionResultResponse;
        invalidateQueries();
        setSelectedIds(new Set());
        if (resultData.failedCount && resultData.failedCount > 0) {
          alert(`거절 완료: ${resultData.rejectedCount}명 성공, ${resultData.failedCount}명 실패`);
        } else {
          alert(`${resultData.rejectedCount}명이 거절되었습니다.`);
        }
      } else {
        await rejectMutation.mutateAsync({
          id: rejectModal.targetIds[0],
          data: { reason: rejectReason.trim() },
        });
        invalidateQueries();
        setSelectedIds((prev) => {
          const next = new Set(prev);
          next.delete(rejectModal.targetIds[0]);
          return next;
        });
        alert(`${rejectModal.targetName ?? '해당 준회원'}이(가) 거절되었습니다.`);
      }
    } catch {
      alert('거절에 실패했습니다. 다시 시도해주세요.');
    }
    setIsProcessing(false);
    closeRejectModal();
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    setSelectedIds(new Set());
  };

  const handleRejectedPageChange = (newPage: number) => {
    setRejectedPage(newPage);
  };

  return (
    <div className="space-y-s6 animate-in fade-in duration-300">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-s3">
          <UserCheck className="w-6 h-6 text-primary" />
          <h1 className="text-h2 font-bold">준회원 관리</h1>
          <span className={cn(
            'px-s3 py-s1 rounded-r4 text-xs font-bold',
            pendingTotal > 0
              ? 'bg-primary/10 text-primary'
              : 'bg-muted text-muted-foreground'
          )}>
            대기 {pendingTotal}명
          </span>
        </div>
      </div>

      {/* 탭 */}
      <div className={cn(
        'flex gap-s1 border-b',
        isDark ? 'border-white/10' : 'border-gray-200'
      )}>
        <button
          type="button"
          onClick={() => handleTabChange('pending')}
          className={cn(
            'px-s5 py-s3 text-sm font-medium transition-colors border-b-2 -mb-px',
            activeTab === 'pending'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          )}
        >
          승인 대기
        </button>
        <button
          type="button"
          onClick={() => handleTabChange('rejected')}
          className={cn(
            'px-s5 py-s3 text-sm font-medium transition-colors border-b-2 -mb-px',
            activeTab === 'rejected'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground hover:text-foreground'
          )}
        >
          거절됨
          {rejectedTotal > 0 && (
            <span className="ml-s2 text-xs text-muted-foreground">({rejectedTotal})</span>
          )}
        </button>
      </div>

      {/* 액션 바 (승인 대기 탭에서만) */}
      {activeTab === 'pending' && selectedIds.size > 0 && (
        <div className={cn(
          'flex items-center gap-s4 px-s5 py-s3 rounded-r3 border',
          isDark ? 'bg-white/5 border-white/10' : 'bg-blue-50 border-blue-200'
        )}>
          <span className="text-sm font-medium">
            {selectedIds.size}명 선택됨
          </span>
          <div className="flex gap-s2 ml-auto">
            <Button
              size="sm"
              onClick={handleApproveBulk}
              disabled={isProcessing}
              className="gap-s2"
            >
              {isProcessing ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
              선택 승인
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => openRejectModal(Array.from(selectedIds), true)}
              disabled={isProcessing}
              className="gap-s2 text-destructive hover:text-destructive"
            >
              <XCircle className="w-4 h-4" />
              선택 거절
            </Button>
          </div>
        </div>
      )}

      {/* 테이블 카드 */}
      <div className={cn(
        'rounded-r4 border overflow-hidden',
        isDark ? 'bg-[#1A1A1A] border-white/10' : 'bg-white border-gray-100 shadow-sm'
      )}>
        {isLoading && (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
          </div>
        )}

        {isError && (
          <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
            <p className="text-sm">데이터를 불러오는데 실패했습니다.</p>
            <Button
              variant="outline"
              size="sm"
              className="mt-s4"
              onClick={() => invalidateQueries()}
            >
              다시 시도
            </Button>
          </div>
        )}

        {/* 승인 대기 탭 */}
        {activeTab === 'pending' && !isLoading && !isError && (
          <>
            {associates.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
                <Users className="w-12 h-12 mb-s4 opacity-30" />
                <p className="text-sm">승인 대기 중인 준회원이 없습니다.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className={cn(
                      'text-xs uppercase tracking-widest border-b',
                      isDark ? 'border-white/10' : 'border-gray-100'
                    )}>
                      <th className="pl-s5 pr-s2 py-s4 w-10">
                        <input
                          type="checkbox"
                          checked={isAllSelected}
                          onChange={handleSelectAll}
                          className="rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                        />
                      </th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground">학번</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground">이름</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground hidden md:table-cell">학과</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground hidden lg:table-cell">가입동기</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground">신청일</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground text-right pr-s5">액션</th>
                    </tr>
                  </thead>
                  <tbody className={cn(
                    'divide-y',
                    isDark ? 'divide-white/5' : 'divide-gray-100'
                  )}>
                    {validAssociates.map((associate) => (
                      <tr
                        key={associate.userId}
                        className={cn(
                          'transition-colors',
                          selectedIds.has(associate.userId)
                            ? 'bg-primary/5'
                            : isDark ? 'hover:bg-white/5' : 'hover:bg-gray-50'
                        )}
                      >
                        <td className="pl-s5 pr-s2 py-s4">
                          <input
                            type="checkbox"
                            checked={selectedIds.has(associate.userId)}
                            onChange={() => handleSelectOne(associate.userId)}
                            className="rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                          />
                        </td>
                        <td className="px-s4 py-s4 text-sm font-medium whitespace-nowrap">
                          {associate.studentId}
                        </td>
                        <td className="px-s4 py-s4 text-sm font-bold whitespace-nowrap">
                          {associate.name}
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground whitespace-nowrap hidden md:table-cell">
                          {associate.department}
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground hidden lg:table-cell">
                          <span className="line-clamp-1 max-w-xs" title={associate.motivation}>
                            {associate.motivation}
                          </span>
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground whitespace-nowrap">
                          {formatDate(associate.createdAt)}
                        </td>
                        <td className="px-s4 py-s4 pr-s5 text-right">
                          <div className="flex items-center justify-end gap-s2">
                            <Button
                              size="xs"
                              onClick={() => handleApproveOne(associate)}
                              disabled={isProcessing}
                              className="gap-s1"
                            >
                              <CheckCircle className="w-3.5 h-3.5" />
                              승인
                            </Button>
                            <Button
                              variant="outline"
                              size="xs"
                              onClick={() => openRejectModal([associate.userId], false, associate.name)}
                              disabled={isProcessing}
                              className="gap-s1 text-destructive hover:text-destructive"
                            >
                              <XCircle className="w-3.5 h-3.5" />
                              거절
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}

        {/* 거절됨 탭 */}
        {activeTab === 'rejected' && !isLoading && !isError && (
          <>
            {rejectedList.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-muted-foreground">
                <Users className="w-12 h-12 mb-s4 opacity-30" />
                <p className="text-sm">거절된 준회원이 없습니다.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className={cn(
                      'text-xs uppercase tracking-widest border-b',
                      isDark ? 'border-white/10' : 'border-gray-100'
                    )}>
                      <th className="px-s5 py-s4 font-bold text-muted-foreground">학번</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground">이름</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground hidden md:table-cell">학과</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground">거절 사유</th>
                      <th className="px-s4 py-s4 font-bold text-muted-foreground pr-s5">거절일</th>
                    </tr>
                  </thead>
                  <tbody className={cn(
                    'divide-y',
                    isDark ? 'divide-white/5' : 'divide-gray-100'
                  )}>
                    {rejectedList.map((item) => (
                      <tr
                        key={item.userId}
                        className={cn(
                          'transition-colors',
                          isDark ? 'hover:bg-white/5' : 'hover:bg-gray-50'
                        )}
                      >
                        <td className="px-s5 py-s4 text-sm font-medium whitespace-nowrap">
                          {item.studentId}
                        </td>
                        <td className="px-s4 py-s4 text-sm font-bold whitespace-nowrap">
                          {item.name}
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground whitespace-nowrap hidden md:table-cell">
                          {item.department}
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground">
                          <span className="line-clamp-2 max-w-sm" title={item.rejectionReason}>
                            {item.rejectionReason ?? '-'}
                          </span>
                        </td>
                        <td className="px-s4 py-s4 text-sm text-muted-foreground whitespace-nowrap pr-s5">
                          {formatDate(item.rejectedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>

      {/* 페이지네이션 */}
      {activeTab === 'pending' && pendingTotalPages > 1 && (
        <Pagination
          currentPage={page}
          totalPages={pendingTotalPages}
          onPageChange={handlePageChange}
        />
      )}
      {activeTab === 'rejected' && rejectedTotalPages > 1 && (
        <Pagination
          currentPage={rejectedPage}
          totalPages={rejectedTotalPages}
          onPageChange={handleRejectedPageChange}
        />
      )}

      {/* 거절 사유 모달 */}
      {rejectModal.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={closeRejectModal}
            onKeyDown={(e) => { if (e.key === 'Escape') closeRejectModal(); }}
            role="button"
            tabIndex={0}
            aria-label="모달 닫기"
          />
          <div className={cn(
            'relative z-10 w-full max-w-md mx-s4 rounded-r4 p-s6 shadow-xl',
            isDark ? 'bg-[#1A1A1A] border border-white/10' : 'bg-white'
          )}>
            <div className="flex items-center justify-between mb-s5">
              <h2 className="text-lg font-bold">
                {rejectModal.isBulk
                  ? `${rejectModal.targetIds.length}명 거절`
                  : `${rejectModal.targetName} 거절`}
              </h2>
              <button
                type="button"
                onClick={closeRejectModal}
                className="p-s1 rounded-r2 hover:bg-muted transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-s4">
              <label htmlFor="reject-reason" className="block text-sm font-medium">
                거절 사유 <span className="text-destructive">*</span>
              </label>
              <textarea
                id="reject-reason"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="거절 사유를 입력해주세요"
                maxLength={255}
                rows={4}
                className={cn(
                  'w-full px-s4 py-s3 rounded-r3 border text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary',
                  isDark
                    ? 'bg-white/5 border-white/10 text-white placeholder:text-white/40'
                    : 'bg-white border-gray-200 placeholder:text-gray-400'
                )}
              />
              <p className="text-xs text-muted-foreground text-right">
                {rejectReason.length}/255
              </p>
            </div>
            <div className="flex justify-end gap-s3 mt-s5">
              <Button
                variant="outline"
                size="sm"
                onClick={closeRejectModal}
                disabled={isProcessing}
              >
                취소
              </Button>
              <Button
                size="sm"
                variant="destructive"
                onClick={handleRejectConfirm}
                disabled={!rejectReason.trim() || isProcessing}
                className="gap-s2"
              >
                {isProcessing ? <Loader2 className="w-4 h-4 animate-spin" /> : <XCircle className="w-4 h-4" />}
                거절
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
