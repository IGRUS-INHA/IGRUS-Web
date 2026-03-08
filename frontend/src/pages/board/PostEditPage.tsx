import { useEffect, useMemo, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Save } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PostFormFields } from "@/components/feature/board/PostFormFields";
import { useGetPostDetail, useUpdatePost } from "@/api/model/post/post";
import { useUIStore } from "@/stores";
import {
  BOARDS,
  BOARD_CATEGORIES,
  POST_OPTIONS,
  BOARD_LABELS,
  postFormSchema,
  type PostFormData,
} from "@/constants/board";
import type { BoardType } from "@/types/common";
import { cn } from "@/lib/utils";
import { useIsMobile } from "@/hooks/useIsMobile";
import { useImageUpload } from "@/hooks/useImageUpload";
import { useResolvedImageUrls } from "@/hooks/useResolvedImageUrls";
import { useToast } from "@/hooks/useToast";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";
import { useQueryClient } from "@tanstack/react-query";
import {
  isForbiddenError,
  isUnauthorizedError,
  isNotFoundError,
  getErrorMessage,
} from "@/utils/error";

export default function PostEditPage() {
  const { boardType, postId } = useParams<{
    boardType: BoardType;
    postId: string;
  }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === "dark";
  const isMobile = useIsMobile();
  const queryClient = useQueryClient();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validBoardType = boardType as BoardType;
  const numericPostId = Number(postId);

  const {
    data: response,
    isLoading,
    error,
  } = useGetPostDetail(validBoardType, numericPostId, {
    query: { enabled: !!(validBoardType && numericPostId) },
  });
  const post = response?.data;

  const categories =
    BOARD_CATEGORIES[validBoardType] || BOARD_CATEGORIES.general;

  const boardLabel = validBoardType
    ? BOARD_LABELS[validBoardType as keyof typeof BOARD_LABELS]
    : "게시판";

  const allowAnonymous = (
    POST_OPTIONS.ALLOW_ANONYMOUS as readonly BoardType[]
  ).includes(validBoardType);
  const allowQuestion = (
    POST_OPTIONS.ALLOW_QUESTION as readonly BoardType[]
  ).includes(validBoardType);
  const allowVisibleToAssociate = validBoardType === BOARDS.NOTICES;

  const {
    files,
    isUploading,
    addFiles,
    removeFile,
    uploadAll,
    setExistingItems,
  } = useImageUpload({
    config: IMAGE_UPLOAD_CONFIG,
    onValidationError: (errors) => {
      errors.forEach((msg) => toast.error(msg));
    },
  });

  const existingObjectKeys = useMemo(
    () => post?.imageUrls ?? [],
    [post?.imageUrls],
  );
  const { urls: resolvedUrls } = useResolvedImageUrls(existingObjectKeys);

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
      category: categories?.[0]?.value ?? "general",
      isAnonymous: false,
      isQuestion: false,
      isVisibleToAssociate: false,
    },
  });

  const [formReady, setFormReady] = useState(false);
  const [imagesInitialized, setImagesInitialized] = useState(false);

  useEffect(() => {
    if (post) {
      reset({
        title: post.title || "",
        content: post.content || "",
        category: categories?.[0]?.value ?? "general",
        isAnonymous: post.isAnonymous || false,
        isQuestion: post.isQuestion || false,
        isVisibleToAssociate: post.isVisibleToAssociate || false,
      });
      setFormReady(true);
    }
  }, [post, reset, categories]);

  useEffect(() => {
    if (imagesInitialized) return;
    if (existingObjectKeys.length === 0) return;
    if (resolvedUrls.size < existingObjectKeys.length) return;

    const items = existingObjectKeys.map((key) => ({
      objectKey: key,
      previewUrl: resolvedUrls.get(key) ?? key,
    }));
    setExistingItems(items);
    setImagesInitialized(true);
  }, [existingObjectKeys, resolvedUrls, setExistingItems, imagesInitialized]);

  const title = watch("title");
  const content = watch("content");
  const isAnonymous = watch("isAnonymous");
  const isQuestion = watch("isQuestion");
  const isVisibleToAssociate = watch("isVisibleToAssociate");

  const updatePost = useUpdatePost();

  const handleBack = () => {
    navigate(`/board/${validBoardType}/${postId}`);
  };

  const handleImageButtonClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
      e.target.value = "";
    }
  };

  const onSubmit = async (data: PostFormData) => {
    const uploadResults = await uploadAll();
    const imageUrls = uploadResults.map((r) => r.objectKey);

    updatePost.mutate(
      {
        boardCode: validBoardType,
        postId: numericPostId,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
          ...(allowVisibleToAssociate && {
            isVisibleToAssociate: data.isVisibleToAssociate ?? false,
          }),
          ...(imageUrls.length > 0 ? { imageUrls } : {}),
        },
      },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: [
              `/api/v1/boards/${validBoardType}/posts/${numericPostId}`,
            ],
          });
          void queryClient.invalidateQueries({
            queryKey: [`/api/v1/boards/${validBoardType}/posts`],
          });
          void queryClient.invalidateQueries({
            queryKey: ["/api/v1/pinned-posts"],
          });

          navigate(`/board/${validBoardType}/${postId}`);
        },
        onError: (error: unknown) => {
          let errorMessage = "게시글 수정에 실패했습니다.";

          if (isForbiddenError(error)) {
            errorMessage =
              "수정 권한이 없습니다.\n\n작성자 본인만 게시글을 수정할 수 있습니다.";
          } else if (isUnauthorizedError(error)) {
            errorMessage = "로그인이 필요합니다.\n로그인 페이지로 이동합니다.";
            alert(errorMessage);
            navigate("/login");
            return;
          } else if (isNotFoundError(error)) {
            errorMessage = "게시글을 찾을 수 없습니다.";
          } else {
            errorMessage = getErrorMessage(error);
          }

          alert(errorMessage);
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-muted-foreground">로딩 중...</p>
      </div>
    );
  }

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
            "flex items-center gap-2 text-sm font-bold transition-colors cursor-pointer",
            isDark
              ? "text-gray-400 hover:text-white"
              : "text-gray-500 hover:text-black",
          )}
        >
          <ArrowLeft size={18} /> 취소
        </button>
        <div className="flex items-center gap-s4">
          <span className="text-xs text-gray-500 font-medium">
            {title && content ? "저장됨" : "저장 안됨"}
          </span>
          <button
            onClick={handleSubmit(onSubmit)}
            disabled={isBusy}
            type="button"
            className="bg-[#03A69E] text-white px-6 py-2 rounded-full text-sm font-bold hover:bg-[#028b84] transition shadow-lg shadow-[#03A69E]/20 flex items-center gap-2 disabled:opacity-50 cursor-pointer"
          >
            <Save size={16} /> {isUploading ? "업로드 중..." : "수정하기"}
          </button>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)}>
        <PostFormFields
          register={register}
          control={control}
          errors={errors}
          setValue={setValue}
          isDark={isDark}
          isMobile={isMobile}
          boardLabel={boardLabel}
          validBoardType={validBoardType}
          allowAnonymous={allowAnonymous}
          allowQuestion={allowQuestion}
          allowVisibleToAssociate={allowVisibleToAssociate}
          isAnonymous={isAnonymous}
          isQuestion={isQuestion}
          isVisibleToAssociate={isVisibleToAssociate}
          content={content}
          files={files}
          fileInputRef={fileInputRef}
          onRemoveFile={removeFile}
          onImageButtonClick={handleImageButtonClick}
          onFileChange={handleFileChange}
          formReady={formReady}
        />
      </form>
    </div>
  );
}
