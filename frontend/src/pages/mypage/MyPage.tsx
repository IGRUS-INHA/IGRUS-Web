import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/uiStore';
import { Layers, Heart, Bookmark, Award, Eye, ThumbsUp, RefreshCw, Loader2 } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import ProfileHeader from '@/components/feature/mypage/ProfileHeader';
import { cn } from '@/lib/utils';
import { useMyProfile, useMyPosts, useMyLikes, useMyBookmarks, useMyRegistrations } from '@/hooks/queries/useMyPage';
import { useUpdateMyProfile, getGetMyProfileQueryKey } from '@/api/model/my-page/my-page';
import { hasErrorCode, getErrorMessage } from '@/utils/error';
import { formatRelativeTime, formatDate } from '@/utils';
import type { UpdateProfileRequest } from '@/api/model/models/updateProfileRequest';
import type { MyRegistrationResponseStatus } from '@/api/model/models/myRegistrationResponseStatus';

type TabType = 'posts' | 'likes' | 'scraps' | 'events';

const REGISTRATION_STATUS_LABELS: Record<string, string> = {
  REGISTERED: '신청 완료',
  WAITING: '대기중',
  APPROVED: '승인됨',
  REJECTED: '거절됨',
  CANCELED: '취소됨',
};

function getRegistrationStatusColor(status?: MyRegistrationResponseStatus) {
  switch (status) {
    case 'APPROVED':
    case 'REGISTERED':
      return 'text-primary';
    case 'WAITING':
      return 'text-yellow-600 dark:text-yellow-400';
    case 'REJECTED':
    case 'CANCELED':
      return 'text-destructive';
    default:
      return 'text-muted-foreground';
  }
}

