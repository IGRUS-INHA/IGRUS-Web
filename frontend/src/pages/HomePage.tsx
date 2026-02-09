import { Link } from 'react-router-dom';
import { ArrowRight, UserPlus, MessageCircle, Megaphone } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useUIStore } from '@/stores/uiStore';
import { useAuth } from '@/hooks';
import { cn } from '@/lib/utils';
import type { Post } from '@/types/entities';

// Featured posts data (임시 데이터 - 추후 API로 대체)
const FEATURED_POSTS: Post[] = [
  {
    id: '1',
    board: 'notices',
    category: '공지',
    title: '2024 봄학기 신입회원 모집',
    author: '운영진',
    content: '인하대학교 IGRUS 동아리에서 새로운 멤버를 모집합니다.',
    date: '2시간 전',
    image:
      'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&q=80&w=800',
    isAnonymous: false,
    isQuestion: false,
    likes: 120,
    comments: 45,
  },
  {
    id: '2',
    board: 'general',
    category: '활동',
    title: '게임 개발 프로젝트 전시회',
    author: 'IGRUS',
    content: '우리 동아리의 최고의 게임 개발 프로젝트를 만나보세요.',
    date: '1일 전',
    image:
      'https://images.unsplash.com/photo-1547826039-bfc35e0f1ea8?auto=format&fit=crop&q=80&w=800',
    isAnonymous: false,
    isQuestion: false,
    likes: 85,
    comments: 12,
  },
  {
    id: '3',
    board: 'general',
    category: '행사',
    title: '게임 업계 선배와의 네트워킹 나이트',
    author: '행사팀',
    content: '게임 업계 선배들과 함께하는 특별한 밤.',
    date: '3일 전',
    tag: 'D-2',
    image:
      'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&q=80&w=800',
    isAnonymous: false,
    isQuestion: false,
    likes: 210,
    comments: 38,
  },
];

export default function HomePage() {
  const theme = useUIStore((state) => state.theme);
  const isDark = theme === 'dark';
  const { isAuthenticated } = useAuth();

  return (
    <div className="animate-in fade-in duration-500">
      {/* Hero Section */}
      <section
        className={cn(
          'relative w-full min-h-[480px] rounded-r4 overflow-hidden transition-all duration-500 hero-accent-line',
          isDark ? 'hero-clean-dark' : 'hero-clean-light'
        )}
      >
        {/* Dot Grid Pattern */}
        <div className="hero-dot-grid" />

        {/* Decorative Elements */}
        <div
          className="hero-deco-ring"
          style={{ top: '15%', right: '12%', width: 160, height: 160 }}
        />
        <div
          className="hero-deco-ring-2"
          style={{ top: '65%', right: '8%', width: 72, height: 72 }}
        />
        <div
          className="hero-glow-orb"
          style={{ top: '10%', right: '5%', width: 200, height: 200 }}
        />
        <img
          src="/igruslogo.png"
          alt=""
          className="hero-logo-deco"
          style={{ bottom: -48, left: -100, top: 'auto', right: 'auto', width: 320, height: 320, opacity: 0.12 }}
        />

        {/* Content */}
        <div className="relative z-10 flex flex-col justify-center min-h-[480px] px-s5 md:px-s8 py-s6 md:py-s8">
          <div className="space-y-6 max-w-2xl">
            {/* Badge */}
            <div
              className={cn(
                'inline-flex items-center px-s3 py-s1 rounded-full text-[11px] font-mono tracking-wider transition-colors hero-badge-glow',
                isDark
                  ? 'bg-[#03A69E]/10 border border-[#03A69E]/20 text-[#66CBC5]'
                  : 'bg-[#03A69E]/5 border border-[#03A69E]/12 text-[#03A69E]'
              )}
            >
              &lt;IGRUS /&gt;
            </div>

            {/* Heading */}
            <h2 className="hero-heading hero-text-glow">
              <span
                className={cn(
                  'block transition-colors',
                  isDark ? 'text-white' : 'text-gray-8'
                )}
              >
                성장과 낭만의 동아리,
              </span>
              <span className="block font-black text-transparent bg-clip-text bg-gradient-to-r from-[#03A69E] via-[#0891b2] to-[#03A69E] hero-gradient-text">
                IGRUS.
              </span>
            </h2>

            {/* Description */}
            <p
              className={cn(
                'text-b1 max-w-lg leading-relaxed transition-colors',
                isDark ? 'text-[#9CA3AF]' : 'text-gray-500'
              )}
            >
              인하대학교 웹 개발 동아리 IGRUS에서 최신 기술로 프로젝트를
              구현하세요. 함께 성장하는 개발자들의 커뮤니티입니다.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-wrap items-center gap-s3 pt-s6">
              {isAuthenticated ? (
                <Button
                  asChild
                  className="flex items-center gap-s2 group/btn px-s6 py-s3 rounded-full font-semibold text-sm transition-all bg-[#03A69E] text-white hover:bg-[#029890] hero-btn-glow"
                >
                  <Link to={__FEATURE_COMMUNITY__ ? "/board/general" : "/board/notices"}>
                    {__FEATURE_COMMUNITY__ ? <MessageCircle size={16} /> : <Megaphone size={16} />}
                    {__FEATURE_COMMUNITY__ ? '커뮤니티 둘러보기' : '공지사항 보기'}
                    <ArrowRight
                      size={16}
                      className="group-hover/btn:translate-x-1 transition-transform"
                    />
                  </Link>
                </Button>
              ) : (
                <Button
                  asChild
                  className="flex items-center gap-s2 group/btn px-s6 py-s3 rounded-full font-semibold text-sm transition-all bg-[#03A69E] text-white hover:bg-[#029890] hero-btn-glow"
                >
                  <Link to="/signup">
                    <UserPlus size={16} />
                    가입하기
                    <ArrowRight
                      size={16}
                      className="group-hover/btn:translate-x-1 transition-transform"
                    />
                  </Link>
                </Button>
              )}
            </div>

          </div>
        </div>
      </section>

      {/* Featured Section */}
      {__FEATURE_COMMUNITY__ && (
        <section className="mt-s6">
          <div className="flex justify-between items-center mb-s6">
            <div>
              <h3
                className={cn(
                  'text-2xl font-bold transition-colors',
                  isDark ? 'text-white' : 'text-black'
                )}
              >
                주요 게시글
              </h3>
              <p className="text-gray-500 text-sm">
                엄선된 이야기와 소식을 확인하세요.
              </p>
            </div>
            <Link
              to="/board/general"
              className="text-sm text-gray-400 hover:text-[#03A69E] transition"
            >
              전체 게시글 보기
            </Link>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-s7 items-stretch">
            {FEATURED_POSTS.map((post) => (
              <PostCard key={post.id} post={post} theme={theme} />
            ))}
          </div>
        </section>
      )}

    </div>
  );
}

