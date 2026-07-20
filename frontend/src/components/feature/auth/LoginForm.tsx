import { type FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { User, Lock, Eye, EyeOff, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

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
    <div>
      {/* 헤더 */}
      <div className="mb-s6 flex flex-col items-center text-center">
        <img src="/igruslogo.png" alt="IGRUS" className="h-12 w-12" />
        <h1 className="mt-s4 typo-h2 text-foreground">로그인</h1>
        <p className="mt-s1 typo-b2 text-muted-foreground">
          학번과 비밀번호로 로그인해요
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-s4">
        {/* 학번 */}
        <div>
          <label className="mb-s2 block text-sm font-medium text-foreground">
            학번
          </label>
          <div className="relative">
            <User
              size={18}
              className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              type="text"
              name="username"
              autoComplete="username"
              placeholder="8자리 학번"
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
              required
              className={cn(
                "h-11 rounded-r3 pl-10",
                errors.studentId && "border-destructive",
              )}
            />
          </div>
          {errors.studentId && (
            <p className="mt-s2 text-sm text-destructive">{errors.studentId}</p>
          )}
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="mb-s2 block text-sm font-medium text-foreground">
            비밀번호
          </label>
          <div className="relative">
            <Lock
              size={18}
              className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              type={showPassword ? "text" : "password"}
              name="password"
              autoComplete="current-password"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className={cn(
                "h-11 rounded-r3 pl-10 pr-10",
                errors.password && "border-destructive",
              )}
            />
            <button
              type="button"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-s3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
          {errors.password && (
            <p className="mt-s2 text-sm text-destructive">{errors.password}</p>
          )}
        </div>

        {/* 비밀번호 찾기 */}
        <div className="flex justify-end">
          <Link
            to="/forgot-password"
            className="text-sm text-muted-foreground transition-colors hover:text-primary"
          >
            비밀번호를 잊으셨나요?
          </Link>
        </div>

        {/* 로그인 버튼 */}
        <Button
          type="submit"
          disabled={loading}
          className="h-12 w-full cursor-pointer text-base font-bold"
        >
          {loading ? (
            <>
              <Loader2 size={18} className="animate-spin" />
              로그인 중...
            </>
          ) : (
            "로그인"
          )}
        </Button>
      </form>

      {/* 회원가입 링크 */}
      <p className="mt-s6 text-center text-sm text-muted-foreground">
        아직 계정이 없으신가요?{" "}
        <Link
          to="/signup"
          className="font-semibold text-primary hover:underline"
        >
          회원가입
        </Link>
      </p>
    </div>
  );
}
