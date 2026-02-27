import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Image as ImageIcon } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { WysiwygEditor } from '@/components/feature/editor';
import { ImagePreviewList } from '@/components/feature/upload';
import { useGetPostDetail, useUpdatePost } from '@/api/model/post/post';
import { useUIStore } from '@/stores';
import { BOARDS, BOARD_CATEGORIES, POST_OPTIONS, BOARD_LABELS, postFormSchema, type PostFormData } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useIsMobile } from '@/hooks/useIsMobile';
import { useImageUpload } from '@/hooks/useImageUpload';
import { useToast } from '@/hooks/useToast';
import { IMAGE_UPLOAD_CONFIG } from '@/utils/upload';
import { useQueryClient } from '@tanstack/react-query';
import { isForbiddenError, isUnauthorizedError, isNotFoundError, getErrorMessage } from '@/utils/error';

export default function PostEditPage() {
  const { boardType, postId } = useParams<{ boardType: BoardType; postId: string }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const isMobile = useIsMobile();
  const queryClient = useQueryClient();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

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
  const post = response?.data;

  // Get categories for this board
  const categories = BOARD_CATEGORIES[validBoardType] || BOARD_CATEGORIES.general;

  // Get board label
  const boardLabel = validBoardType ? BOARD_LABELS[validBoardType as keyof typeof BOARD_LABELS] : '게시판';

  // Check if anonymous and question posts are allowed
  const allowAnonymous = (POST_OPTIONS.ALLOW_ANONYMOUS as readonly BoardType[]).includes(validBoardType);
  const allowQuestion = (POST_OPTIONS.ALLOW_QUESTION as readonly BoardType[]).includes(validBoardType);
  const allowVisibleToAssociate = validBoardType === BOARDS.NOTICES;

  // Image upload
  const { files, isUploading, addFiles, removeFile, uploadAll, setExistingUrls } = useImageUpload({
    config: IMAGE_UPLOAD_CONFIG,
    onValidationError: (errors) => {
      errors.forEach((msg) => toast.error(msg));
    },
  });

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
      category: categories[0]?.value || 'general',
      isAnonymous: false,
      isQuestion: false,
      isVisibleToAssociate: false,
    },
  });

  // Initialize form with fetched data
  const [formReady, setFormReady] = useState(false);
  useEffect(() => {
    if (post) {
      reset({
        title: post.title || '',
        content: post.content || '',
        category: categories[0]?.value || 'general',
        isAnonymous: post.isAnonymous || false,
        isQuestion: post.isQuestion || false,
        isVisibleToAssociate: post.isVisibleToAssociate || false,
      });
      if (post.imageUrls && post.imageUrls.length > 0) {
        setExistingUrls(post.imageUrls);
      }
      setFormReady(true);
    }
  }, [post, reset, categories, setExistingUrls]);

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

  const handleImageButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
      e.target.value = '';
    }
  };

  const onSubmit = async (data: PostFormData) => {
    const uploadResults = await uploadAll();
    const imageUrls = uploadResults.map((r) => r.fileUrl);

    updatePost.mutate(
      {
        boardCode: validBoardType,
        postId: numericPostId,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
          ...(allowVisibleToAssociate && { isVisibleToAssociate: data.isVisibleToAssociate ?? false }),
          ...(imageUrls.length > 0 ? { imageUrls } : {}),
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
          void queryClient.invalidateQueries({
            queryKey: ['/api/v1/pinned-posts']
          });

          navigate(`/board/${validBoardType}/${postId}`);
        },
        onError: (error: unknown) => {
          let errorMessage = '게시글 수정에 실패했습니다.';

          if (isForbiddenError(error)) {
            errorMessage = '수정 권한이 없습니다.\n\n작성자 본인만 게시글을 수정할 수 있습니다.';
          } else if (isUnauthorizedError(error)) {
            errorMessage = '로그인이 필요합니다.\n로그인 페이지로 이동합니다.';
            alert(errorMessage);
            navigate('/login');
            return;
          } else if (isNotFoundError(error)) {
            errorMessage = '게시글을 찾을 수 없습니다.';
          } else {
            errorMessage = getErrorMessage(error);
          }

          alert(errorMessage);
        },
      }
    );
  };

  // Loading state
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-muted-foreground">로딩 중...</p>
      </div>
    );
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

  const isBusy = isSubmitting || isUploading;

  return (
    <div className="animate-in slide-in-from-bottom-8 duration-300">
      {/* Top Bar */}
      <div className="flex justify-between items-center mb-s8 sticky top-0 z-10 py-s4 backdrop-blur-md bg-background/80">
        <button
          onClick={handleBack}
          type="button"
          className={cn(
            'flex items-center gap-2 text-sm font-bold transition-colors cursor-pointer',
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
            disabled={isBusy}
            type="button"
            className="bg-[#03A69E] text-white px-6 py-2 rounded-full text-sm font-bold hover:bg-[#028b84] transition shadow-lg shadow-[#03A69E]/20 flex items-center gap-2 disabled:opacity-50 cursor-pointer"
          >
            <Save size={16} /> {isUploading ? '업로드 중...' : '수정하기'}
          </button>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)}>
        <div
          className={cn(
            'w-full max-w-[1616px] mx-auto p-s4 sm:p-8 md:p-12 rounded-r4 sm:rounded-[1.5rem] md:rounded-[2.5rem] border min-h-[60vh] md:min-h-[80vh] flex flex-col',
            isDark ? 'bg-[#1A1A1A] border-white/5' : 'bg-white border-gray-100 shadow-sm'
          )}
        >
          {/* Settings Bar */}
          <div className="flex flex-wrap gap-s4 mb-s6">
            <div className="px-2 py-2 text-xl text-[#03A69E]">
              {validBoardType === 'general' ? boardLabel : `${boardLabel} 게시판`}
            </div>

            <input type="hidden" {...register('category')} />

            {allowAnonymous && (
              <button
                onClick={() => setValue('isAnonymous', !isAnonymous)}
                type="button"
                className={cn(
                  'px-4 py-2 rounded-xl text-sm border transition-all cursor-pointer',
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
                  'px-4 py-2 rounded-xl text-sm border transition-all cursor-pointer',
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
                  'px-4 py-2 rounded-xl text-sm border transition-all cursor-pointer',
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
                'w-full text-2xl sm:text-3xl md:text-4xl font-bold bg-transparent border-none focus:ring-0 focus:outline-none opacity-80',
                isDark ? 'text-white placeholder-gray-500' : 'text-black placeholder-gray-500',
                errors.title && 'border-b-2 border-destructive'
              )}
            />
            {errors.title && (
              <span className="text-destructive text-sm mt-s2 block">{errors.title.message}</span>
            )}
          </div>

          {/* Content WYSIWYG Editor */}
          <div className="flex-1 relative mb-s8">
            {formReady && (
              <Controller
                name="content"
                control={control}
                render={({ field }) => (
                  <WysiwygEditor
                    value={field.value ?? ''}
                    onChange={field.onChange}
                    hasError={!!errors.content}
                  />
                )}
              />
            )}
            {errors.content && (
              <span className="text-destructive text-sm mt-s2 block">{errors.content.message}</span>
            )}
          </div>

          {/* Image Preview */}
          {files.length > 0 && (
            <ImagePreviewList
              files={files}
              onRemove={removeFile}
              className="mb-s4"
            />
          )}

          {/* Bottom Toolbar */}
          <div className={cn('mt-s4 sm:mt-8 pt-s3 sm:pt-4 border-t flex gap-s3 sm:gap-4', isDark ? 'border-white/5' : 'border-gray-100')}>
            <button
              type="button"
              onClick={handleImageButtonClick}
              className={cn(
                'p-2 sm:p-3 rounded-lg transition cursor-pointer',
                isDark ? 'text-gray-400 hover:bg-white/10' : 'text-gray-500 hover:bg-gray-100'
              )}
            >
              <ImageIcon size={isMobile ? 20 : 24} />
            </button>
            <div className="ml-auto text-xs text-gray-500 flex items-center gap-s3">
              {files.length > 0 && (
                <span>이미지 {files.length}/{IMAGE_UPLOAD_CONFIG.maxFiles}</span>
              )}
              <span>{content?.length || 0} 글자</span>
            </div>
          </div>
        </div>
      </form>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        onChange={handleFileChange}
        className="hidden"
      />
    </div>
  );
}
