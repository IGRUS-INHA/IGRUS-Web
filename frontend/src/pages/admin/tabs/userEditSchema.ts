import { z } from "zod";

export const userEditSchema = z.object({
  studentId: z
    .string()
    .min(1, "학번을 입력해주세요.")
    .regex(/^\d{8}$/, "학번은 8자리 숫자여야 합니다."),
  email: z
    .string()
    .min(1, "이메일을 입력해주세요.")
    .email("올바른 이메일을 입력해주세요."),
  name: z
    .string()
    .min(1, "이름을 입력해주세요.")
    .max(50, "이름은 50자 이내여야 합니다."),
  phoneNumber: z
    .string()
    .min(1, "전화번호를 입력해주세요.")
    .regex(/^\d{3}-\d{4}-\d{4}$/, "올바른 전화번호를 입력해주세요."),
  department: z.string().min(1, "학과를 선택해주세요."),
  grade: z
    .number({ message: "학년을 선택해주세요." })
    .min(1, "학년을 선택해주세요.")
    .max(4, "학년은 1~4 사이여야 합니다."),
  enrollmentStatus: z.enum(["ENROLLED", "GENERAL_LEAVE", "MILITARY_LEAVE"], {
    message: "재학 상태를 선택해주세요.",
  }),
  gender: z.enum(["MALE", "FEMALE"], {
    message: "성별을 선택해주세요.",
  }),
});

export type UserEditFormData = z.infer<typeof userEditSchema>;
