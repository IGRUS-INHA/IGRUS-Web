import { Link } from 'react-router-dom';
import { useUIStore } from '@/stores';
import { MessageCircle, Heart, Bookmark, EyeOff, HelpCircle } from 'lucide-react';
import { Card } from '@/components/ui/card';

export default function PostListItem({ post, linkTo }) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  const content = (
    <Card
      className={`p-s5 rounded-[2rem] border transition-all hover:border-primary/50 group cursor-pointer ${
        isDark ? 'bg-card border-border' : 'bg-card border-border shadow-sm'
      }`}
    >
      <div className="flex justify-between items-start mb-s4">
        <div className="flex items-center gap-3">
          <span
            className={`px-3 py-1 rounded-full text-c2 font-bold uppercase tracking-widest ${
              isDark ? 'bg-white/5 text-muted-foreground' : 'bg-muted text-muted-foreground'
            }`}
          >
            {post.category}
          </span>
          {post.isAnonymous && (
            <span title="익명">
              <EyeOff size={14} className="text-muted-foreground" />
            </span>
          )}
          {post.isQuestion && (
            <span title="질문">
              <HelpCircle size={14} className="text-primary" />
            </span>
          )}
        </div>
        <p className="text-c1 text-muted-foreground">{post.date}</p>
      </div>

      <h3 className="text-h3 mb-2 group-hover:text-primary transition-colors">{post.title}</h3>
      <p className="text-b2 mb-s5 line-clamp-2 text-muted-foreground">{post.content}</p>

      <div className="flex justify-between items-center">
        <div className="flex items-center gap-2">
          <div
            className={`w-6 h-6 rounded-full flex items-center justify-center text-c2 font-bold ${
              isDark ? 'bg-white/10' : 'bg-muted'
            }`}
          >
            {post.author[0]}
          </div>
          <span className="text-c1 font-medium text-muted-foreground">{post.author}</span>
        </div>

        <div className="flex items-center gap-s4 text-muted-foreground">
          <div className="flex items-center gap-1.5 hover:text-primary transition-colors">
            <Heart size={16} />
            <span className="text-c1">{post.likes}</span>
          </div>
          <div className="flex items-center gap-1.5 hover:text-primary transition-colors">
            <MessageCircle size={16} />
            <span className="text-c1">{post.comments}</span>
          </div>
          <Bookmark size={16} className="hover:text-primary transition-colors" />
        </div>
      </div>
    </Card>
  );

  if (linkTo) {
    return <Link to={linkTo}>{content}</Link>;
  }

  return content;
}
