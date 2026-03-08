import { useSearchParams } from "react-router-dom";
import DashboardTab from "./tabs/DashboardTab";
import UsersTab from "./tabs/UsersTab";
import AssociatesTab from "./tabs/AssociatesTab";
import InquiriesTab from "./tabs/InquiriesTab";
import ReportsTab from "./tabs/ReportsTab";
import LoginHistoryTab from "./tabs/LoginHistoryTab";
import SemestersTab from "./tabs/SemestersTab";

type TabKey =
  | "dashboard"
  | "users"
  | "associates"
  | "inquiries"
  | "reports"
  | "login-history"
  | "semesters";

const TAB_LABELS: Record<TabKey, string> = {
  dashboard: "대시보드",
  users: "회원관리",
  associates: "준회원 승인",
  inquiries: "문의 관리",
  reports: "댓글 신고",
  "login-history": "로그인 이력",
  semesters: "금학기 회원",
};

const TAB_COMPONENTS: Record<TabKey, React.ComponentType> = {
  dashboard: DashboardTab,
  users: UsersTab,
  associates: AssociatesTab,
  inquiries: InquiriesTab,
  reports: ReportsTab,
  "login-history": LoginHistoryTab,
  semesters: SemestersTab,
};

export default function AdminDashboard() {
  const [searchParams] = useSearchParams();
  const activeTab = (searchParams.get("tab") as TabKey) ?? "users";

  const ActiveComponent = TAB_COMPONENTS[activeTab] ?? DashboardTab;

  return (
    <div className="space-y-s6 animate-in fade-in duration-300">
      <h1 className="text-2xl font-bold pb-s4 border-b border-border">
        {TAB_LABELS[activeTab]}
      </h1>
      <ActiveComponent />
    </div>
  );
}
