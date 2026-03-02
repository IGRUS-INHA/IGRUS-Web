import { useState } from "react";
import {
  useGetCandidateMembers,
  useRegisterMembers,
  useRemoveMembers,
} from "@/api/model/admin-semester-member/admin-semester-member";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useUIStore } from "@/stores";
import { cn } from "@/lib/utils";
import { useQueryClient } from "@tanstack/react-query";

const currentYear = new Date().getFullYear();
const currentSemester = new Date().getMonth() < 7 ? 1 : 2;

const ROLE_LABELS: Record<string, string> = {
  ADMIN: "관리자",
  OPERATOR: "운영진",
  MEMBER: "정회원",
  ASSOCIATE: "준회원",
};

export default function SemestersTab() {
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();

  const [year, setYear] = useState(currentYear);
  const [semester, setSemester] = useState(currentSemester);
  const [selected, setSelected] = useState<number[]>([]);

  const { data: response, isLoading } = useGetCandidateMembers(year, semester);

  const { mutate: registerMembers, isPending: registering } =
    useRegisterMembers({
      mutation: {
        onSuccess: (res) => {
          const d = res.status === 200 ? res.data : undefined;
          addToast({
            type: "success",
            title: "등록 완료",
            message: `${d?.registeredCount ?? 0}명 등록됨`,
          });
          setSelected([]);
          queryClient.invalidateQueries({
            queryKey: [
              `/api/v1/admin/semesters/${year}/${semester}/candidates`,
            ],
          });
        },
        onError: () => {
          addToast({ type: "error", message: "등록 실패" });
        },
      },
    });

  const { mutate: removeMembers, isPending: removing } = useRemoveMembers({
    mutation: {
      onSuccess: (res) => {
        const count = res.status === 200 ? res.data : 0;
        addToast({
          type: "success",
          title: "제외 완료",
          message: `${count}명 제외됨`,
        });
        setSelected([]);
        queryClient.invalidateQueries({
          queryKey: [`/api/v1/admin/semesters/${year}/${semester}/candidates`],
        });
      },
      onError: () => {
        addToast({ type: "error", message: "제외 실패" });
      },
    },
  });

  const candidates = response?.status === 200 ? (response.data ?? []) : [];
  const isBusy = registering || removing;

  const toggleSelect = (userId: number) => {
    setSelected((prev) =>
      prev.includes(userId)
        ? prev.filter((id) => id !== userId)
        : [...prev, userId],
    );
  };

  const toggleAll = () => {
    const allIds = candidates.map((c) => c.userId!).filter(Boolean);
    setSelected((prev) => (prev.length === allIds.length ? [] : allIds));
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <div className="text-muted-foreground">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-s6">
      {/* Year/Semester selector */}
      <Card className="p-s5">
        <div className="flex gap-s4 items-center">
          <div className="flex gap-s2 items-center">
            <label className="text-sm font-medium">연도:</label>
            <select
              value={year}
              onChange={(e) => {
                setYear(Number(e.target.value));
                setSelected([]);
              }}
              className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
            >
              {Array.from({ length: 5 }, (_, i) => currentYear - 2 + i).map(
                (y) => (
                  <option key={y} value={y}>
                    {y}
                  </option>
                ),
              )}
            </select>
          </div>
          <div className="flex gap-s2 items-center">
            <label className="text-sm font-medium">학기:</label>
            <select
              value={semester}
              onChange={(e) => {
                setSemester(Number(e.target.value));
                setSelected([]);
              }}
              className="px-s3 py-s2 rounded-r2 border border-border bg-background text-sm"
            >
              <option value={1}>1학기</option>
              <option value={2}>2학기</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Bulk actions */}
      {candidates.length > 0 && (
        <div className="flex gap-s3 items-center">
          <span className="text-sm text-muted-foreground">
            {selected.length}명 선택
          </span>
          <Button
            size="sm"
            onClick={() =>
              registerMembers({ year, semester, data: { userIds: selected } })
            }
            disabled={selected.length === 0 || isBusy}
          >
            일괄 등록
          </Button>
          <Button
            size="sm"
            variant="destructive"
            onClick={() => {
              if (confirm("선택한 회원을 제외하시겠습니까?")) {
                removeMembers({ year, semester, data: { userIds: selected } });
              }
            }}
            disabled={selected.length === 0 || isBusy}
          >
            일괄 제외
          </Button>
        </div>
      )}

      {/* Candidates Table */}
      <Card className="p-s5 overflow-x-auto">
        <table className="w-full text-left">
          <thead>
            <tr className="typo-c1 text-muted-foreground uppercase tracking-widest border-b border-border">
              <th className="pb-s4">
                <input
                  type="checkbox"
                  checked={
                    selected.length === candidates.length &&
                    candidates.length > 0
                  }
                  onChange={toggleAll}
                  className="accent-primary"
                />
              </th>
              <th className="pb-s4 font-bold">학번</th>
              <th className="pb-s4 font-bold">이름</th>
              <th className="pb-s4 font-bold">역할</th>
              <th className="pb-s4 font-bold hidden lg:table-cell">
                등록 여부
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {candidates.map((c) => (
              <tr key={c.userId}>
                <td className="py-s4">
                  <input
                    type="checkbox"
                    checked={selected.includes(c.userId!)}
                    onChange={() => toggleSelect(c.userId!)}
                    className="accent-primary"
                  />
                </td>
                <td className="py-s4 typo-b2 font-medium">{c.studentId}</td>
                <td className="py-s4 typo-b2 font-bold">{c.name}</td>
                <td className="py-s4 typo-b2">
                  {ROLE_LABELS[c.role ?? ""] ?? c.role}
                </td>
                <td className="py-s4 hidden lg:table-cell">
                  <span
                    className={cn(
                      "px-2 py-1 rounded-r2 typo-c2 font-bold",
                      c.alreadyRegistered
                        ? "bg-success/10 text-success"
                        : "bg-muted text-muted-foreground",
                    )}
                  >
                    {c.alreadyRegistered ? "등록됨" : "미등록"}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {candidates.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">
            등록 후보 회원이 없습니다.
          </div>
        )}
      </Card>
    </div>
  );
}
