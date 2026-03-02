import type { Inquiry } from "@/types";

export const MOCK_INQUIRIES: Inquiry[] = [
  {
    id: "1",
    inquiryNumber: "INQ-20260131001",
    category: "TECHNICAL",
    title: "React Query 사용 중 캐싱 문제",
    content:
      "useQuery를 사용하는데 데이터가 계속 refetch되는 것 같습니다. staleTime을 설정했는데도 그런 것 같아요.",
    status: "ANSWERED",
    createdAt: "2026-01-29T14:30:00",
    answeredAt: "2026-01-29T18:45:00",
    answer:
      "React Query의 기본 동작은 window focus 시 자동으로 refetch됩니다. refetchOnWindowFocus 옵션을 false로 설정해보세요.",
  },
  {
    id: "2",
    inquiryNumber: "INQ-20260131002",
    category: "ACCOUNT",
    title: "회원가입 시 이메일 인증 안됨",
    content:
      "회원가입을 했는데 인증 이메일이 오지 않습니다. 스팸 메일함도 확인했는데 없어요.",
    status: "PENDING",
    createdAt: "2026-01-30T10:15:00",
  },
  {
    id: "3",
    inquiryNumber: "INQ-20260131003",
    category: "EVENT",
    title: "세미나 참가 신청 방법",
    content: "다음 주에 있는 AI 세미나에 참가하고 싶은데 어떻게 신청하나요?",
    status: "ANSWERED",
    createdAt: "2026-01-28T16:20:00",
    answeredAt: "2026-01-28T20:10:00",
    answer:
      '이벤트 페이지에서 해당 세미나를 클릭하시면 "참가 신청" 버튼이 있습니다. 로그인 후 이용 가능합니다.',
  },
  {
    id: "4",
    inquiryNumber: "INQ-20260131004",
    category: "GENERAL",
    title: "IGRUS 동아리 가입 문의",
    content:
      "2학년 컴퓨터공학과 학생입니다. IGRUS 동아리에 가입하고 싶은데 언제 모집하나요?",
    status: "IN_PROGRESS",
    createdAt: "2026-01-31T09:00:00",
  },
  {
    id: "5",
    inquiryNumber: "INQ-20260131005",
    category: "TECHNICAL",
    title: "Spring Boot JWT 토큰 만료 처리",
    content:
      "JWT 토큰이 만료되었을 때 자동으로 갱신하는 로직을 구현하고 싶습니다. 어떻게 해야 하나요?",
    status: "ANSWERED",
    createdAt: "2026-01-27T11:30:00",
    answeredAt: "2026-01-27T14:20:00",
    answer:
      "Refresh Token을 사용하는 것을 추천합니다. axios interceptor에서 401 응답 시 refresh token으로 갱신 후 재요청하는 방식으로 구현하면 됩니다.",
  },
];
