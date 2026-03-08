import type { RefObject } from "react";
import { Controller } from "react-hook-form";
import type {
  Control,
  FieldErrors,
  UseFormRegister,
  UseFormSetValue,
} from "react-hook-form";
import { Image as ImageIcon } from "lucide-react";
import { WysiwygEditor } from "@/components/feature/editor";
import { ImagePreviewList } from "@/components/feature/upload";
import type { PostFormData } from "@/constants/board";
import type { BoardType } from "@/types/common";
import type { UploadFile } from "@/types/upload";
import { IMAGE_UPLOAD_CONFIG } from "@/utils/upload";
import { cn } from "@/lib/utils";

interface PostFormFieldsProps {
  // react-hook-form
  register: UseFormRegister<PostFormData>;
  control: Control<PostFormData>;
  errors: FieldErrors<PostFormData>;
  setValue: UseFormSetValue<PostFormData>;

  // 테마/레이아웃
  isDark: boolean;
  isMobile: boolean;

  // 게시판 메타
  boardLabel: string;
  validBoardType: BoardType;
  allowAnonymous: boolean;
  allowQuestion: boolean;
  allowVisibleToAssociate: boolean;

  // watch된 값
  isAnonymous: boolean;
  isQuestion: boolean;
  isVisibleToAssociate: boolean;
  content: string | undefined;

  // 이미지
  files: UploadFile[];
  fileInputRef: RefObject<HTMLInputElement>;
  onRemoveFile: (id: string) => void;
  onImageButtonClick: () => void;
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;

  // Edit 전용 (Write는 기본값 true)
  formReady?: boolean;
}

export function PostFormFields({
  register,
  control,
  errors,
  setValue,
  isDark,
  isMobile,
  boardLabel,
  validBoardType,
  allowAnonymous,
  allowQuestion,
  allowVisibleToAssociate,
  isAnonymous,
  isQuestion,
  isVisibleToAssociate,
  content,
  files,
  fileInputRef,
  onRemoveFile,
  onImageButtonClick,
  onFileChange,
  formReady = true,
}: PostFormFieldsProps) {
  const toggleBtnBase =
    "px-4 py-2 rounded-xl text-sm border transition-all cursor-pointer";
  const toggleBtnActive = "bg-[#03A69E]/10 border-[#03A69E] text-[#03A69E]";
  const toggleBtnInactive = isDark
    ? "bg-white/5 border-white/10 text-gray-400"
    : "bg-gray-50 border-gray-200 text-gray-500";

  return (
    <>
      <div
        className={cn(
          "w-full max-w-[1616px] mx-auto p-s4 sm:p-8 md:p-12 rounded-r4 sm:rounded-[1.5rem] md:rounded-[2.5rem] border min-h-[60vh] md:min-h-[80vh] flex flex-col",
          isDark
            ? "bg-[#1A1A1A] border-white/5"
            : "bg-white border-gray-100 shadow-sm",
        )}
      >
        {/* Settings Bar */}
        <div className="flex flex-wrap gap-s4 mb-s6">
          <div className="px-2 py-2 text-xl text-[#03A69E]">
            {validBoardType === "general" ? boardLabel : `${boardLabel} 게시판`}
          </div>

          <input type="hidden" {...register("category")} />

          {allowAnonymous && (
            <button
              onClick={() => setValue("isAnonymous", !isAnonymous)}
              type="button"
              className={cn(
                toggleBtnBase,
                isAnonymous ? toggleBtnActive : toggleBtnInactive,
              )}
            >
              익명
            </button>
          )}

          {allowQuestion && (
            <button
              onClick={() => setValue("isQuestion", !isQuestion)}
              type="button"
              className={cn(
                toggleBtnBase,
                isQuestion ? toggleBtnActive : toggleBtnInactive,
              )}
            >
              질문
            </button>
          )}

          {allowVisibleToAssociate && (
            <button
              onClick={() =>
                setValue("isVisibleToAssociate", !isVisibleToAssociate)
              }
              type="button"
              className={cn(
                toggleBtnBase,
                isVisibleToAssociate ? toggleBtnActive : toggleBtnInactive,
              )}
            >
              준회원 공개
            </button>
          )}
        </div>

        {/* Title Input */}
        <div className="mb-s6">
          <input
            {...register("title")}
            type="text"
            placeholder="게시글 제목"
            className={cn(
              "w-full text-2xl sm:text-3xl md:text-4xl font-bold bg-transparent border-none focus:ring-0 focus:outline-none opacity-80",
              isDark
                ? "text-white placeholder-gray-500"
                : "text-black placeholder-gray-500",
              errors.title && "border-b-2 border-destructive",
            )}
          />
          {errors.title && (
            <span className="text-destructive text-sm mt-s2 block">
              {errors.title.message}
            </span>
          )}
        </div>

        {/* Content WYSIWYG Editor */}
        <div className="flex-1 relative mb-s7">
          {formReady && (
            <Controller
              name="content"
              control={control}
              render={({ field }) => (
                <WysiwygEditor
                  value={field.value ?? ""}
                  onChange={field.onChange}
                  hasError={!!errors.content}
                />
              )}
            />
          )}
          {errors.content && (
            <span className="text-destructive text-sm mt-s2 block">
              {errors.content.message}
            </span>
          )}
        </div>

        {/* Image Preview */}
        {files.length > 0 && (
          <ImagePreviewList
            files={files}
            onRemove={onRemoveFile}
            className="mb-s4"
          />
        )}

        {/* Bottom Toolbar */}
        <div
          className={cn(
            "sm:pt-s3 sm:pt-4 border-t flex gap-s3 sm:gap-4",
            isDark ? "border-white/5" : "border-gray-100",
          )}
        >
          <button
            type="button"
            onClick={onImageButtonClick}
            className={cn(
              "p-2 sm:p-3 rounded-lg transition cursor-pointer",
              isDark
                ? "text-gray-400 hover:bg-white/10"
                : "text-gray-500 hover:bg-gray-100",
            )}
          >
            <ImageIcon size={isMobile ? 20 : 24} />
          </button>
          <div className="ml-auto text-xs text-gray-500 flex items-center gap-s3">
            {files.length > 0 && (
              <span>
                이미지 {files.length}/{IMAGE_UPLOAD_CONFIG.maxFiles}
              </span>
            )}
            <span>{content?.length || 0} 글자</span>
          </div>
        </div>
      </div>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        onChange={onFileChange}
        className="hidden"
      />
    </>
  );
}
