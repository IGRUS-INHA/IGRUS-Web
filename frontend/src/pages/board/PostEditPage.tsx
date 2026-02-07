import { useParams, useNavigate } from 'react-router-dom';
import { FullPageSpinner } from '@/components/ui';
import { ArrowLeft, Save, Image as ImageIcon, Paperclip } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import MDEditor from '@uiw/react-md-editor';
import '@uiw/react-md-editor/markdown-editor.css';
import { useGetPostDetail, useUpdatePost } from '@/api/model/post/post';
import type { PostDetailResponse } from '@/api/model/models';
import { useUIStore } from '@/stores';
import { postFormSchema, type PostFormData } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { isForbiddenError, isUnauthorizedError, isNotFoundError, getErrorMessage } from '@/utils/error';
import { useCurrentBoard } from '@/hooks/useBoards';

export default function PostEditPage() {
  const { boardType, postId } = useParams<{ boardType: BoardType; postId: string }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const queryClient = useQueryClient();

  const validBoardType = boardType as BoardType;
  const numericPostId = Number(postId);

  // Fetch existing post data
  const { data: response, isLoading, error } = useGetPostDetail(
    validBoardType,
    numericPostId,
    {
      query: { enabled: !!(validBoardType && numericPostId) },
    }
  );
  // customFetch가 에러 시 throw하므로 data는 항상 성공 타입
  const post = response?.data as PostDetailResponse | undefined;

  // Get current board info
  const { board, isLoading: boardLoading } = useCurrentBoard();

  // Get board label (fallback to hardcoded)
  const boardLabel = board?.name ?? '게시판';

  // Check if anonymous and question posts are allowed (server-driven)
  const allowAnonymous = board?.allowsAnonymous ?? false;
  const allowQuestion = board?.allowsQuestionTag ?? false;
  const allowVisibleToAssociate = validBoardType === 'notices';

  // Form setup
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<PostFormData>({
    resolver: zodResolver(postFormSchema),
    defaultValues: {
      isAnonymous: false,
      isQuestion: false,
      isVisibleToAssociate: false,
    },
  });

  // Initialize form with fetched data
  useEffect(() => {
    if (post) {
      reset({
        title: post.title || '',
        content: post.content || '',
        isAnonymous: post.isAnonymous || false,
        isQuestion: post.isQuestion || false,
        isVisibleToAssociate: false,
      });
    }
  }, [post, reset]);

  // Watch form values
  const title = watch('title');
  const content = watch('content');
  const isAnonymous = watch('isAnonymous');
  const isQuestion = watch('isQuestion');
  const isVisibleToAssociate = watch('isVisibleToAssociate');

  // Mutation
  const updatePost = useUpdatePost();

  // Handlers
  const handleBack = () => {
    navigate(`/board/${validBoardType}/${postId}`);
  };

  const onSubmit = (data: PostFormData) => {
    updatePost.mutate(
      {
        boardCode: validBoardType,
        postId: numericPostId,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
        },
      },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/boards/${validBoardType}/posts/${numericPostId}`]
          });
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/boards/${validBoardType}/posts`]
          });

          navigate(`/board/${validBoardType}/${postId}`);
        },
        onError: (error: unknown) => {
          let errorMessage = '게시글 수정에 실패했습니다.';

          // 403 Forbidden - 권한 없음
          if (isForbiddenError(error)) {
            errorMessage = '수정 권한이 없습니다.\n\n작성자 본인만 게시글을 수정할 수 있습니다.';
          }
          // 401 Unauthorized - 인증 필요
          else if (isUnauthorizedError(error)) {
            errorMessage = '로그인이 필요합니다.\n로그인 페이지로 이동합니다.';
            alert(errorMessage);
            navigate('/login');
            return;
          }
          // 404 Not Found
          else if (isNotFoundError(error)) {
            errorMessage = '게시글을 찾을 수 없습니다.';
          }
          // 기타 에러
          else {
            errorMessage = getErrorMessage(error);
          }

          alert(errorMessage);
        },
      }
    );
  };

  // Loading state
  if (isLoading || boardLoading) {
    return <FullPageSpinner />;
  }

  // Error state
  if (error || !post) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-s4">
        <p className="text-muted-foreground">게시글을 불러올 수 없습니다.</p>
        <button
          type="button"
          onClick={handleBack}
          className="text-primary hover:underline cursor-pointer"
        >
          뒤로 가기
        </button>
      </div>
    );
  }

  // Permission check
  if (!post.isAuthor) {
    return (
      <div className="flex flex-col items-center justify-center py-12 gap-s4">
        <p className="text-muted-foreground">수정 권한이 없습니다.</p>
        <button
          type="button"
          onClick={handleBack}
          className="text-primary hover:underline cursor-pointer"
        >
          뒤로 가기
        </button>
      </div>
    );
  }

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      {/* Top Bar */}
      <div className="flex justify-between items-center mb-s8 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
        <button
          onClick={handleBack}
          type="button"
          className={cn(
            'flex items-center gap-s2 text-sm font-bold transition-colors cursor-pointer',
            isDark ? 'text-gray-400 hover:text-white' : 'text-gray-500 hover:text-black'
          )}
        >
          <ArrowLeft size={18} /> 취소
        </button>
        <div className="flex items-center gap-s4">
          <span className="text-xs text-gray-500 font-medium">
            {title && content ? '저장됨' : '저장 안됨'}
          </span>
          <button
            onClick={handleSubmit(onSubmit)}
            disabled={isSubmitting}
            type="button"
            className="bg-[#03A69E] text-white px-s5 py-s2 rounded-full text-sm font-bold hover:bg-[#028b84] transition shadow-lg shadow-[#03A69E]/20 flex items-center gap-s2 disabled:opacity-50 cursor-pointer"
          >
            <Save size={16} /> 수정하기
          </button>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)}>
        <div
          className={cn(
            'w-full max-w-[1616px] mx-auto p-s6 md:p-s7 rounded-[2.5rem] border min-h-[80vh] flex flex-col',
            isDark ? 'bg-[#1A1A1A] border-white/5' : 'bg-white border-gray-100 shadow-sm'
          )}
        >
          {/* Settings Bar */}
          <div className="flex flex-wrap gap-s4 mb-s6">
            <div className="px-s2 py-s2 text-xl text-[#03A69E]">
              {validBoardType === 'general' ? boardLabel : `${boardLabel} 게시판`}
            </div>


            {allowAnonymous && (
              <button
                onClick={() => setValue('isAnonymous', !isAnonymous)}
                type="button"
                className={cn(
                  'px-s4 py-s2 rounded-r4 text-sm border transition-all cursor-pointer',
                  isAnonymous
                    ? 'bg-[#03A69E]/10 border-[#03A69E] text-[#03A69E]'
                    : isDark
                      ? 'bg-white/5 border-white/10 text-gray-400'
                      : 'bg-gray-50 border-gray-200 text-gray-500'
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
                  'px-s4 py-s2 rounded-r4 text-sm border transition-all cursor-pointer',
                  isQuestion
                    ? 'bg-[#03A69E]/10 border-[#03A69E] text-[#03A69E]'
                    : isDark
                      ? 'bg-white/5 border-white/10 text-gray-400'
                      : 'bg-gray-50 border-gray-200 text-gray-500'
                )}
              >
                질문
              </button>
            )}

            {allowVisibleToAssociate && (
              <button
                onClick={() => setValue('isVisibleToAssociate', !isVisibleToAssociate)}
                type="button"
                className={cn(
                  'px-s4 py-s2 rounded-r4 text-sm border transition-all cursor-pointer',
                  isVisibleToAssociate
                    ? 'bg-[#03A69E]/10 border-[#03A69E] text-[#03A69E]'
                    : isDark
                      ? 'bg-white/5 border-white/10 text-gray-400'
                      : 'bg-gray-50 border-gray-200 text-gray-500'
                )}
              >
                준회원 공개
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
                'w-full text-4xl font-bold bg-transparent border-none focus:ring-0 focus:outline-none opacity-80',
                isDark ? 'text-white placeholder-gray-500' : 'text-black placeholder-gray-500',
                errors.title && 'border-b-2 border-destructive'
              )}
            />
            {errors.title && (
              <span className="text-destructive text-sm mt-s2 block">{errors.title.message}</span>
            )}
          </div>

          {/* Content Markdown Editor */}
          <div className="flex-1 relative mb-s8">
            <Controller
              name="content"
              control={control}
              render={({ field }) => (
                <MDEditor
                  value={field.value}
                  onChange={field.onChange}
                  preview="live"
                  height={500}
                  data-color-mode={isDark ? 'dark' : 'light'}
                  commandsFilter={(command) => {
                    if (command.name === 'title' || command.name === 'image' || command.name === 'checked-list') {
                      return false;
                    }
                    return command;
                  }}
                  className={cn(
                    errors.content && 'border-2 border-destructive rounded-r2'
                  )}
                />
              )}
            />
            {errors.content && (
              <span className="text-destructive text-sm mt-s2 block">{errors.content.message}</span>
            )}
          </div>

          {/* Bottom Toolbar */}
          <div className={cn('mt-s6 pt-s4 border-t flex gap-s4', isDark ? 'border-white/5' : 'border-gray-100')}>
            <button
              type="button"
              className={cn(
                'p-s3 rounded-r3 transition cursor-pointer',
                isDark ? 'text-gray-400 hover:bg-white/10' : 'text-gray-500 hover:bg-gray-100'
              )}
            >
              <ImageIcon size={24} />
            </button>
            <button
              type="button"
              className={cn(
                'p-s3 rounded-r3 transition cursor-pointer',
                isDark ? 'text-gray-400 hover:bg-white/10' : 'text-gray-500 hover:bg-gray-100'
              )}
            >
              <Paperclip size={24} />
            </button>
            <div className="ml-auto text-xs text-gray-500 flex items-center">
              {content?.length || 0} 글자
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
