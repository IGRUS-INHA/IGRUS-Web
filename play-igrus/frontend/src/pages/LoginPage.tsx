import { useState } from "react";
import { useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import { ApiError, login } from "../api/client";
import { field, input, label, primaryBtn } from "../components/formStyles";

interface Form {
  studentId: string;
  password: string;
}

export default function LoginPage() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Form>();
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const location = useLocation();

  const onSubmit = async (data: Form) => {
    setError("");
    try {
      await login(data.studentId, data.password);
      navigate((location.state as { from?: string } | null)?.from ?? "/", { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "로그인에 실패했습니다");
    }
  };

  return (
    <div className={css({ maxW: "sm", mx: "auto", pt: "8" })}>
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "1" })}>로그인</h1>
      <p className={css({ fontSize: "sm", color: "gray.500", mb: "6" })}>
        아이그루스 계정으로 로그인해요
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className={flex({ direction: "column", gap: "4" })}>
        <div className={field}>
          <label htmlFor="studentId" className={label}>
            학번
          </label>
          <input
            id="studentId"
            type="text"
            inputMode="numeric"
            autoComplete="username"
            placeholder="12345678"
            className={input}
            {...register("studentId", {
              required: "학번을 입력해주세요",
              pattern: { value: /^\d{8}$/, message: "학번은 8자리 숫자예요" },
            })}
          />
          {errors.studentId && <p className={errText}>{errors.studentId.message}</p>}
        </div>

        <div className={field}>
          <label htmlFor="password" className={label}>
            비밀번호
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={input}
            {...register("password", { required: "비밀번호를 입력해주세요" })}
          />
          {errors.password && <p className={errText}>{errors.password.message}</p>}
        </div>

        {error && <p className={errText}>{error}</p>}

        <button type="submit" disabled={isSubmitting} className={primaryBtn}>
          {isSubmitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      <p className={css({ fontSize: "sm", color: "gray.500", mt: "6", textAlign: "center" })}>
        계정이 없다면{" "}
        <a
          href="https://www.igrus.co.kr/signup?utm_source=play"
          className={css({ color: "indigo.600", fontWeight: "600" })}
        >
          아이그루스에서 가입
        </a>
        하세요
      </p>
    </div>
  );
}

const errText = css({ fontSize: "xs", color: "red.500", mt: "1" });
