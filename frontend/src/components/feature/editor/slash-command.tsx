import {
  CheckSquare,
  Code,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Text,
  TextQuote,
  Minus,
} from "lucide-react";
import { createSuggestionItems, Command, renderItems } from "novel";

export const suggestionItems = createSuggestionItems([
  {
    title: "텍스트",
    description: "일반 텍스트를 입력합니다.",
    searchTerms: ["p", "paragraph", "text"],
    icon: <Text size={18} />,
    command: ({ editor, range }) => {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .toggleNode("paragraph", "paragraph")
        .run();
    },
  },
  {
    title: "제목 1",
    description: "큰 제목입니다.",
    searchTerms: ["title", "big", "large", "h1", "heading"],
    icon: <Heading1 size={18} />,
    command: ({ editor, range }) => {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .setNode("heading", { level: 1 })
        .run();
    },
  },
  {
    title: "제목 2",
    description: "중간 제목입니다.",
    searchTerms: ["subtitle", "medium", "h2", "heading"],
    icon: <Heading2 size={18} />,
    command: ({ editor, range }) => {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .setNode("heading", { level: 2 })
        .run();
    },
  },
  {
    title: "제목 3",
    description: "작은 제목입니다.",
    searchTerms: ["subtitle", "small", "h3", "heading"],
    icon: <Heading3 size={18} />,
    command: ({ editor, range }) => {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .setNode("heading", { level: 3 })
        .run();
    },
  },
  {
    title: "글머리 기호 목록",
    description: "글머리 기호 목록을 만듭니다.",
    searchTerms: ["unordered", "point", "bullet", "list"],
    icon: <List size={18} />,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleBulletList().run();
    },
  },
  {
    title: "번호 목록",
    description: "번호가 매겨진 목록을 만듭니다.",
    searchTerms: ["ordered", "number", "list"],
    icon: <ListOrdered size={18} />,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleOrderedList().run();
    },
  },
  {
    title: "할 일 목록",
    description: "체크박스가 있는 할 일 목록입니다.",
    searchTerms: ["todo", "task", "check", "checkbox"],
    icon: <CheckSquare size={18} />,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleTaskList().run();
    },
  },
  {
    title: "인용",
    description: "인용문을 추가합니다.",
    searchTerms: ["blockquote", "quote"],
    icon: <TextQuote size={18} />,
    command: ({ editor, range }) => {
      editor
        .chain()
        .focus()
        .deleteRange(range)
        .toggleNode("paragraph", "paragraph")
        .toggleBlockquote()
        .run();
    },
  },
  {
    title: "코드",
    description: "코드 블록을 추가합니다.",
    searchTerms: ["codeblock", "code"],
    icon: <Code size={18} />,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).toggleCodeBlock().run();
    },
  },
  {
    title: "구분선",
    description: "구분선을 추가합니다.",
    searchTerms: ["hr", "divider", "separator", "horizontal"],
    icon: <Minus size={18} />,
    command: ({ editor, range }) => {
      editor.chain().focus().deleteRange(range).setHorizontalRule().run();
    },
  },
]);

export const slashCommand = Command.configure({
  suggestion: {
    items: () => suggestionItems,
    render: renderItems,
  },
});
