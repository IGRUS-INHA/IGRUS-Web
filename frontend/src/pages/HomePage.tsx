import { Link } from 'react-router-dom';
import { Sparkles, ArrowRight } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useUIStore } from '@/stores/uiStore';
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

  return (
    <div className="animate-in fade-in duration-500">
      {/* Hero Section */}
      <section
        className={cn(
          'relative w-full h-96 rounded-r4 border py-s8 px-s7 flex flex-col justify-center overflow-hidden group transition-all duration-300',
          isDark
            ? 'bg-gradient-to-br from-[#1E1E1E] to-[#121212] border-white/5'
            : 'bg-gradient-to-br from-[#F3F4F6] to-white border-gray-100'
        )}
      >
        <div className="relative z-10 space-y-s4">
          <div
            className={cn(
              'inline-flex items-center gap-s2 px-s3 py-s1 border rounded-r4 text-[10px] uppercase tracking-widest transition-colors',
              isDark
                ? 'bg-white/5 border-white/10 text-gray-400'
                : 'bg-gray-100 border-gray-200 text-gray-500'
            )}
          >
            <Sparkles size={12} className="text-[#03A69E]" />
            혁신적인 개발
          </div>
          <h2
            className={cn(
              'text-5xl font-bold leading-tight transition-colors',
              isDark ? 'text-white' : 'text-black'
            )}
          >
            웹을 개발하고,
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#03A69E] to-[#027d77]">
              미래를 만드세요.
            </span>
          </h2>
          <p
            className={cn(
              'max-w-lg transition-colors',
              isDark ? 'text-gray-400' : 'text-gray-600'
            )}
          >
            인하대학교 웹 개발 동아리 IGRUS에서 최신 기술로 프로젝트를
            구현하세요. 함께 성장하는 개발자들의 커뮤니티입니다.
          </p>
          <Button
            asChild
            className={cn(
              'flex items-center gap-s2 group/btn px-s6 py-s3 rounded-r4 font-semibold text-sm transition-all w-fit',
              isDark
                ? 'bg-white text-black hover:bg-primary hover:text-white'
                : 'bg-black text-white hover:bg-primary'
            )}
          >
            <Link to="/board/general">
              커뮤니티 둘러보기{' '}
              <ArrowRight
                size={16}
                className="group-hover/btn:translate-x-1 transition-transform"
              />
            </Link>
          </Button>
        </div>

        {/* Decorative Elements */}
        <div className="absolute top-[-20%] right-[-10%] w-[500px] h-[500px] blur-[120px] rounded-r4 transition-colors duration-1000 bg-[#03A69E]/20 group-hover:bg-primary/30" />
        <div className="absolute right-20 top-1/2 -translate-y-1/2 opacity-20 select-none pointer-events-none">
          <div
            className={cn(
              'w-48 h-48 border rounded-r4 rotate-12 flex items-center justify-center',
              isDark ? 'border-white/10' : 'border-black/5'
            )}
          >
            <div
              className={cn(
                'w-32 h-32 border rounded-r4 animate-pulse',
                isDark ? 'border-[#03A69E]/20' : 'border-[#03A69E]/10'
              )}
            />
          </div>
        </div>
      </section>

      {/* Featured Section */}
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
