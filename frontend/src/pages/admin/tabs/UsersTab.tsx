import { Fragment, useState } from 'react';
import { Search, ChevronDown, ChevronRight } from 'lucide-react';
import { useGetUserList, useChangeUserRole, useGetUserDetail } from '@/api/model/admin-user-management/admin-user-management';
import type { GetUserListRole } from '@/api/model/models/getUserListRole';
import type { GetUserListStatus } from '@/api/model/models/getUserListStatus';
import type { ChangeUserRoleRequestRole } from '@/api/model/models/changeUserRoleRequestRole';
import type { UserListResponse } from '@/api/model/models/userListResponse';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Pagination } from '@/components/board/Pagination';
import { useAuth } from '@/hooks';
import { useUIStore } from '@/stores';
import { cn } from '@/lib/utils';
import { useQueryClient } from '@tanstack/react-query';

const ROLE_OPTIONS: { value: GetUserListRole | ''; label: string }[] = [
  { value: '', label: '전체 역할' },
  { value: 'ADMIN', label: '관리자' },
  { value: 'OPERATOR', label: '운영진' },
  { value: 'MEMBER', label: '정회원' },
  { value: 'ASSOCIATE', label: '준회원' },
];

const STATUS_OPTIONS: { value: GetUserListStatus | ''; label: string }[] = [
  { value: '', label: '전체 상태' },
  { value: 'ACTIVE', label: '활성' },
  { value: 'SUSPENDED', label: '정지' },
  { value: 'WITHDRAWN', label: '탈퇴' },
];

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '관리자',
  OPERATOR: '운영진',
  MEMBER: '정회원',
  ASSOCIATE: '준회원',
};

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'bg-success/10 text-success',
  SUSPENDED: 'bg-destructive/10 text-destructive',
  WITHDRAWN: 'bg-muted text-muted-foreground',
};

function UserDetailRow({ userId, colSpan }: { userId: number; colSpan: number }) {
  const { data: response, isLoading } = useGetUserDetail(userId);
  const detail = response?.status === 200 ? response.data : undefined;

  if (isLoading) {
    return (
      <tr>
        <td colSpan={colSpan} className="py-s3 px-s4 bg-muted/30 text-center text-b2 text-muted-foreground">
          로딩 중...
        </td>
      </tr>
    );
  }

  if (!detail) return null;

  return (
    <tr>
      <td colSpan={colSpan} className="py-s3 px-s4 bg-muted/30">
        <div className="flex gap-s6 text-b2">
          <div>
            <span className="text-muted-foreground">학과: </span>
            <span className="font-medium">{detail.department || '-'}</span>
          </div>
          <div>
            <span className="text-muted-foreground">전화번호: </span>
            <span className="font-medium">{detail.phoneNumber || '-'}</span>
          </div>
          <div>
            <span className="text-muted-foreground">학년: </span>
            <span className="font-medium">{detail.grade ? `${detail.grade}학년` : '-'}</span>
          </div>
          <div>
            <span className="text-muted-foreground">성별: </span>
            <span className="font-medium">{detail.gender === 'MALE' ? '남' : detail.gender === 'FEMALE' ? '여' : '-'}</span>
          </div>
        </div>
      </td>
    </tr>
  );
}

