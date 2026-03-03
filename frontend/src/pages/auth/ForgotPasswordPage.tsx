import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import Swal from "sweetalert2";
import { Lock, User, ArrowRight } from "lucide-react";
import { useRequestPasswordReset } from "@/api/model/password-authentication/password-authentication";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { useUIStore } from "@/stores";

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const { theme } = useUIStore();
  const isDark = theme === "dark";

  const [studentId, setStudentId] = useState("");
  const [loading, setLoading] = useState(false);

  const requestResetMutation = useRequestPasswordReset();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!studentId) {
      Swal.fire({
        icon: "warning",
        title: "학번 입력 필요",
        text: "학번을 입력해주세요.",
        confirmButtonText: "확인",
        confirmButtonColor: "#FFC107",
        showClass: { popup: "", backdrop: "" },
        hideClass: { popup: "", backdrop: "" },
      });
      return;
    }

    if (studentId.length !== 8 || !/^\d{8}$/.test(studentId)) {
      Swal.fire({
        icon: "warning",
        title: "학번 형식 오류",
        text: "학번은 8자리 숫자로 입력해주세요.",
        confirmButtonText: "확인",
        confirmButtonColor: "#FFC107",
        showClass: { popup: "", backdrop: "" },
        hideClass: { popup: "", backdrop: "" },
      });
      return;
    }

    setLoading(true);
    try {
      await requestResetMutation.mutateAsync({
        data: { studentId },
      });

      await Swal.fire({
        icon: "success",
        title: "발송 완료",
        html: "비밀번호 재설정 링크가 발송되었습니다.<br><br>등록된 이메일을 확인해주세요.",
        confirmButtonText: "확인",
        confirmButtonColor: "#28A745",
        showClass: { popup: "", backdrop: "" },
        hideClass: { popup: "", backdrop: "" },
      });

      // 재설정 페이지로 이동 (학번을 state로 전달)
      navigate("/reset-password", { state: { studentId } });
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "알 수 없는 오류";

      if (errorMessage.includes("사용자")) {
        Swal.fire({
          icon: "error",
          title: "학번 오류",
          html: "등록되지 않은 학번입니다.<br><br>학번을 확인해주세요.",
          confirmButtonText: "확인",
          confirmButtonColor: "#DC3545",
          showClass: { popup: "", backdrop: "" },
          hideClass: { popup: "", backdrop: "" },
        });
      } else if (errorMessage.includes("승인")) {
        Swal.fire({
          icon: "info",
          title: "승인 대기 중",
          html: "관리자 승인이 완료되지 않은 계정입니다.<br><br>승인 완료 후 이용 가능합니다.",
          confirmButtonText: "확인",
          confirmButtonColor: "#17A2B8",
          showClass: { popup: "", backdrop: "" },
          hideClass: { popup: "", backdrop: "" },
        });
      } else {
        Swal.fire({
          icon: "error",
          title: "요청 실패",
          html: "비밀번호 재설정 요청에 실패했습니다.<br><br>다시 시도해주세요.",
          confirmButtonText: "확인",
          confirmButtonColor: "#DC3545",
          showClass: { popup: "", backdrop: "" },
          hideClass: { popup: "", backdrop: "" },
        });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center h-full">
      <div className="max-w-md w-full animate-in slide-in-from-bottom-8 duration-500">
        <Card
          className={`p-s6 lg:p-s7 rounded-r4 border ${isDark ? "bg-card" : "bg-card shadow-xl"}`}
        >
          <CardContent className="p-0">
            <div className="text-center mb-s6">
              <div className="w-16 h-16 bg-primary/20 rounded-r4 flex items-center justify-center mx-auto mb-s5">
                <Lock size={32} className="text-primary" />
              </div>
              <h2 className="typo-h2 mb-s2">비밀번호 찾기</h2>
              <p className="text-muted-foreground typo-b2">
                학번을 입력하시면 등록된 이메일로 비밀번호 재설정 링크를
                보내드립니다.
              </p>
              <p className="text-sm text-muted-foreground mt-s1">
                이메일이 오지 않으면 스팸 메일함을 확인해주세요.
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-s4">
              <div className="relative">
                <User
                  size={18}
                  className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="text"
                  placeholder="학번 (8자리)"
                  value={studentId}
                  onChange={(e) =>
                    setStudentId(e.target.value.replace(/\D/g, "").slice(0, 8))
                  }
                  required
                  maxLength={8}
                  className={`w-full rounded-r4 pl-12 pr-4 py-s6 border focus:border-primary transition-all ${
                    isDark
                      ? "bg-white/5 border-border"
                      : "bg-muted border-border"
                  }`}
                />
              </div>

              <Button
                type="submit"
                disabled={loading || studentId.length !== 8}
                className="w-full py-s6 rounded-r4 font-bold flex items-center justify-center gap-s2 shadow-lg shadow-primary/20"
              >
                {loading ? "요청 중..." : "재설정 링크 발송"}
                <ArrowRight size={18} />
              </Button>
            </form>

            <div className="mt-s5 text-center space-y-s3">
              <Link
                to="/login"
                className="typo-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                로그인 페이지로 이동
              </Link>
              <Link
                to="/signup"
                className="typo-c1 font-bold text-muted-foreground hover:text-primary transition uppercase tracking-widest block"
              >
                계정이 없으신가요? 회원가입
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
