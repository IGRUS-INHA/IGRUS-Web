import { useState, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users, List, BarChart3 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { FullPageSpinner } from "@/components/ui";
import { Pagination } from "@/components/board/Pagination";
import { useUIStore } from "@/stores";
import { cn } from "@/lib/utils";
import { useEvent } from "@/hooks/queries/useEvents";
import {
  useRegistrationList,
  useApproveEventRegistration,
  useRejectEventRegistration,
  useRevertEventRegistration,
} from "@/hooks/queries/useEventRegistrations";
import {
  isForbiddenError,
  isEventAccessDenied,
  getErrorMessage,
} from "@/utils/error";
import { formatDate } from "@/utils/date";
import RegistrationChart from "@/components/feature/event/RegistrationChart";
import {
  aggregateByGender,
  aggregateByGrade,
  aggregateByDepartment,
  aggregateByStatus,
} from "@/utils/chart";
import type { RegistrationListResponse } from "@/api/model/models";

type ActiveTab = "list" | "dashboard";

const PAGE_SIZE = 20;

const STATUS_BADGE: Record<string, { label: string; className: string }> = {
  REGISTERED: { label: "등록", className: "bg-primary/10 text-primary" },
  WAITING: { label: "대기", className: "bg-yellow-500/10 text-yellow-600" },
  APPROVED: { label: "승인", className: "bg-success/10 text-success" },
  REJECTED: { label: "거절", className: "bg-destructive/10 text-destructive" },
  CANCELED: { label: "취소", className: "bg-muted text-muted-foreground" },
};

const REGISTRATION_TYPE_LABEL: Record<string, string> = {
  AUTO_APPROVE: "선착순",
  MANUAL_APPROVE: "선발제",
};

const TABS: { key: ActiveTab; label: string; icon: typeof List }[] = [
  { key: "list", label: "리스트", icon: List },
  { key: "dashboard", label: "대시보드", icon: BarChart3 },
];

export default function EventRegistrationsPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const navigate = useNavigate();
  const addToast = useUIStore((s) => s.addToast);
  const numericEventId = Number(eventId);

  const [activeTab, setActiveTab] = useState<ActiveTab>("list");
  const [page, setPage] = useState(1);

  const {
    data: eventResponse,
    isLoading: eventLoading,
    error: eventError,
  } = useEvent(numericEventId);

  const { data: registrationsResponse, isLoading: regLoading } =
    useRegistrationList(numericEventId, {
      page: 0,
      size: 100,
      sort: ["registeredAt,DESC"],
    });

  const { mutate: approve, isPending: isApproving } =
    useApproveEventRegistration(numericEventId);
  const { mutate: reject, isPending: isRejecting } =
    useRejectEventRegistration(numericEventId);
  const { mutate: revert, isPending: isReverting } =
    useRevertEventRegistration(numericEventId);
  const isBusy = isApproving || isRejecting || isReverting;

  const event = eventResponse?.data;
  const allRegistrations =
    registrationsResponse?.status === 200
      ? (registrationsResponse.data.content ?? [])
      : [];
  const isManualApprove = event?.registrationType === "MANUAL_APPROVE";

  // 페이지네이션
  const totalPages = Math.ceil(allRegistrations.length / PAGE_SIZE);
  const paginatedRegistrations = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return allRegistrations.slice(start, start + PAGE_SIZE);
  }, [allRegistrations, page]);

  // 차트 데이터
  const genderData = useMemo(
    () => aggregateByGender(allRegistrations),
    [allRegistrations],
  );
  const gradeData = useMemo(
    () => aggregateByGrade(allRegistrations),
    [allRegistrations],
  );
  const departmentData = useMemo(
    () => aggregateByDepartment(allRegistrations),
    [allRegistrations],
  );
  const statusData = useMemo(
    () => aggregateByStatus(allRegistrations),
    [allRegistrations],
  );

  const isLoading = eventLoading || regLoading;

  // 승인/거절 핸들러
  const handleApprove = (registrationId: number) => {
    approve(
      { registrationId },
      {
        onSuccess: () => addToast({ type: "success", message: "승인 완료" }),
        onError: (error: unknown) => {
          addToast({
            type: "error",
            message: isForbiddenError(error)
              ? "승인 권한이 없습니다."
              : getErrorMessage(error),
          });
        },
      },
    );
  };

  const handleReject = (registrationId: number) => {
    reject(
      { registrationId },
      {
        onSuccess: () => addToast({ type: "success", message: "거절 완료" }),
        onError: (error: unknown) => {
          addToast({
            type: "error",
            message: isForbiddenError(error)
              ? "거절 권한이 없습니다."
              : getErrorMessage(error),
          });
        },
      },
    );
  };

  const handleRevert = (registrationId: number) => {
    revert(
      { registrationId },
      {
        onSuccess: () => addToast({ type: "success", message: "취소 완료" }),
        onError: (error: unknown) => {
          addToast({
            type: "error",
            message: isForbiddenError(error)
              ? "권한이 없습니다."
              : getErrorMessage(error),
          });
        },
      },
    );
  };

  const renderActions = (r: RegistrationListResponse) => {
    if (!isManualApprove || !r.registrationId) return null;

    if (r.status === "WAITING") {
      return (
        <div className="flex gap-s2 justify-end">
          <Button
            size="xs"
            onClick={() => handleApprove(r.registrationId!)}
            disabled={isBusy}
          >
            승인
          </Button>
          <Button
            size="xs"
            variant="destructive"
            onClick={() => handleReject(r.registrationId!)}
            disabled={isBusy}
          >
            거절
          </Button>
        </div>
      );
    }

    if (r.status === "APPROVED") {
      return (
        <Button
          size="xs"
          variant="outline"
          onClick={() => handleRevert(r.registrationId!)}
          disabled={isBusy}
        >
          승인 취소
        </Button>
      );
    }

    if (r.status === "REJECTED") {
      return (
        <Button
          size="xs"
          variant="outline"
          onClick={() => handleRevert(r.registrationId!)}
          disabled={isBusy}
        >
          거절 취소
        </Button>
      );
    }

    return null;
  };

  if (isLoading) return <FullPageSpinner />;

  if (isForbiddenError(eventError) || isEventAccessDenied(eventError)) {
    return (
      <div className="text-center py-12 space-y-s4">
        <p className="text-muted-foreground">신청자 관리 권한이 없습니다.</p>
        <button
          type="button"
          onClick={() => navigate("/events")}
          className="text-sm text-primary hover:underline cursor-pointer"
        >
          행사 목록으로 돌아가기
        </button>
      </div>
    );
  }

  if (!event) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        행사를 찾을 수 없습니다.
      </div>
    );
  }

  return (
    <div className="animate-in slide-in-from-right-8 duration-300">
      {/* 뒤로가기 */}
      <button
        type="button"
        onClick={() => navigate("/events")}
        className="mb-s6 flex items-center gap-s2 text-sm font-bold transition-colors text-muted-foreground hover:text-foreground cursor-pointer"
      >
        <ArrowLeft size={18} /> 행사 목록으로 돌아가기
      </button>

      {/* 헤더 */}
      <div className="mb-s6 space-y-s2">
        <div className="flex items-center gap-s3">
          <Users size={24} className="text-primary" />
          <h1 className="typo-h2 font-bold">신청자 관리</h1>
        </div>
        <div className="flex items-center gap-s3 flex-wrap">
          <h2 className="typo-b1 text-muted-foreground">{event.title}</h2>
          {event.registrationType && (
            <span className="px-s2 py-0.5 rounded-r2 typo-c2 font-bold bg-primary/10 text-primary">
              {REGISTRATION_TYPE_LABEL[event.registrationType] ??
                event.registrationType}
            </span>
          )}
          <span className="typo-b2 text-muted-foreground">
            {event.currentCount ?? 0} / {event.capacity ?? 0}명
          </span>
        </div>
      </div>

      {/* 탭 */}
      <div className="flex gap-s2 mb-s6 border-b border-border pb-s2">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            type="button"
            onClick={() => setActiveTab(key)}
            className={cn(
              "flex items-center gap-s2 px-s4 py-s3 rounded-r3 text-sm font-medium transition-all cursor-pointer",
              activeTab === key
                ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20"
                : "text-muted-foreground hover:text-foreground hover:bg-muted",
            )}
          >
            <Icon size={16} />
            {label}
          </button>
        ))}
      </div>

      {/* 탭 콘텐츠 */}
      <div className="animate-in fade-in duration-200">
        {activeTab === "list" ? (
          <>
            {/* 테이블 */}
            <Card className="p-s5 overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
                    <th className="pb-s4 font-bold">학번</th>
                    <th className="pb-s4 font-bold">이름</th>
                    <th className="pb-s4 font-bold hidden lg:table-cell">
                      이메일
                    </th>
                    <th className="pb-s4 font-bold">상태</th>
                    <th className="pb-s4 font-bold">신청일</th>
                    {isManualApprove && (
                      <th className="pb-s4 font-bold text-right">작업</th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {paginatedRegistrations.map((r) => {
                    const badge = r.status ? STATUS_BADGE[r.status] : undefined;
                    return (
                      <tr key={r.registrationId}>
                        <td className="py-s4 typo-b2 font-medium">
                          {r.studentId ?? "-"}
                        </td>
                        <td className="py-s4 typo-b2 font-bold">
                          {r.userName ?? "-"}
                        </td>
                        <td className="py-s4 typo-b2 text-muted-foreground hidden lg:table-cell">
                          {r.userEmail ?? "-"}
                        </td>
                        <td className="py-s4">
                          {badge ? (
                            <span
                              className={cn(
                                "px-s2 py-0.5 rounded-r2 typo-c2 font-bold",
                                badge.className,
                              )}
                            >
                              {badge.label}
                            </span>
                          ) : (
                            <span className="typo-c2 text-muted-foreground">
                              -
                            </span>
                          )}
                        </td>
                        <td className="py-s4 typo-b2 text-muted-foreground">
                          {formatDate(r.registeredAt)}
                        </td>
                        {isManualApprove && (
                          <td className="py-s4 text-right">
                            {renderActions(r)}
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              {allRegistrations.length === 0 && (
                <div className="text-center py-12 text-muted-foreground">
                  신청자가 없습니다.
                </div>
              )}
            </Card>

            {totalPages > 1 && (
              <div className="mt-s6">
                <Pagination
                  currentPage={page}
                  totalPages={totalPages}
                  onPageChange={setPage}
                />
              </div>
            )}
          </>
        ) : (
          <>
            {allRegistrations.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                신청자가 없어 통계를 표시할 수 없습니다.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-s5">
                <RegistrationChart
                  title="성별"
                  totalCount={allRegistrations.length}
                  data={genderData}
                  type="pie"
                />
                <RegistrationChart
                  title="신청 상태"
                  totalCount={allRegistrations.length}
                  data={statusData}
                  type="pie"
                />
                <RegistrationChart
                  title="학년"
                  totalCount={allRegistrations.length}
                  data={gradeData}
                  type="bar"
                />
                <RegistrationChart
                  title="학과"
                  totalCount={allRegistrations.length}
                  data={departmentData}
                  type="bar"
                />
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
