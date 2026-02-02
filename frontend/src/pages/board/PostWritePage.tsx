import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Image as ImageIcon, Paperclip } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useCreatePost } from '@/api/model/post/post';
import { useUIStore } from '@/stores';
import { Button } from '@/components/ui/button';
import { BOARD_CATEGORIES, POST_OPTIONS } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';

// Form validation schema
const postSchema = z.object({
  title: z.string().min(1, '제목을 입력해주세요').max(200, '제목은 200자 이내로 입력해주세요'),
  content: z.string().min(1, '내용을 입력해주세요'),
  category: z.string().min(1, '카테고리를 선택해주세요'),
  isAnonymous: z.boolean().optional(),
  isQuestion: z.boolean().optional(),
});

type PostForm = z.infer<typeof postSchema>;

export default function PostWritePage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const validBoardType = boardType as BoardType;

  // Get categories for this board
  const categories = BOARD_CATEGORIES[validBoardType] || BOARD_CATEGORIES.general;

  // Check if anonymous and question posts are allowed
  const allowAnonymous = POST_OPTIONS.ALLOW_ANONYMOUS.includes(validBoardType);
  const allowQuestion = POST_OPTIONS.ALLOW_QUESTION.includes(validBoardType);

  // Form setup
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<PostForm>({
    resolver: zodResolver(postSchema),
    defaultValues: {
      category: categories[0]?.value || 'general',
      isAnonymous: false,
      isQuestion: false,
    },
  });

  // Watch form values
  const title = watch('title');
  const content = watch('content');
  const isAnonymous = watch('isAnonymous');
  const isQuestion = watch('isQuestion');

  // Mutation
  const createPost = useCreatePost();

  // Handlers
  const handleBack = () => {
    navigate(`/board/${validBoardType}`);
  };

  const onSubmit = (data: PostForm) => {
    createPost.mutate(
      {
        boardCode: validBoardType,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isAnonymous !== undefined && { isAnonymous: data.isAnonymous }),
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
        },
      },
      {
        onSuccess: (response) => {
          if (response.status === 201) {
            const postId = response.data.postId;
            if (postId) {
              navigate(`/board/${validBoardType}/${postId}`);
            }
          }
        },
        onError: (error) => {
          alert(`게시글 작성 실패: ${error.message}`);
        },
      }
    );
  };

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      {/* Top Bar */}
      <div className="flex justify-between items-center mb-s8 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
        <button
          onClick={handleBack}
          type="button"
          className={cn(
            'flex items-center gap-s2 text-sm font-bold transition-colors cursor-pointer',
            isDark ? 'text-muted-foreground hover:text-foreground' : 'text-muted-foreground hover:text-foreground'
          )}
        >
          <ArrowLeft size={18} /> 취소
        </button>
        <div className="flex items-center gap-s4">
          <span className="text-xs text-muted-foreground font-medium">
            {title && content ? '저장됨' : '저장 안됨'}
          </span>
          <Button
            onClick={handleSubmit(onSubmit)}
            disabled={isSubmitting}
            type="button"
            className="rounded-full flex items-center gap-s2 shadow-lg"
          >
            <Save size={16} /> 게시하기
          </Button>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)}>
        <div
          className={cn(
            'max-w-4xl mx-auto p-s8 md:p-12 rounded-[2.5rem] border min-h-[80vh] flex flex-col',
            isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'
          )}
        >
          {/* Settings Bar */}
          <div className="flex flex-wrap gap-s4 mb-s8">
            <select
              {...register('category')}
              className={cn(
                'px-s4 py-s2 rounded-r3 text-sm font-bold border focus:outline-none focus:border-primary cursor-pointer',
                isDark ? 'bg-white/5 border-border' : 'bg-muted/50 border-border'
              )}
            >
              {categories.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
            {errors.category && (
              <span className="text-destructive text-xs">{errors.category.message}</span>
            )}

            {allowAnonymous && (
              <button
                onClick={() => setValue('isAnonymous', !isAnonymous)}
                type="button"
                className={cn(
                  'px-s4 py-s2 rounded-r3 text-sm font-bold border transition-all cursor-pointer',
                  isAnonymous
                    ? 'bg-primary/10 border-primary text-primary'
                    : isDark
                      ? 'bg-white/5 border-border text-muted-foreground'
                      : 'bg-muted/50 border-border text-muted-foreground'
                )}
              >
                익명
              </button>
            )}

            {allowQuestion && (
              <button
                onClick={() => setValue('isQuestion', !isQuestion)}
                type="button"
                className={cn(
                  'px-s4 py-s2 rounded-r3 text-sm font-bold border transition-all cursor-pointer',
                  isQuestion
                    ? 'bg-primary/10 border-primary text-primary'
                    : isDark
                      ? 'bg-white/5 border-border text-muted-foreground'
                      : 'bg-muted/50 border-border text-muted-foreground'
                )}
              >
                질문 게시글
              </button>
            )}
          </div>

          {/* Title Input */}
          <div className="mb-s6">
            <input
              {...register('title')}
              type="text"
              placeholder="게시글 제목"
              className={cn(
                'w-full text-4xl font-bold bg-transparent border-none focus:ring-0 placeholder-muted-foreground',
                errors.title && 'border-b-2 border-destructive'
              )}
            />
            {errors.title && (
              <span className="text-destructive text-sm mt-s2 block">{errors.title.message}</span>
            )}
          </div>

          {/* Content Textarea */}
          <div className="flex-1 relative mb-s8">
            <textarea
              {...register('content')}
              placeholder="이야기를 작성하세요... (마크다운 지원)"
              className={cn(
                'w-full h-full bg-transparent border-none focus:ring-0 resize-none text-lg leading-relaxed',
                isDark ? 'text-muted-foreground placeholder-muted-foreground' : 'text-foreground placeholder-muted-foreground',
                errors.content && 'border-2 border-destructive rounded-r2 p-4'
              )}
            />
            {errors.content && (
              <span className="text-destructive text-sm mt-s2 block">{errors.content.message}</span>
            )}
          </div>

          {/* Bottom Toolbar */}
          <div className={cn('mt-s8 pt-s4 border-t flex gap-s4', 'border-border')}>
            <button
              type="button"
              className={cn(
                'p-s2 rounded-r2 transition cursor-pointer',
                isDark ? 'text-muted-foreground hover:bg-white/10' : 'text-muted-foreground hover:bg-muted'
              )}
            >
              <ImageIcon size={20} />
            </button>
            <button
              type="button"
              className={cn(
                'p-s2 rounded-r2 transition cursor-pointer',
                isDark ? 'text-muted-foreground hover:bg-white/10' : 'text-muted-foreground hover:bg-muted'
              )}
            >
              <Paperclip size={20} />
            </button>
            <div className="ml-auto text-xs text-muted-foreground flex items-center">
              {content?.length || 0} 글자
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
