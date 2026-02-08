import { useSearchParams } from 'react-router-dom';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  Users,
  UserCheck,
  MessageSquare,
  Flag,
  History,
  GraduationCap,
} from 'lucide-react';
import DashboardTab from './tabs/DashboardTab';
import UsersTab from './tabs/UsersTab';
import AssociatesTab from './tabs/AssociatesTab';
import InquiriesTab from './tabs/InquiriesTab';
import ReportsTab from './tabs/ReportsTab';
import LoginHistoryTab from './tabs/LoginHistoryTab';
import SemestersTab from './tabs/SemestersTab';

const TABS = [
  { key: 'dashboard', label: '대시보드', icon: LayoutDashboard },
  { key: 'users', label: '회원 관리', icon: Users },
  { key: 'associates', label: '준회원 승인', icon: UserCheck },
  { key: 'inquiries', label: '문의 관리', icon: MessageSquare },
  { key: 'reports', label: '댓글 신고', icon: Flag },
  { key: 'login-history', label: '로그인 이력', icon: History },
  { key: 'semesters', label: '금학기 회원', icon: GraduationCap },
] as const;

type TabKey = (typeof TABS)[number]['key'];

const TAB_COMPONENTS: Record<TabKey, React.ComponentType> = {
  dashboard: DashboardTab,
  users: UsersTab,
  associates: AssociatesTab,
  inquiries: InquiriesTab,
  reports: ReportsTab,
  'login-history': LoginHistoryTab,
  semesters: SemestersTab,
};

export default function AdminDashboard() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = (searchParams.get('tab') as TabKey) ?? 'dashboard';

  const handleTabChange = (tab: TabKey) => {
    const newParams = new URLSearchParams(searchParams);
    if (tab === 'dashboard') {
      newParams.delete('tab');
    } else {
      newParams.set('tab', tab);
    }
    setSearchParams(newParams);
  };

  const ActiveComponent = TAB_COMPONENTS[activeTab] ?? DashboardTab;

  return (
    <div className="space-y-s6 animate-in fade-in duration-300">
      {/* Tab Navigation */}
      <div className="flex gap-s2 overflow-x-auto pb-s2 border-b border-border scrollbar-hide">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => handleTabChange(key)}
            className={cn(
              'flex items-center gap-s2 px-s4 py-s3 rounded-r3 text-sm font-medium whitespace-nowrap transition-all cursor-pointer',
              activeTab === key
                ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            )}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="animate-in fade-in duration-200">
        <ActiveComponent />
      </div>
    </div>
  );
}
