import { useState } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/uiStore';
import { Layers, Heart, Bookmark, Award } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import ProfileHeader from '@/components/feature/mypage/ProfileHeader';
import { cn } from '@/lib/utils';

type TabType = 'posts' | 'likes' | 'scraps' | 'events';

export default function MyPage() {
  const { user, logout } = useAuthStore();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [activeTab, setActiveTab] = useState<TabType>('posts');

  const handleChangePassword = () => {
    alert('비밀번호 변경 기능이 곧 추가됩니다.');
  };

  const handleLogout = () => {
    if (confirm('로그아웃 하시겠습니까?')) {
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

  // Mock data for tabs
  const tabCounts = {
    posts: 24,
    likes: 156,
    scraps: 42,
    events: 3,
  };

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {/* Profile Header */}
      <ProfileHeader user={user} onChangePassword={handleChangePassword} onLogout={handleLogout} />

      {/* Tabs */}
      <div className="flex gap-s4 overflow-x-auto pb-2">
        {(['posts', 'likes', 'scraps', 'events'] as TabType[]).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={cn(
              'flex-1 min-w-[120px] text-center px-s6 py-s4 rounded-[2rem] border transition-all cursor-pointer',
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
          'p-s8 rounded-[2.5rem] border min-h-[400px]',
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
          {activeTab === 'posts' && (
            <>
              {[1, 2, 3].map((i) => (
                <div
                  key={i}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  )}
                >
                  <div>
                    <p className="text-c1 text-muted-foreground mb-1 font-bold uppercase">자유게시판 • 2일 전</p>
                    <h4 className="font-bold text-lg">팀을 위한 Figma 워크플로우 최적화 방법</h4>
                  </div>
                  <div className="text-sm font-bold text-primary cursor-pointer">보기</div>
                </div>
              ))}
            </>
          )}

          {activeTab === 'likes' && (
            <>
              {[1, 2, 3, 4].map((i) => (
                <div
                  key={i}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  )}
                >
                  <div className="flex items-center gap-s4">
                    <div className="w-10 h-10 rounded-full bg-destructive/10 flex items-center justify-center text-destructive">
                      <Heart size={18} className="fill-current" />
                    </div>
                    <div>
                      <p className="text-c1 text-muted-foreground mb-1 font-bold uppercase">좋아요한 게시글</p>
                      <h4 className="font-bold text-lg">주간 디자인 영감 #42</h4>
                      <p className="text-c1 text-muted-foreground">작성자: 홍길동</p>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}

          {activeTab === 'scraps' && (
            <>
              {[1, 2].map((i) => (
                <div
                  key={i}
                  className={cn(
                    'p-s6 rounded-r4 border flex justify-between items-center transition-all hover:scale-[1.01] cursor-pointer',
                    isDark ? 'bg-white/5 border-border' : 'bg-muted border-border'
                  )}
                >
                  <div>
                    <div className="flex items-center gap-s2 mb-1">
                      <Bookmark size={14} className="text-primary fill-current" />
                      <p className="text-c1 text-primary font-bold">스크랩한 자료</p>
                    </div>
                    <h4 className="font-bold text-lg mb-1">2024 React 라이브러리 종합 가이드</h4>
                    <p className="text-c1 text-muted-foreground">작성자: 김철수 • 5월 20일 저장</p>
                  </div>
                </div>
              ))}
            </>
          )}

          {activeTab === 'events' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-s4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="p-s6 rounded-r4 border border-primary/30 bg-primary/5">
                  <div className="flex justify-between items-center">
                    <div>
                      <h4 className="font-bold text-lg">여름 네트워킹 모임</h4>
                      <p className="text-c1 text-primary mt-1 font-bold">상태: 참가 확정</p>
                      <p className="text-c1 text-muted-foreground mt-2">2024년 6월 15일 • 디자인홀 B1</p>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="text-c1 text-muted-foreground hover:text-destructive bg-white/50 dark:bg-black/20 rounded-r2"
                    >
                      취소
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>
    </div>
  );
}