// PostCard Component
interface PostCardProps {
  post: Post;
  theme: 'light' | 'dark';
}

function PostCard({ post, theme }: PostCardProps) {
  const isDark = theme === 'dark';

  return (
    <Link to={`/board/${post.board}/${post.id}`} className="h-full">
      <Card
        className={cn(
          'h-full flex flex-col overflow-hidden cursor-pointer transition-all duration-300 hover:scale-[1.02]',
          isDark
            ? 'bg-[#1A1A1A] border-white/5 hover:border-[#03A69E]/30'
            : 'bg-white border-gray-100 hover:border-[#03A69E]/30 hover:shadow-lg'
        )}
      >
        {post.image && (
          <div className="relative h-64 overflow-hidden">
            <img
              src={post.image}
              alt={post.title}
              className="w-full h-full object-cover transition-transform duration-300 hover:scale-110"
            />
            {post.tag && (
              <div className="absolute top-s4 right-s4 bg-primary text-white px-s3 py-s1 rounded-r4 text-xs font-bold">
                {post.tag}
              </div>
            )}
          </div>
        )}
        <CardContent className="p-s4 flex-1 flex flex-col">
          <div className="space-y-s4 flex-1 flex flex-col">
            <div
              className={cn(
                'text-xs font-bold uppercase tracking-widest',
                isDark ? 'text-gray-400' : 'text-gray-500'
              )}
            >
              {post.category}
            </div>
            <h3
              className={cn(
                'text-2xl font-bold line-clamp-2 transition-colors',
                isDark ? 'text-white' : 'text-black'
              )}
            >
              {post.title}
            </h3>
            <p
              className={cn(
                'text-sm line-clamp-2 transition-colors',
                isDark ? 'text-gray-400' : 'text-gray-600'
              )}
            >
              {post.content}
            </p>
            <div className="flex items-center justify-between pt-s2">
              <div className="flex items-center gap-s4 text-xs text-gray-500">
                <span>👍 {post.likes}</span>
                <span>💬 {post.comments}</span>
              </div>
              <span className="text-xs text-gray-500">{post.date}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
