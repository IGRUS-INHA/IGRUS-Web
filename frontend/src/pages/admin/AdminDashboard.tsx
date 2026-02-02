import { useState } from 'react';
import { useUIStore } from '@/stores';
import {
  Users,
  FileText,
  BarChart2,
  AlertCircle,
  CheckCircle,
  XCircle,
  Search,
} from 'lucide-react';
import { cn } from '@/lib/utils';

type AdminTab = 'users' | 'approvals' | 'inquiries';

export default function AdminDashboard() {
  const { theme } = useUIStore();
  const isDark = theme === 'dark';
  const [activeTab, setActiveTab] = useState<AdminTab>('users');

  const renderStats = () => (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-s6 mb-12">
      {[
        {
          label: 'Visitors',
          value: '2.4k',
          icon: <BarChart2 size={24} />,
          color: 'text-blue-500',
        },
        {
          label: 'Active Members',
          value: '450',
          icon: <Users size={24} />,
          color: 'text-primary',
        },
        {
          label: 'Pending Inquiries',
          value: '8',
          icon: <AlertCircle size={24} />,
          color: 'text-orange-500',
        },
        {
          label: "Today's Posts",
          value: '24',
          icon: <FileText size={24} />,
          color: 'text-purple-500',
        },
      ].map((stat, i) => (
        <div
          key={i}
          className={cn(
            'p-s8 rounded-[2.5rem] border',
            isDark
              ? 'bg-[#1A1A1A] border-white/5'
              : 'bg-white border-gray-100 shadow-sm'
          )}
        >
          <div className={cn(stat.color, 'mb-s4')}>{stat.icon}</div>
          <p className="text-muted-foreground text-xs font-bold uppercase tracking-widest mb-1">
            {stat.label}
          </p>
          <h3 className="text-3xl font-bold">{stat.value}</h3>
        </div>
      ))}
    </div>
  );

  const renderContent = () => {
    switch (activeTab) {
      case 'users':
        return (
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="text-xs text-muted-foreground uppercase tracking-widest border-b border-white/5 dark:border-white/5">
                  <th className="pb-s4 font-bold">Student ID</th>
                  <th className="pb-s4 font-bold">Name</th>
                  <th className="pb-s4 font-bold">Status</th>
                  <th className="pb-s4 font-bold">Role</th>
                  <th className="pb-s4 font-bold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-white/5">
                {[
                  {
                    id: '20230001',
                    name: 'Kim Min-su',
                    status: 'Active',
                    role: 'Member',
                  },
                  {
                    id: '20210542',
                    name: 'Lee Ha-na',
                    status: 'Active',
                    role: 'Admin',
                  },
                  {
                    id: '20240122',
                    name: 'Park Jun-ho',
                    status: 'Suspended',
                    role: 'Member',
                  },
                ].map((user, i) => (
                  <tr key={i} className="group">
                    <td className="py-s4 text-sm font-medium">{user.id}</td>
                    <td className="py-s4 text-sm font-bold">{user.name}</td>
                    <td className="py-s4">
                      <span
                        className={cn(
                          'px-s2 py-1 rounded-md text-[10px] font-bold',
                          user.status === 'Active'
                            ? 'bg-green-500/10 text-green-500'
                            : 'bg-red-500/10 text-red-500'
                        )}
                      >
                        {user.status}
                      </span>
                    </td>
                    <td className="py-s4 text-sm text-muted-foreground">{user.role}</td>
                    <td className="py-s4 text-right">
                      <button
                        className="text-primary hover:underline text-xs font-bold"
                        type="button"
                      >
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
      case 'approvals':
        return (
          <div className="space-y-s4">
            {[
              {
                id: '20249999',
                name: 'New Student 1',
                date: '2024-05-25',
                intro: 'Hi, I love coding!',
              },
              {
                id: '20248888',
                name: 'New Student 2',
                date: '2024-05-24',
                intro: 'Interested in UI/UX.',
              },
            ].map((app, i) => (
              <div
                key={i}
                className={cn(
                  'p-s6 rounded-r4 border flex items-center justify-between',
                  isDark
                    ? 'bg-white/5 border-white/5'
                    : 'bg-gray-50 border-gray-100'
                )}
              >
                <div>
                  <h4 className="font-bold">
                    {app.name}{' '}
                    <span className="text-muted-foreground text-xs font-normal">
                      ({app.id})
                    </span>
                  </h4>
                  <p className="text-xs text-muted-foreground mt-1">{app.intro}</p>
                </div>
                <div className="flex gap-s2">
                  <button
                    className="p-s2 text-green-500 hover:bg-green-500/10 rounded-r2 transition"
                    type="button"
                  >
                    <CheckCircle size={20} />
                  </button>
                  <button
                    className="p-s2 text-red-500 hover:bg-red-500/10 rounded-r2 transition"
                    type="button"
                  >
                    <XCircle size={20} />
                  </button>
                </div>
              </div>
            ))}
            {/* Empty State */}
            <div className="hidden text-center py-10 text-muted-foreground text-sm">
              No pending approvals.
            </div>
          </div>
        );
      case 'inquiries':
        return (
          <div className="space-y-s4">
            {[
              {
                id: '1',
                title: 'Question about dues',
                author: 'member@test.com',
                date: '2024-05-23',
                status: 'Pending',
              },
              {
                id: '2',
                title: 'Room reservation issue',
                author: 'active@test.com',
                date: '2024-05-22',
                status: 'Answered',
              },
            ].map((inq, i) => (
              <div
                key={i}
                className={cn(
                  'p-s6 rounded-r4 border flex items-center justify-between',
                  isDark
                    ? 'bg-white/5 border-white/5'
                    : 'bg-gray-50 border-gray-100'
                )}
              >
                <div>
                  <div className="flex items-center gap-s2 mb-1">
                    <span
                      className={cn(
                        'w-2 h-2 rounded-full',
                        inq.status === 'Pending' ? 'bg-orange-500' : 'bg-green-500'
                      )}
                    />
                    <span className="text-xs text-muted-foreground font-bold uppercase">
                      {inq.status}
                    </span>
                  </div>
                  <h4 className="font-bold">{inq.title}</h4>
                  <p className="text-xs text-muted-foreground mt-1">
                    From: {inq.author} • {inq.date}
                  </p>
                </div>
                <button
                  className="px-s4 py-s2 text-xs font-bold bg-primary text-white rounded-r2"
                  type="button"
                >
                  Reply
                </button>
              </div>
            ))}
          </div>
        );
    }
  };

  return (
    <div className="space-y-s8 animate-in fade-in duration-300">
      {renderStats()}

      <div
        className={cn(
          'p-s8 rounded-[2.5rem] border',
          isDark ? 'bg-[#1A1A1A] border-white/10' : 'bg-white border-gray-100 shadow-sm'
        )}
      >
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-s8 gap-s4">
          <div className="flex gap-s2 overflow-x-auto">
            {(['users', 'approvals', 'inquiries'] as AdminTab[]).map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={cn(
                  'px-s4 py-s2 rounded-r3 text-xs font-bold uppercase tracking-widest transition-all',
                  activeTab === tab
                    ? 'bg-primary text-white'
                    : isDark
                      ? 'text-gray-400 hover:bg-white/5'
                      : 'text-muted-foreground hover:bg-gray-100'
                )}
                type="button"
              >
                {tab}
              </button>
            ))}
          </div>

          <div className="relative">
            <Search
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <input
              type="text"
              placeholder="Search..."
              className={cn(
                'pl-10 pr-4 py-s2 rounded-r3 text-sm border focus:outline-none focus:border-primary',
                isDark
                  ? 'bg-white/5 border-white/10 text-white'
                  : 'bg-gray-50 border-gray-200'
              )}
            />
          </div>
        </div>

        {renderContent()}
      </div>
    </div>
  );
}
