import {
  FileText,
  MessageCircle,
  UserCheck,
  HelpCircle,
  UserPlus,
} from "lucide-react";
import { useGetDashboardStats } from "@/api/model/admin-dashboard/admin-dashboard";
import StatCard from "@/components/feature/admin/StatCard";

export default function DashboardTab() {
  const { data: response, isLoading } = useGetDashboardStats();
  const stats = response?.status === 200 ? response.data : undefined;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-s6">
      <StatCard
        label="오늘 게시글"
        value={stats?.todayPostCount ?? 0}
        icon={<FileText size={24} />}
        colorClass="text-primary"
      />
      <StatCard
        label="오늘 댓글"
        value={stats?.todayCommentCount ?? 0}
        icon={<MessageCircle size={24} />}
        colorClass="text-info"
      />
      <StatCard
        label="이번 주 정회원 승인"
        value={stats?.weeklyApprovedMemberCount ?? 0}
        icon={<UserCheck size={24} />}
        colorClass="text-success"
      />
      <StatCard
        label="대기 중 문의"
        value={stats?.pendingInquiryCount ?? 0}
        icon={<HelpCircle size={24} />}
        colorClass="text-warning"
      />
      <StatCard
        label="승인 대기 준회원"
        value={stats?.pendingAssociateCount ?? 0}
        icon={<UserPlus size={24} />}
        colorClass="text-destructive"
      />
    </div>
  );
}