export default function UsersTab() {
  const { user: currentUser } = useAuth();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const [keyword, setKeyword] = useState('');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<GetUserListRole | ''>('');
  const [statusFilter, setStatusFilter] = useState<GetUserListStatus | ''>('');
  const [page, setPage] = useState(1);
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [selectedRole, setSelectedRole] = useState<ChangeUserRoleRequestRole | ''>('');
  const [expandedUserIds, setExpandedUserIds] = useState<Set<number>>(new Set());

  const { data: response, isLoading } = useGetUserList({
    ...(searchKeyword && { keyword: searchKeyword }),
    ...(roleFilter && { role: roleFilter }),
    ...(statusFilter && { status: statusFilter }),
    page: page - 1,
    size: 20,
  });

  const { mutate: changeRole, isPending: isChanging } = useChangeUserRole({
    mutation: {
      onSuccess: () => {
        addToast({ type: 'success', title: '권한 변경 완료', message: '회원 권한이 변경되었습니다.' });
        setEditingUserId(null);
        setSelectedRole('');
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/users'] });
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/associates/pending'] });
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/associates/rejected'] });
        queryClient.invalidateQueries({ queryKey: ['/api/v1/admin/dashboard'] });
      },
      onError: () => {
        addToast({ type: 'error', title: '권한 변경 실패', message: '권한 변경 중 오류가 발생했습니다.' });
      },
    },
  });

  const data = response?.status === 200 ? response.data : undefined;
  const users = data?.users ?? [];
  const totalPages = data?.totalPages ?? 0;
  const isAdmin = currentUser?.role === 'ADMIN';

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchKeyword(keyword);
    setPage(1);
  };

  const handleRoleChange = (userId: number) => {
    if (!selectedRole) return;
    changeRole({ userId, data: { role: selectedRole } });
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
                placeholder="이름 또는 학번 검색"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="pl-9"
              />
            </div>
            <Button type="submit">검색</Button>
          </form>
          <div className="flex gap-s2">
            <select
              value={roleFilter}
              onChange={(e) => { setRoleFilter(e.target.value as GetUserListRole | ''); setPage(1); }}
              className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
            >
              {ROLE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as GetUserListStatus | ''); setPage(1); }}
              className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
            >
              {STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
        </div>
      </Card>

      {/* Table */}
      <Card className="p-s5 overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="text-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
              <th className="pb-s4 font-bold w-8"></th>
              <th className="pb-s4 font-bold">학번</th>
              <th className="pb-s4 font-bold">이름</th>
              <th className="pb-s4 font-bold hidden lg:table-cell">이메일</th>
              <th className="pb-s4 font-bold">역할</th>
              <th className="pb-s4 font-bold">상태</th>
              <th className="pb-s4 font-bold hidden lg:table-cell">가입일</th>
              {isAdmin && <th className="pb-s4 font-bold text-right">작업</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {users.map((u) => {
              const isExpanded = expandedUserIds.has(u.userId!);
              const colSpan = isAdmin ? 8 : 7;
              return (
                <Fragment key={u.userId}>
                  <tr
                    className="group cursor-pointer hover:bg-muted/20"
                    onClick={() => setExpandedUserIds((prev) => {
                      const next = new Set(prev);
                      isExpanded ? next.delete(u.userId!) : next.add(u.userId!);
                      return next;
                    })}
                  >
                    <td className="py-s4 text-muted-foreground">
                      {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </td>
                    <td className="py-s4 text-b2 font-medium">{u.studentId}</td>
                    <td className="py-s4 text-b2 font-bold">{u.name}</td>
                    <td className="py-s4 text-b2 text-muted-foreground hidden lg:table-cell">{u.email}</td>
                    <td className="py-s4 text-b2">{ROLE_LABELS[u.role ?? ''] ?? u.role}</td>
                    <td className="py-s4">
                      <span className={cn('px-2 py-1 rounded-r2 text-c2 font-bold', STATUS_BADGE[u.status ?? ''] ?? 'bg-muted text-muted-foreground')}>
                        {u.status === 'ACTIVE' ? '활성' : u.status === 'SUSPENDED' ? '정지' : u.status === 'WITHDRAWN' ? '탈퇴' : u.status}
                      </span>
                    </td>
                    <td className="py-s4 text-b2 text-muted-foreground hidden lg:table-cell">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('ko-KR') : '-'}
                    </td>
                    {isAdmin && (
                      <td className="py-s4 text-right" onClick={(e) => e.stopPropagation()}>
                        {editingUserId === u.userId ? (
                          <div className="flex items-center gap-s2 justify-end">
                            <select
                              value={selectedRole}
                              onChange={(e) => setSelectedRole(e.target.value as ChangeUserRoleRequestRole)}
                              className="px-s2 py-1 rounded-r2 border border-border bg-background text-sm"
                            >
                              <option value="">선택</option>
                              {ROLE_OPTIONS.filter((o) => o.value && o.value !== u.role).map((o) => (
                                <option key={o.value} value={o.value}>{o.label}</option>
                              ))}
                            </select>
                            <Button
                              size="xs"
                              onClick={() => handleRoleChange(u.userId!)}
                              disabled={!selectedRole || isChanging}
                            >
                              확인
                            </Button>
                            <Button
                              size="xs"
                              variant="outline"
                              onClick={() => { setEditingUserId(null); setSelectedRole(''); }}
                            >
                              취소
                            </Button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className="text-primary hover:underline text-c1 font-bold cursor-pointer"
                            onClick={() => setEditingUserId(u.userId!)}
                          >
                            권한 변경
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                  {isExpanded && <UserDetailRow userId={u.userId!} colSpan={colSpan} />}
                </Fragment>
              );
            })}
          </tbody>
        </table>

        {users.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">회원이 없습니다.</div>
        )}
      </Card>

      <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
