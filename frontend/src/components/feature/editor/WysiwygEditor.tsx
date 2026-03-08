import { useCallback, useEffect, useRef } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import type { Editor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { Markdown } from "tiptap-markdown";
import {
  Bold,
  Italic,
  Strikethrough,
  Code,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Code2,
  Minus,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface WysiwygEditorProps {
  value: string;
  onChange: (text: string) => void;
  placeholder?: string;
  className?: string;
  hasError?: boolean;
}

interface ToolbarButtonProps {
  onClick: () => void;
  isActive?: boolean;
  title: string;
  children: React.ReactNode;
}

function ToolbarButton({
  onClick,
  isActive,
  title,
  children,
}: ToolbarButtonProps) {
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className={cn(
        "p-1.5 rounded transition-colors cursor-pointer",
        isActive
          ? "bg-primary/15 text-primary"
          : "text-muted-foreground hover:text-foreground hover:bg-muted",
      )}
    >
      {children}
    </button>
  );
}

export function WysiwygEditor({
  value,
  onChange,
  placeholder,
  className,
  hasError,
}: WysiwygEditorProps) {
  const isExternalUpdate = useRef(false);

  const handleUpdate = useCallback(
    ({ editor }: { editor: Editor }) => {
      if (isExternalUpdate.current) return;
      onChange(editor.storage.markdown.getMarkdown() as string);
    },
    [onChange],
  );

  const editor = useEditor({
    extensions: [
      StarterKit,
      Markdown.configure({
        html: false,
        transformCopiedText: true,
        transformPastedText: true,
      }),
    ],
    content: value,
    onUpdate: handleUpdate,
    editorProps: {
      attributes: {
        class: "min-h-[300px] md:min-h-[500px] px-s5 pb-s2 focus:outline-none",
        ...(placeholder ? { "data-placeholder": placeholder } : {}),
      },
    },
  });

  // 외부 value 변경 시 에디터 콘텐츠 동기화 (수정 페이지에서 API 데이터 로드 후 반영)
  useEffect(() => {
    if (!editor || !value) return;
    const currentContent = editor.storage.markdown.getMarkdown() as string;
    if (currentContent !== value) {
      isExternalUpdate.current = true;
      editor.commands.setContent(value);
      isExternalUpdate.current = false;
    }
  }, [editor, value]);

  if (!editor) return null;

  return (
    <div
      className={cn(
        "rounded-r2 border border-border overflow-hidden",
        hasError && "border-2 border-destructive",
        className,
      )}
    >
      {/* 툴바 */}
      <div className="flex flex-wrap items-center gap-0.5 px-s3 py-s2 border-b border-border bg-muted/30">
        {/* 인라인 서식 */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBold().run()}
          isActive={editor.isActive("bold")}
          title="굵게 (Ctrl+B)"
        >
          <Bold size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleItalic().run()}
          isActive={editor.isActive("italic")}
          title="기울임 (Ctrl+I)"
        >
          <Italic size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleStrike().run()}
          isActive={editor.isActive("strike")}
          title="취소선"
        >
          <Strikethrough size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleCode().run()}
          isActive={editor.isActive("code")}
          title="인라인 코드 (Ctrl+E)"
        >
          <Code size={15} />
        </ToolbarButton>

        <div className="w-px h-4 bg-border mx-1" />

        {/* 제목 */}
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 1 }).run()
          }
          isActive={editor.isActive("heading", { level: 1 })}
          title="제목 1"
        >
          <Heading1 size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 2 }).run()
          }
          isActive={editor.isActive("heading", { level: 2 })}
          title="제목 2"
        >
          <Heading2 size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() =>
            editor.chain().focus().toggleHeading({ level: 3 }).run()
          }
          isActive={editor.isActive("heading", { level: 3 })}
          title="제목 3"
        >
          <Heading3 size={15} />
        </ToolbarButton>

        <div className="w-px h-4 bg-border mx-1" />

        {/* 리스트 */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBulletList().run()}
          isActive={editor.isActive("bulletList")}
          title="글머리 목록"
        >
          <List size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleOrderedList().run()}
          isActive={editor.isActive("orderedList")}
          title="번호 목록"
        >
          <ListOrdered size={15} />
        </ToolbarButton>

        <div className="w-px h-4 bg-border mx-1" />

        {/* 블록 */}
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleBlockquote().run()}
          isActive={editor.isActive("blockquote")}
          title="인용구"
        >
          <Quote size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().toggleCodeBlock().run()}
          isActive={editor.isActive("codeBlock")}
          title="코드 블록"
        >
          <Code2 size={15} />
        </ToolbarButton>
        <ToolbarButton
          onClick={() => editor.chain().focus().setHorizontalRule().run()}
          title="구분선"
        >
          <Minus size={15} />
        </ToolbarButton>
      </div>

      {/* 에디터 본문 */}
      <EditorContent
        editor={editor}
        className="prose prose-sm dark:prose-invert max-w-none w-full"
      />
    </div>
  );
}
