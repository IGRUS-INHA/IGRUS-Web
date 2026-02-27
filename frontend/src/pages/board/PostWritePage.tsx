import { useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Image as ImageIcon } from 'lucide-react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { WysiwygEditor } from '@/components/feature/editor';
import { ImagePreviewList } from '@/components/feature/upload';
import { useCreatePost } from '@/api/model/post/post';
import { useUIStore } from '@/stores';
import { BOARDS, BOARD_CATEGORIES, POST_OPTIONS, BOARD_LABELS, postFormSchema, type PostFormData } from '@/constants/board';
import type { BoardType } from '@/types/common';
import { cn } from '@/lib/utils';
import { useIsMobile } from '@/hooks/useIsMobile';
import { useImageUpload } from '@/hooks/useImageUpload';
import { useToast } from '@/hooks/useToast';
import { IMAGE_UPLOAD_CONFIG } from '@/utils/upload';
import { isForbiddenError, isBoardWriteDenied, isUnauthorizedError, getErrorMessage } from '@/utils/error';

export default function PostWritePage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const isMobile = useIsMobile();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validBoardType = boardType as BoardType;

  // Get categories for this board
  const categories = BOARD_CATEGORIES[validBoardType] || BOARD_CATEGORIES.general;

  // Get board label
  const boardLabel = validBoardType ? BOARD_LABELS[validBoardType as keyof typeof BOARD_LABELS] : '게시판';

  // Check if anonymous and question posts are allowed
  const allowAnonymous = (POST_OPTIONS.ALLOW_ANONYMOUS as readonly BoardType[]).includes(validBoardType);
  const allowQuestion = (POST_OPTIONS.ALLOW_QUESTION as readonly BoardType[]).includes(validBoardType);
  const allowVisibleToAssociate = validBoardType === BOARDS.NOTICES;

  // Image upload
  const { files, isUploading, addFiles, removeFile, uploadAll } = useImageUpload({
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

  // Watch form values
  const title = watch('title');
  const content = watch('content');
  const isAnonymous = watch('isAnonymous');
  const isQuestion = watch('isQuestion');
  const isVisibleToAssociate = watch('isVisibleToAssociate');

  // Mutation
  const createPost = useCreatePost();

  // Handlers
  const handleBack = () => {
    navigate(`/board/${validBoardType}`);
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
    // 업로드 대기 중인 파일이 있으면 먼저 업로드
    const uploadResults = await uploadAll();
    const imageUrls = uploadResults.map((r) => r.objectKey);

    createPost.mutate(
      {
        boardCode: validBoardType,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isAnonymous !== undefined && { isAnonymous: data.isAnonymous }),
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
          ...(data.isVisibleToAssociate !== undefined && { isVisibleToAssociate: data.isVisibleToAssociate }),
          ...(imageUrls.length > 0 && { imageUrls }),
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
        onError: (error: unknown) => {
          let errorMessage = '게시글 작성에 실패했습니다.';

          if (isForbiddenError(error) || isBoardWriteDenied(error)) {
            errorMessage = '❌ 권한이 없습니다.\n\n로그인 후 다시 시도하거나,\n게시판 작성 권한을 확인해주세요.\n\n• 자유게시판/정보공유: MEMBER(정회원) 이상\n• 공지사항: OPERATOR(운영진) 이상';
          } else if (isUnauthorizedError(error)) {
            errorMessage = '❌ 로그인이 필요합니다.\n로그인 페이지로 이동합니다.';
            alert(errorMessage);
            navigate('/login');
            return;
          } else {
            errorMessage = getErrorMessage(error);
          }

          alert(errorMessage);
        },
      }
    );
  };

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
            <Save size={16} /> {isUploading ? '업로드 중...' : '게시하기'}
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
            {/* 게시판 이름 표시 (읽기 전용) */}
            <div className="px-2 py-2 text-xl text-[#03A69E]">
              {validBoardType === 'general' ? boardLabel : `${boardLabel} 게시판`}
            </div>

            {/* 숨겨진 카테고리 필드 (첫 번째 카테고리로 자동 설정) */}
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
