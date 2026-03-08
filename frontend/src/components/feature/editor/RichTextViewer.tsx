import { useEffect } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { Markdown } from "tiptap-markdown";
import { cn } from "@/lib/utils";

interface RichTextViewerProps {
  content: string;
  className?: string;
}

export function RichTextViewer({ content, className }: RichTextViewerProps) {
  const editor = useEditor({
    editable: false,
    extensions: [StarterKit, Markdown.configure({ html: false })],
    content: "",
  });

  // 에디터 마운트 후 혹은 content 변경 시 마크다운 파싱하여 반영
  useEffect(() => {
    if (!editor || !content) return;
    editor.commands.setContent(content);
  }, [editor, content]);

  return (
    <EditorContent
      editor={editor}
      className={cn("prose prose-sm max-w-none", className)}
    />
  );
}
