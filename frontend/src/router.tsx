import { createBrowserRouter, type RouteObject } from "react-router-dom";
import { Layout, ProtectedRoute } from "@/components/common";

// 페이지
import HomePage from "@/pages/HomePage";
import NotFoundPage from "@/pages/NotFoundPage";

// 인증
import LoginPage from "@/pages/auth/LoginPage";
import SignupPage from "@/pages/auth/SignupPage";
import ForgotPasswordPage from "@/pages/auth/ForgotPasswordPage";
import ResetPasswordPage from "@/pages/auth/ResetPasswordPage";

// 게시판
import BoardListPage from "@/pages/board/BoardListPage";
import PostDetailPage from "@/pages/board/PostDetailPage";
import PostWritePage from "@/pages/board/PostWritePage";
import PostEditPage from "@/pages/board/PostEditPage";

// 행사
import EventListPage from "@/pages/event/EventListPage";
import EventCreatePage from "@/pages/event/EventCreatePage";
import EventEditPage from "@/pages/event/EventEditPage";
import EventRegistrationsPage from "@/pages/event/EventRegistrationsPage";
import EventApplyPage from "@/pages/event/EventApplyPage";
import EventExternalApplyPage from "@/pages/event/EventExternalApplyPage";

// 문의
import InquiryPage from "@/pages/inquiry/InquiryPage";
import InquiryHistoryPage from "@/pages/inquiry/InquiryHistoryPage";
import InquiryDetailPage from "@/pages/inquiry/InquiryDetailPage";
import InquiryLookupPage from "@/pages/inquiry/InquiryLookupPage";

// 법적 페이지
import { PrivacyPolicyPage, TermsOfServicePage } from "@/pages/legal";

// 마이페이지
import MyPage from "@/pages/mypage/MyPage";
import ChangePasswordPage from "@/pages/mypage/ChangePasswordPage";
import WithdrawPage from "@/pages/mypage/WithdrawPage";

// 관리자
import AdminDashboard from "@/pages/admin/AdminDashboard";

const routes: RouteObject[] = [
  {
    path: "/",
    element: <Layout />,
    children: [
      // 공개 페이지
      { index: true, element: <HomePage /> },
      { path: "login", element: <LoginPage /> },
      { path: "signup", element: <SignupPage /> },
      { path: "forgot-password", element: <ForgotPasswordPage /> },
      { path: "reset-password", element: <ResetPasswordPage /> },
      { path: "inquiry", element: <InquiryPage /> },
      { path: "inquiry/history", element: <InquiryHistoryPage /> },
      { path: "inquiry/history/:inquiryId", element: <InquiryDetailPage /> },
      { path: "inquiry/lookup", element: <InquiryLookupPage /> },

      // 법적 페이지
      { path: "privacy", element: <PrivacyPolicyPage /> },
      { path: "terms", element: <TermsOfServicePage /> },

      // 게시판
      {
        path: "board/:boardType",
        element: (
          <ProtectedRoute>
            <BoardListPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "board/:boardType/write",
        element: (
          <ProtectedRoute minRole="MEMBER">
            <PostWritePage />
          </ProtectedRoute>
        ),
      },
      {
        path: "board/:boardType/:postId/edit",
        element: (
          <ProtectedRoute minRole="MEMBER">
            <PostEditPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "board/:boardType/:postId",
        element: (
          <ProtectedRoute>
            <PostDetailPage />
          </ProtectedRoute>
        ),
      },

      // 행사
      { path: "events", element: <EventListPage /> },
      {
        path: "events/create",
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <EventCreatePage />
          </ProtectedRoute>
        ),
      },
      {
        path: "events/:eventId/edit",
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <EventEditPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "events/:eventId/apply",
        element: (
          <ProtectedRoute minRole="MEMBER">
            <EventApplyPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "events/:eventId/apply/external",
        element: <EventExternalApplyPage />,
      },
      {
        path: "events/:eventId/registrations",
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <EventRegistrationsPage />
          </ProtectedRoute>
        ),
      },

      // 마이페이지
      {
        path: "mypage/change-password",
        element: (
          <ProtectedRoute>
            <ChangePasswordPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "mypage/withdraw",
        element: (
          <ProtectedRoute>
            <WithdrawPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "mypage/*",
        element: (
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        ),
      },

      // 관리자
      {
        path: "admin",
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <AdminDashboard />
          </ProtectedRoute>
        ),
      },
      // 404
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];

const router = createBrowserRouter(routes);

export default router;
