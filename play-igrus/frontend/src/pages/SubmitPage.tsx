import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router-dom";
import { css } from "styled-system/css";
import { flex } from "styled-system/patterns";
import {
  ApiError,
  fetchMine,
  imageSrc,
  submitProject,
  updateProject,
  type Project,
} from "../api/client";
import ImageDropzone from "../components/ImageDropzone";
import RequireLogin from "../components/RequireLogin";
import { CATEGORIES } from "../components/category";
import { field, input, label, primaryBtn, requiredMark } from "../components/formStyles";

interface Form {
  title: string;
  description: string;
  body: string;
  url: string;
  category: string;
}

export default function SubmitPage() {
  return (
    <RequireLogin>
      <SubmitForm />
    </RequireLogin>
  );
}

function SubmitForm() {
  const { id } = useParams(); // /edit/:id 면 수정 모드
  const editId = id ? Number(id) : undefined;
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<Form>({ defaultValues: { category: CATEGORIES[0] } });
  const [error, setError] = useState("");
  // 이미지는 드롭도 받아야 해서 RHF 대신 state 로 관리
  const [thumbFile, setThumbFile] = useState<File | undefined>();
  const [bannerFile, setBannerFile] = useState<File | undefined>();
  // 수정 모드: 사용자가 보고 있어야 할 버전 = 심사 대기/반려 수정본 > 라이브
  const [existing, setExisting] = useState<Project | null>(null);
  const [loading, setLoading] = useState(!!editId);
  const navigate = useNavigate();

  useEffect(() => {
    if (!editId) return;
    fetchMine()
      .then((items) => {
        const mine = items.find((p) => p.id === editId);
        if (!mine) {
          setError("작품을 찾을 수 없습니다");
          return;
        }
        const base = mine.update ?? mine;
        setExisting(base);
        reset({
          title: base.title,
          description: base.description,
          body: base.body ?? "",
          url: base.redirectUrl ?? "",
          category: base.category,
        });
      })
      .catch(() => setError("작품을 불러올 수 없습니다"))
      .finally(() => setLoading(false));
  }, [editId, reset]);

  const desc = watch("description") ?? "";

  const onSubmit = async (data: Form) => {
    setError("");
    if (!thumbFile && !existing?.thumbnailUrl) {
      setError("썸네일을 등록해주세요");
      return;
    }
    const form = new FormData();
    form.set("title", data.title);
    form.set("description", data.description);
    form.set("body", data.body);
    form.set("url", data.url);
    form.set("category", data.category);
    if (thumbFile) form.set("thumbnail", thumbFile);
    if (bannerFile) form.set("banner", bannerFile);
    try {
      if (editId) await updateProject(editId, form);
      else await submitProject(form);
      navigate("/my");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "제출에 실패했습니다");
    }
  };

  if (loading) {
    return <p className={css({ textAlign: "center", color: "gray.400", py: "20" })}>불러오는 중…</p>;
  }

  return (
    <div className={css({ maxW: "lg", mx: "auto" })}>
      <h1 className={css({ fontSize: "2xl", fontWeight: "800", mb: "1" })}>
        {editId ? "작품 수정" : "작품 출시"}
      </h1>
      <p className={css({ fontSize: "sm", color: "gray.500", mb: "6" })}>
        {editId
          ? "수정 내용은 운영진 승인 후 반영돼요 (그전까지 기존 버전이 공개돼요)"
          : "운영진 승인 후 메인에 공개돼요"}
      </p>

      <form onSubmit={handleSubmit(onSubmit)} className={flex({ direction: "column", gap: "5" })}>
        <div className={field}>
          <label htmlFor="title" className={label}>
            제목<span className={requiredMark}>필수</span>
          </label>
          <input
            id="title"
            className={input}
            {...register("title", {
              required: "제목을 입력해주세요",
              maxLength: { value: 100, message: "제목은 100자 이내예요" },
            })}
          />
          {errors.title && <p className={errText}>{errors.title.message}</p>}
        </div>

        <div className={field}>
          <label htmlFor="description" className={label}>
            설명<span className={requiredMark}>필수</span>{" "}
            <span className={css({ color: "gray.400", fontWeight: "400" })}>({desc.length}/50)</span>
          </label>
          <input
            id="description"
            maxLength={50}
            placeholder="카드에 보이는 한 줄 소개"
            className={input}
            {...register("description", {
              required: "설명을 입력해주세요",
              maxLength: { value: 50, message: "설명은 50자 이내예요" },
            })}
          />
          {errors.description && <p className={errText}>{errors.description.message}</p>}
        </div>

        <div className={field}>
          <label htmlFor="body" className={label}>
            본문{" "}
            <span className={css({ color: "gray.400", fontWeight: "400" })}>(마크다운)</span>
          </label>
          <textarea
            id="body"
            rows={10}
            placeholder={"# 내 작품 소개\n\n마크다운으로 자유롭게 작성해요"}
            className={`${input} ${css({ resize: "vertical", fontFamily: "mono", fontSize: "sm" })}`}
            {...register("body")}
          />
          {errors.body && <p className={errText}>{errors.body.message}</p>}
        </div>

        <div className={field}>
          <label htmlFor="url" className={label}>
            작품 주소<span className={requiredMark}>필수</span>
          </label>
          <input
            id="url"
            type="url"
            inputMode="url"
            placeholder="https://"
            className={input}
            {...register("url", {
              required: "작품 주소를 입력해주세요",
              pattern: { value: /^https?:\/\/.+/, message: "http:// 또는 https:// 로 시작해야 해요" },
            })}
          />
          {errors.url && <p className={errText}>{errors.url.message}</p>}
        </div>

        <div className={field}>
          <label htmlFor="category" className={label}>
            분류<span className={requiredMark}>필수</span>
          </label>
          <select id="category" className={input} {...register("category", { required: true })}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        <ImageDropzone
          id="thumbnail"
          title="정방형 썸네일"
          hint="카드에 보이는 정사각형 이미지 · 4MB 이하"
          file={thumbFile}
          onChange={setThumbFile}
          existingUrl={imageSrc(existing?.thumbnailUrl)}
          square
          required
        />
        <ImageDropzone
          id="banner"
          title="배너 (선택)"
          hint="상세 화면 위에 보이는 가로 이미지 · 4MB 이하"
          file={bannerFile}
          onChange={setBannerFile}
          existingUrl={imageSrc(existing?.bannerUrl)}
        />

        {error && <p className={errText}>{error}</p>}

        <button type="submit" disabled={isSubmitting} className={primaryBtn}>
          {isSubmitting ? "제출 중…" : editId ? "수정 신청" : "출시 신청"}
        </button>
      </form>
    </div>
  );
}

const errText = css({ fontSize: "xs", color: "red.500", mt: "1" });
