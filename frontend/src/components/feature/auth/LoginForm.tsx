import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { User, Lock, ArrowRight, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface LoginFormData {
  studentId: string;
  password: string;
}

interface LoginFormProps {
  onSubmit?: (data: LoginFormData) => void;
  loading?: boolean;
  errors?: {
    studentId?: string;
    password?: string;
  };
}

export default function LoginForm({
  onSubmit,
  loading = false,
  errors = {},
}: LoginFormProps) {
  const [studentId, setStudentId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    onSubmit?.({ studentId, password });
  };

  return (
    <div className="overflow-hidden rounded-r4 border border-border bg-card shadow-2xl">
      <div className="grid min-h-[520px] grid-cols-1 lg:grid-cols-5">
        {/* Brand Panel */}
        <div className="login-brand-panel relative flex flex-col justify-between overflow-hidden p-s6 lg:col-span-2 lg:p-s7">
          {/* Decorative circles */}
          <div className="pointer-events-none absolute inset-0">
            <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full border-2 border-primary/20" />
            <div className="absolute -left-8 top-1/3 h-24 w-24 rounded-full border border-primary/15" />
            <div className="absolute -bottom-6 right-1/4 h-32 w-32 rounded-full border border-primary/10" />
            {/* Dot grid overlay */}
            <div className="login-dot-grid absolute inset-0" />
          </div>

          {/* Brand content */}
          <div className="relative z-10">
            <div className="mb-s4 flex items-center gap-s3 lg:mb-s7">
              <img
                src="/igruslogo.png"
                alt="IGRUS"
                className="h-9 w-9 lg:h-11 lg:w-11"
              />
              <span className="text-lg font-bold tracking-tight text-primary">
                IGRUS
              </span>
            </div>

            <h1 className="mb-s2 hidden text-4xl font-extrabold leading-tight text-foreground lg:block">
              Welcome
            </h1>
            <p className="hidden text-foreground/80 typo-b2 lg:block">
              IGRUS에 오신 것을 환영합니다.
            </p>

            {/* Mobile-only compact text */}
            <p className="text-foreground/80 typo-b2 lg:hidden">
              IGRUS에 오신 것을 환영합니다.
            </p>
          </div>

          <p className="relative z-10 hidden text-foreground/60 typo-c1 lg:block">
            인하대학교 IT 동아리
          </p>
        </div>

        {/* Form Panel */}
        <div className="flex flex-col justify-center p-s6 lg:col-span-3 lg:p-s7">
          <div className="mb-s6">
            <h2 className="mb-s2 typo-h2">로그인</h2>
            <p className="text-muted-foreground typo-b2">
              학번과 비밀번호를 입력해주세요
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-s5">
            {/* Student ID */}
            <div>
              <label className="mb-s2 block text-muted-foreground typo-label">
                학번
              </label>
              <div className="relative">
                <User
                  size={18}
                  className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type="text"
                  placeholder="8자리 학번 입력"
                  value={studentId}
                  onChange={(e) => setStudentId(e.target.value)}
                  required
                  className={`w-full rounded-r3 border py-s5 pl-12 pr-s4 transition-all ${
                    errors.studentId
                      ? "border-destructive focus:border-destructive"
                      : "border-border focus:border-primary"
                  }`}
                />
              </div>
              {errors.studentId && (
                <p className="mt-s1 text-destructive typo-c1">
                  {errors.studentId}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="mb-s2 block text-muted-foreground typo-label">
                비밀번호
              </label>
              <div className="group relative">
                <Lock
                  size={18}
                  className="absolute left-s4 top-1/2 -translate-y-1/2 text-muted-foreground"
                />
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="영문, 숫자 포함 8자 이상"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className={`w-full rounded-r3 border py-s5 pl-12 pr-12 transition-all ${
                    errors.password
                      ? "border-destructive focus:border-destructive"
                      : "border-border focus:border-primary"
                  }`}
                />
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-s4 top-1/2 -translate-y-1/2 text-muted-foreground opacity-0 transition-all hover:text-foreground group-focus-within:opacity-100"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-s1 text-destructive typo-c1">
                  {errors.password}
                </p>
              )}
            </div>

            {/* Forgot password */}
            <div className="flex justify-end">
              <Link
                to="/forgot-password"
                className="text-primary transition hover:text-primary/80 typo-c1"
              >
                비밀번호를 잊으셨나요?
              </Link>
            </div>

            {/* Submit */}
            <Button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-s2 rounded-r3 py-s5 font-bold"
            >
              {loading ? "로그인 중..." : "로그인"}
              {!loading && <ArrowRight size={18} />}
            </Button>
          </form>

          {/* Sign up link */}
          <div className="mt-s6 text-center">
            <p className="text-muted-foreground typo-b2">
              아직 계정이 없으신가요?{" "}
              <Link
                to="/signup"
                className="font-semibold text-primary transition hover:text-primary/80"
              >
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
