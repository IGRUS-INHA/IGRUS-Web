import { useUIStore } from '@/stores';
import { User, Mail, Calendar, Edit3, Shield } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { ROLE_LABELS } from '@/constants';

export default function ProfileHeader({ user }) {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';

  return (
    <Card
      className={`p-s6 lg:p-s7 rounded-[2.5rem] border flex flex-col md:flex-row items-center gap-s6 ${
        isDark
          ? 'bg-gradient-to-br from-card to-background border-border'
          : 'bg-card border-border shadow-xl shadow-black/5'
      }`}
    >
      <div className="relative">
        <div className="w-32 h-32 rounded-[2.5rem] bg-primary/20 border border-primary/50 flex items-center justify-center">
          <User size={64} className="text-primary" />
        </div>
        <button className="absolute -bottom-2 -right-2 w-10 h-10 bg-foreground text-background rounded-r4 flex items-center justify-center hover:bg-primary transition border-2 border-background">
          <Edit3 size={16} />
        </button>
      </div>

      <div className="flex-1 text-center md:text-left">
        <div className="flex flex-col md:flex-row items-center gap-s3 mb-2">
          <h2 className="text-h1">{user.name}</h2>
          <span className="px-3 py-1 bg-primary/20 text-primary rounded-full text-c2 font-bold uppercase tracking-widest border border-primary/30">
            {ROLE_LABELS[user.role]}
          </span>
        </div>
        <div className="flex flex-wrap justify-center md:justify-start gap-s5 text-muted-foreground text-b2">
          <div className="flex items-center gap-2">
            <Shield size={16} className="text-primary" />
            {user.studentId}
          </div>
          <div className="flex items-center gap-2">
            <Mail size={16} className="text-primary" />
            {user.email}
          </div>
          <div className="flex items-center gap-2">
            <Calendar size={16} className="text-primary" />
            가입일 {user.joinedDate}
          </div>
        </div>
      </div>

      <div className="flex gap-s3">
        <div className="text-center px-s5 py-s4 rounded-[2rem] bg-white/5 border border-border">
          <div className="text-h2 text-primary">{user.postCount || 0}</div>
          <div className="text-c2 text-muted-foreground uppercase font-bold tracking-widest">게시글</div>
        </div>
        <div className="text-center px-s5 py-s4 rounded-[2rem] bg-white/5 border border-border">
          <div className="text-h2 text-primary">{user.likeCount || 0}</div>
          <div className="text-c2 text-muted-foreground uppercase font-bold tracking-widest">좋아요</div>
        </div>
      </div>
    </Card>
  );
}
