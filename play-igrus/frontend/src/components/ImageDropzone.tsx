import { useEffect, useMemo, useRef, useState } from "react";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { label, requiredMark } from "./formStyles";

const ACCEPTED = ["image/png", "image/jpeg", "image/webp"];
const MAX_BYTES = 4 << 20; // 서버와 동일 4MB — 업로드 전에 미리 걸러준다

interface Props {
  id: string;
  title: string;
  hint: string;
  file?: File;
  onChange: (file?: File) => void;
  /** 미리보기 비율 — 썸네일은 정방형, 배너는 가로 */
  square?: boolean;
  /** 수정 모드에서 이미 등록된 이미지 — 새 파일 선택 전까지 미리보기로 보여준다 */
  existingUrl?: string;
  /** 필수 뱃지 표시 */
  required?: boolean;
}

/** 이미지 선택 영역 — 탭하면 파일 선택, 드래그 앤 드롭도 지원 (모바일은 탭) */
export default function ImageDropzone({
  id,
  title,
  hint,
  file,
  onChange,
  square,
  existingUrl,
  required,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);
  const [warn, setWarn] = useState("");

  const objectUrl = useMemo(() => (file ? URL.createObjectURL(file) : undefined), [file]);
  useEffect(() => {
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [objectUrl]);
  // 새 파일 > 기존 등록 이미지 순으로 미리보기 (✕ 는 새 파일만 취소 — 기존 이미지 제거는 미지원)
  const previewUrl = objectUrl ?? existingUrl;

  const pick = (picked?: File) => {
    setWarn("");
    if (!picked) return;
    if (!ACCEPTED.includes(picked.type)) {
      setWarn("PNG/JPEG/WebP 이미지만 올릴 수 있어요");
      return;
    }
    if (picked.size > MAX_BYTES) {
      setWarn("4MB 이하 이미지만 올릴 수 있어요");
      return;
    }
    onChange(picked);
  };

  return (
    <div>
      <p className={label}>
        {title}
        {required && <span className={requiredMark}>필수</span>}
      </p>
      <div
        role="button"
        tabIndex={0}
        aria-label={`${title} 업로드`}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") inputRef.current?.click();
        }}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          pick(e.dataTransfer.files?.[0]);
        }}
        className={css({
          pos: "relative",
          border: "2px dashed",
          borderColor: dragOver ? "indigo.500" : "gray.300",
          bg: dragOver ? "indigo.50" : "gray.50",
          rounded: "xl",
          cursor: "pointer",
          overflow: "hidden",
          transition: "all 0.15s",
          _hover: { borderColor: "indigo.400" },
        })}
      >
        {previewUrl ? (
          <>
            <img
              src={previewUrl}
              alt="미리보기"
              className={css({
                w: square ? "40" : "full",
                h: square ? "40" : "32",
                mx: square ? "auto" : undefined,
                display: "block",
                objectFit: "cover",
              })}
            />
            {file && (
            <button
              type="button"
              aria-label="이미지 제거"
              onClick={(e) => {
                e.stopPropagation();
                if (inputRef.current) inputRef.current.value = "";
                onChange(undefined);
              }}
              className={css({
                pos: "absolute",
                top: "2",
                right: "2",
                w: "7",
                h: "7",
                rounded: "full",
                bg: "black/60",
                color: "white",
                fontSize: "sm",
                cursor: "pointer",
              })}
            >
              ✕
            </button>
            )}
          </>
        ) : (
          <div className={flex({ direction: "column", align: "center", gap: "1", py: "7" })}>
            <span className={css({ fontSize: "2xl" })}>🖼️</span>
            <span className={css({ fontSize: "sm", fontWeight: "600", color: "gray.600" })}>
              탭해서 선택하거나 끌어다 놓기
            </span>
            <span className={css({ fontSize: "xs", color: "gray.400" })}>{hint}</span>
          </div>
        )}
      </div>
      {warn && <p className={css({ fontSize: "xs", color: "red.500", mt: "1" })}>{warn}</p>}
      <input
        ref={inputRef}
        id={id}
        type="file"
        accept={ACCEPTED.join(",")}
        className={css({ srOnly: true })}
        onChange={(e) => pick(e.target.files?.[0])}
      />
    </div>
  );
}
