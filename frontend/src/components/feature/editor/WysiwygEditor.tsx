import { useCallback, useEffect, useRef } from 'react';
import {
  EditorRoot,
  EditorContent,
  EditorCommand,
  EditorCommandItem,
  EditorCommandList,
  EditorCommandEmpty,
  EditorBubble,
  EditorBubbleItem,
  type EditorInstance,
} from 'novel';
import { handleCommandNavigation } from 'novel';
import { Markdown } from 'tiptap-markdown';
import { Bold, Italic, Strikethrough, Code } from 'lucide-react';
import { defaultExtensions } from './extensions';
import { suggestionItems, slashCommand } from './slash-command';
import { cn } from '@/lib/utils';

interface WysiwygEditorProps {
  value: string;
  onChange: (markdown: string) => void;
  placeholder?: string;
  className?: string;
  hasError?: boolean;
}

const extensions = [
  ...defaultExtensions,
  slashCommand,
  Markdown.configure({
    html: false,
    transformCopiedText: true,
    transformPastedText: true,
  }),
];

export function WysiwygEditor({
  value,
  onChange,
  className,
  hasError,
}: WysiwygEditorProps) {
  const editorRef = useRef<EditorInstance | null>(null);

  const handleCreate = useCallback(({ editor }: { editor: EditorInstance }) => {
    editorRef.current = editor;
    if (value) {
      editor.commands.setContent(value);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleUpdate = useCallback(({ editor }: { editor: EditorInstance }) => {
    const md = editor.storage.markdown.getMarkdown();
    onChange(md);
  }, [onChange]);

  // 외부 value 변경 시 에디터 콘텐츠 동기화 (수정 페이지에서 API 데이터 로드 후 반영)
  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || !value) return;
    const currentContent = editor.storage.markdown.getMarkdown();
    if (currentContent !== value) {
      editor.commands.setContent(value);
    }
  }, [value]);

  return (
    <div
      className={cn(
        'novel-editor rounded-r2 border border-border',
        hasError && 'border-2 border-destructive',
        className,
      )}
    >
      <EditorRoot>
        <EditorContent
          extensions={extensions}
          initialContent={undefined}
          onCreate={handleCreate}
          onUpdate={handleUpdate}
          editorProps={{
            handleDOMEvents: {
              keydown: (_view, event) => handleCommandNavigation(event),
            },
            attributes: {
              class: 'prose prose-lg dark:prose-invert prose-headings:font-bold focus:outline-none max-w-full min-h-[500px] px-s6 py-s5',
            },
          }}
          className="w-full"
        >
          {/* Slash Command Menu */}
          <EditorCommand className="z-50 h-auto max-h-[330px] w-72 overflow-y-auto rounded-r3 border border-border bg-background px-1 py-2 shadow-md">
            <EditorCommandEmpty className="px-2 text-muted-foreground text-sm">
              결과 없음
            </EditorCommandEmpty>
            <EditorCommandList>
              {suggestionItems.map((item) => (
                <EditorCommandItem
                  key={item.title}
                  value={item.title}
                  onCommand={(val) => item.command?.(val)}
                  className="flex items-center gap-2 px-2 py-1.5 rounded-r2 text-sm hover:bg-accent cursor-pointer aria-selected:bg-accent"
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-r2 border border-border bg-background">
                    {item.icon}
                  </div>
                  <div>
                    <p className="font-medium">{item.title}</p>
                    <p className="text-xs text-muted-foreground">{item.description}</p>
                  </div>
                </EditorCommandItem>
              ))}
            </EditorCommandList>
          </EditorCommand>

          {/* Bubble Menu */}
          <EditorBubble className="flex w-fit overflow-hidden rounded-r2 border border-border bg-background shadow-xl">
            <EditorBubbleItem
              onSelect={(editor) => editor.chain().focus().toggleBold().run()}
            >
              <button
                type="button"
                className={cn(
                  'p-2 text-muted-foreground hover:text-foreground transition-colors',
                )}
              >
                <Bold size={16} />
              </button>
            </EditorBubbleItem>
            <EditorBubbleItem
              onSelect={(editor) => editor.chain().focus().toggleItalic().run()}
            >
              <button
                type="button"
                className="p-2 text-muted-foreground hover:text-foreground transition-colors"
              >
                <Italic size={16} />
              </button>
            </EditorBubbleItem>
            <EditorBubbleItem
              onSelect={(editor) => editor.chain().focus().toggleStrike().run()}
            >
              <button
                type="button"
                className="p-2 text-muted-foreground hover:text-foreground transition-colors"
              >
                <Strikethrough size={16} />
              </button>
            </EditorBubbleItem>
            <EditorBubbleItem
              onSelect={(editor) => editor.chain().focus().toggleCode().run()}
            >
              <button
                type="button"
                className="p-2 text-muted-foreground hover:text-foreground transition-colors"
              >
                <Code size={16} />
              </button>
            </EditorBubbleItem>
          </EditorBubble>
        </EditorContent>
      </EditorRoot>
    </div>
  );
}