export default function MyPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [activeTab, setActiveTab] = useState<TabType>('posts');

  const queryClient = useQueryClient();

  // API 조회
  const profile = useMyProfile();
  const postsQuery = useMyPosts({ page: 0, size: 10 });
  const likesQuery = useMyLikes({ page: 0, size: 10 });
  const bookmarksQuery = useMyBookmarks({ page: 0, size: 10 });
  const registrationsQuery = useMyRegistrations();

  // 프로필 수정 mutation
  const { mutateAsync: updateProfile, isPending: isProfileUpdating } = useUpdateMyProfile({
    mutation: {
      onSuccess: () => {
        void queryClient.invalidateQueries({ queryKey: getGetMyProfileQueryKey() });
        void Swal.fire({ icon: 'success', title: '수정 완료', text: '프로필이 수정되었습니다.', timer: 1500, showConfirmButton: false, showClass: { popup: '', backdrop: '' }, hideClass: { popup: '', backdrop: '' } });
      },
      onError: (error: unknown) => {
        let errorMessage = getErrorMessage(error);
        if (hasErrorCode(error, 'DUPLICATE_EMAIL')) {
          errorMessage = '이미 사용 중인 이메일입니다.';
        } else if (hasErrorCode(error, 'DUPLICATE_PHONE_NUMBER')) {
          errorMessage = '이미 사용 중인 전화번호입니다.';
        }
        void Swal.fire({ icon: 'error', title: '수정 실패', text: errorMessage, showClass: { popup: '', backdrop: '' }, hideClass: { popup: '', backdrop: '' } });
      },
    },
  });

  const handleUpdateProfile = async (data: UpdateProfileRequest) => {
    await updateProfile({ data });
  };

  const handleChangePassword = () => {
    navigate('/mypage/change-password');
  };

  const handleLogout = async () => {
    const result = await Swal.fire({
      icon: 'question',
      title: '로그아웃',
      text: '로그아웃 하시겠습니까?',
      showCancelButton: true,
      confirmButtonText: '로그아웃',
      cancelButtonText: '취소',
      confirmButtonColor: '#DC3545',
      cancelButtonColor: '#6C757D',
      showClass: { popup: '', backdrop: '' },
      hideClass: { popup: '', backdrop: '' },
    });

    if (result.isConfirmed) {
      logout();
    }
  };

  if (!user) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <p className="text-muted-foreground">로그인이 필요합니다.</p>
      </div>
    );
  }

  // 탭별 카운트 (로딩 중이면 '-' 표시)
  const tabCounts = {
    posts: postsQuery.isLoading ? '-' : postsQuery.totalElements,
    likes: likesQuery.isLoading ? '-' : likesQuery.totalElements,
    scraps: bookmarksQuery.isLoading ? '-' : bookmarksQuery.totalElements,
    events: registrationsQuery.isLoading ? '-' : registrationsQuery.registrations.length,
  };

  // 프로필 데이터 (API 응답 우선, fallback으로 auth store)
  const profileData = profile.data?.status === 200 ? profile.data.data : undefined;

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {/* Profile Header */}
      <ProfileHeader
        user={user}
        profile={profileData}
        onChangePassword={handleChangePassword}
        onLogout={handleLogout}
        onWithdraw={() => navigate('/mypage/withdraw')}
        onUpdateProfile={handleUpdateProfile}
        isUpdating={isProfileUpdating}
      />

      {/* Tabs */}
      <div className="flex gap-s4 overflow-x-auto pb-s2">
        {(['posts', 'likes', 'scraps', 'events'] as TabType[]).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={cn(
              'flex-1 min-w-[120px] text-center px-s6 py-s4 rounded-r4 border transition-all cursor-pointer',
              activeTab === tab
                ? 'bg-primary/10 border-primary'
                : 'bg-white/5 border-border hover:border-primary/30'
            )}
          >
            <div className={cn('text-2xl font-bold', activeTab === tab ? 'text-primary' : 'text-muted-foreground')}>
              {tabCounts[tab]}
            </div>
            <div
              className={cn(
                'text-c2 uppercase font-bold tracking-widest',
                activeTab === tab ? 'text-primary' : 'text-muted-foreground'
              )}
            >
              {tab === 'posts' ? '게시글' : tab === 'likes' ? '좋아요' : tab === 'scraps' ? '스크랩' : '행사'}
            </div>
          </button>
        ))}
      </div>

      {/* Main Content Area */}
      <Card
        className={cn(
          'p-s6 rounded-r4 border min-h-[400px]',
          isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'
        )}
      >
        <h3 className="text-xl font-bold mb-s6 flex items-center gap-s2 capitalize">
          {activeTab === 'posts' && <Layers size={20} className="text-primary" />}
          {activeTab === 'likes' && <Heart size={20} className="text-primary" />}
          {activeTab === 'scraps' && <Bookmark size={20} className="text-primary" />}
          {activeTab === 'events' && <Award size={20} className="text-primary" />}
          {activeTab === 'events' ? '신청한 행사' : `${activeTab === 'posts' ? '게시글' : activeTab === 'likes' ? '좋아요' : '스크랩'} 내역`}
        </h3>

        <div className="space-y-s4">
          {/* 게시글 탭 */}
          {activeTab === 'posts' && (
            <TabContent
              isLoading={postsQuery.isLoading}
              isError={postsQuery.isError}
              isEmpty={postsQuery.posts.length === 0}
              emptyMessage="작성한 게시글이 없습니다."
              onRetry={() => void postsQuery.refetch()}
            >
              {postsQuery.posts.map((post) => (
                <div
                  key={post.id}
                  onClick={() => post.boardCode && post.id && navigate(`/boards/${post.boardCode}/posts/${post.id}`)}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  )}
                >
                  <div className="min-w-0 flex-1">
                    <p className="text-c1 text-muted-foreground mb-s1 font-bold">
                      {post.boardName ?? '게시판'} {post.createdAt ? `\u2022 ${formatRelativeTime(post.createdAt)}` : ''}
                    </p>
                    <h4 className="font-bold text-lg truncate">{post.title ?? '제목 없음'}</h4>
                    <div className="flex items-center gap-s4 mt-s2 text-c1 text-muted-foreground">
                      <span className="flex items-center gap-1"><Eye size={12} /> {post.viewCount ?? 0}</span>
                      <span className="flex items-center gap-1"><ThumbsUp size={12} /> {post.likeCount ?? 0}</span>
                    </div>
                  </div>
                  <div className="text-sm font-bold text-primary cursor-pointer ml-s4 shrink-0">보기</div>
                </div>
              ))}
            </TabContent>
          )}

          {/* 좋아요 탭 */}
          {activeTab === 'likes' && (
            <TabContent
              isLoading={likesQuery.isLoading}
              isError={likesQuery.isError}
              isEmpty={likesQuery.posts.length === 0}
              emptyMessage="좋아요한 게시글이 없습니다."
              onRetry={() => void likesQuery.refetch()}
            >
              {likesQuery.posts.map((post) => (
                <div
                  key={post.postId}
                  onClick={() => post.boardCode && post.postId && navigate(`/boards/${post.boardCode}/posts/${post.postId}`)}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border',
                    post.isDeleted && 'opacity-50'
                  )}
                >
                  <div className="flex items-center gap-s4 min-w-0">
                    <div className="w-10 h-10 rounded-r4 bg-destructive/10 flex items-center justify-center text-destructive shrink-0">
                      <Heart size={18} className="fill-current" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-c1 text-muted-foreground mb-s1 font-bold">
                        {post.boardName ?? '게시판'} {post.createdAt ? `\u2022 ${formatRelativeTime(post.createdAt)}` : ''}
                      </p>
                      <h4 className="font-bold text-lg truncate">
                        {post.isDeleted ? (post.deletedMessage ?? '삭제된 게시글') : (post.title ?? '제목 없음')}
                      </h4>
                      <p className="text-c1 text-muted-foreground">
                        작성자: {post.authorName ?? '알 수 없음'}
                        {post.likeCount !== undefined && ` \u2022 좋아요 ${post.likeCount}`}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </TabContent>
          )}

          {/* 스크랩 탭 */}
          {activeTab === 'scraps' && (
            <TabContent
              isLoading={bookmarksQuery.isLoading}
              isError={bookmarksQuery.isError}
              isEmpty={bookmarksQuery.posts.length === 0}
              emptyMessage="스크랩한 게시글이 없습니다."
              onRetry={() => void bookmarksQuery.refetch()}
            >
              {bookmarksQuery.posts.map((post) => (
                <div
                  key={post.postId}
                  onClick={() => post.boardCode && post.postId && navigate(`/boards/${post.boardCode}/posts/${post.postId}`)}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border',
                    post.isDeleted && 'opacity-50'
                  )}
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-s2 mb-s1">
                      <Bookmark size={14} className="text-primary fill-current" />
                      <p className="text-c1 text-primary font-bold">{post.boardName ?? '스크랩한 자료'}</p>
                    </div>
                    <h4 className="font-bold text-lg mb-s1 truncate">
                      {post.isDeleted ? (post.deletedMessage ?? '삭제된 게시글') : (post.title ?? '제목 없음')}
                    </h4>
                    <p className="text-c1 text-muted-foreground">
                      작성자: {post.authorName ?? '알 수 없음'}
                      {post.createdAt ? ` \u2022 ${formatDate(post.createdAt)}` : ''}
                    </p>
                  </div>
                </div>
              ))}
            </TabContent>
          )}

          {/* 행사 탭 */}
          {activeTab === 'events' && (
            <TabContent
              isLoading={registrationsQuery.isLoading}
              isError={registrationsQuery.isError}
              isEmpty={registrationsQuery.registrations.length === 0}
              emptyMessage="신청한 행사가 없습니다."
              onRetry={() => void registrationsQuery.refetch()}
            >
              <div className="grid grid-cols-1 md:grid-cols-2 gap-s4">
                {registrationsQuery.registrations.map((reg) => (
                  <div
                    key={reg.registrationId}
                    onClick={() => reg.eventId && navigate(`/events/${reg.eventId}`)}
                    className="p-s6 rounded-r4 border border-primary/30 bg-primary/5 cursor-pointer transition-all hover:scale-[1.01]"
                  >
                    <div>
                      <h4 className="font-bold text-lg">{reg.eventTitle ?? '행사명 없음'}</h4>
                      <p className={cn('text-c1 mt-s1 font-bold', getRegistrationStatusColor(reg.status))}>
                        상태: {reg.status ? (REGISTRATION_STATUS_LABELS[reg.status] ?? reg.status) : '-'}
                      </p>
                      <p className="text-c1 text-muted-foreground mt-s2">
                        {reg.eventStartAt ? formatDate(reg.eventStartAt) : '일정 미정'}
                        {reg.registeredAt ? ` \u2022 신청일 ${formatDate(reg.registeredAt)}` : ''}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </TabContent>
          )}
        </div>
      </Card>
    </div>
  );
}

// 탭 컨텐츠 래퍼 (로딩/에러/빈 상태 처리)
function TabContent({
  isLoading,
  isError,
  isEmpty,
  emptyMessage,
  onRetry,
  children,
}: {
  isLoading: boolean;
  isError: boolean;
  isEmpty: boolean;
  emptyMessage: string;
  onRetry: () => void;
  children: React.ReactNode;
}) {
  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
        <Loader2 size={32} className="animate-spin mb-s4" />
        <p className="text-b2">데이터를 불러오는 중...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
        <p className="text-b2 mb-s4">데이터를 불러올 수 없습니다.</p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onRetry}
          className="flex items-center gap-s2"
        >
          <RefreshCw size={14} /> 다시 시도
        </Button>
      </div>
    );
  }

  if (isEmpty) {
    return (
      <div className="flex items-center justify-center py-16 text-muted-foreground">
        <p className="text-b2">{emptyMessage}</p>
      </div>
    );
  }

  return <>{children}</>;
}
