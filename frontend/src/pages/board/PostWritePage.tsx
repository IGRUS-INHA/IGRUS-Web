import { useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Save } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PostFormFields } from "@/components/feature/board/PostFormFields";
import { useCreatePost } from "@/api/model/post/post";
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
import { useToast } from "@/hooks/useToast";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";
import {
  isForbiddenError,
  isBoardWriteDenied,
  isUnauthorizedError,
  getErrorMessage,
} from "@/utils/error";

export default function PostWritePage() {
  const { boardType } = useParams<{ boardType: BoardType }>();
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === "dark";
  const isMobile = useIsMobile();
  const toast = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validBoardType = boardType as BoardType;

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

  const { files, isUploading, addFiles, removeFile, uploadAll } =
    useImageUpload({
      config: IMAGE_UPLOAD_CONFIG,
      onValidationError: (errors) => {
        errors.forEach((msg) => toast.error(msg));
      },
    });

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
      category: categories?.[0]?.value ?? "general",
      isAnonymous: false,
      isQuestion: false,
      isVisibleToAssociate: false,
    },
  });

  const title = watch("title");
  const content = watch("content");
  const isAnonymous = watch("isAnonymous");
  const isQuestion = watch("isQuestion");
  const isVisibleToAssociate = watch("isVisibleToAssociate");

  const createPost = useCreatePost();

  const handleBack = () => {
    navigate(`/board/${validBoardType}`);
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

    createPost.mutate(
      {
        boardCode: validBoardType,
        data: {
          title: data.title,
          content: data.content,
          ...(data.isAnonymous !== undefined && {
            isAnonymous: data.isAnonymous,
          }),
          ...(data.isQuestion !== undefined && { isQuestion: data.isQuestion }),
          ...(data.isVisibleToAssociate !== undefined && {
            isVisibleToAssociate: data.isVisibleToAssociate,
          }),
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
          let errorMessage = "게시글 작성에 실패했습니다.";

          if (isForbiddenError(error) || isBoardWriteDenied(error)) {
            errorMessage =
              "❌ 권한이 없습니다.\n\n로그인 후 다시 시도하거나,\n게시판 작성 권한을 확인해주세요.\n\n• 자유게시판/정보공유: MEMBER(정회원) 이상\n• 공지사항: OPERATOR(운영진) 이상";
          } else if (isUnauthorizedError(error)) {
            errorMessage =
              "❌ 로그인이 필요합니다.\n로그인 페이지로 이동합니다.";
            alert(errorMessage);
            navigate("/login");
            return;
          } else {
            errorMessage = getErrorMessage(error);
          }

          alert(errorMessage);
        },
      },
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
            <Save size={16} /> {isUploading ? "업로드 중..." : "게시하기"}
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
        />
      </form>
    </div>
  );
}
