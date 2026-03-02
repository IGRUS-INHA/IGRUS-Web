import { Link } from "react-router-dom";
import { useUIStore } from "@/stores";
import type { Post } from "@/types/entities";

interface PostCardProps {
  post: Pick<Post, "title" | "author" | "date" | "category"> & {
    image?: string;
    tag?: string;
  };
  linkTo?: string;
}

export default function PostCard({ post, linkTo }: PostCardProps) {
  const { theme } = useUIStore();
  const isDark = theme === "dark";

  const authorName =
    typeof post.author === "string" ? post.author : post.author.name;

  const content = (
    <div className="group cursor-pointer">
      <div
        className={`relative aspect-[4/3] rounded-r4 overflow-hidden mb-s3 transition-colors duration-300 ${
          isDark ? "bg-card" : "bg-muted"
        }`}
      >
        {post.image && (
          <img
            src={post.image}
            alt={post.title}
            className={`w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-in-out ${
              isDark ? "opacity-80" : "opacity-100"
            }`}
          />
        )}
        {post.tag && (
          <span className="absolute top-s4 right-s4 bg-background text-foreground typo-c2 font-bold px-s2 py-s1 rounded-r1 shadow-lg uppercase tracking-wider">
            {post.tag}
          </span>
        )}
        <div
          className={`absolute inset-0 transition-opacity duration-300 ${
            isDark
              ? "bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100"
              : "bg-black/5 opacity-0 group-hover:opacity-100"
          }`}
        />
      </div>
      <div className="flex justify-between items-start">
        <div className="flex-1 mr-s4">
          <h4 className="font-bold typo-b1 leading-tight mb-s1 transition-colors group-hover:text-primary">
            {post.title}
          </h4>
          <div className="flex items-center gap-s2">
            <p className="typo-c1 text-muted-foreground">{authorName}</p>
            <span className="w-1 h-1 rounded-full bg-muted-foreground" />
            <p className="typo-c1 text-muted-foreground">{post.date}</p>
          </div>
        </div>
        <span
          className={`typo-c2 border px-s2 py-s1 rounded-full whitespace-nowrap uppercase tracking-widest group-hover:border-primary/50 group-hover:text-primary transition-colors ${
            isDark
              ? "text-muted-foreground border-border"
              : "text-muted-foreground border-border"
          }`}
        >
          {post.category}
        </span>
      </div>
    </div>
  );

  if (linkTo) {
    return <Link to={linkTo}>{content}</Link>;
  }

  return content;
}
