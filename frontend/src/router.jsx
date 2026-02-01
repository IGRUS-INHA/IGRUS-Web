import { createBrowserRouter } from 'react-router-dom';
import { Layout, ProtectedRoute } from '@/components/common';

// 페이지
import HomePage from '@/pages/HomePage';
import NotFoundPage from '@/pages/NotFoundPage';

// 인증
import LoginPage from '@/pages/auth/LoginPage';
import SignupPage from '@/pages/auth/SignupPage';
import VerifyEmailPage from '@/pages/auth/VerifyEmailPage';
import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/auth/ResetPasswordPage';

// 게시판
import BoardListPage from '@/pages/board/BoardListPage';
import PostDetailPage from '@/pages/board/PostDetailPage';
import PostWritePage from '@/pages/board/PostWritePage';
import PostEditPage from '@/pages/board/PostEditPage';

// 행사
import EventListPage from '@/pages/event/EventListPage';
import EventDetailPage from '@/pages/event/EventDetailPage';
import EventWritePage from '@/pages/event/EventWritePage';
import EventEditPage from '@/pages/event/EventEditPage';

// 문의
import InquiryPage from '@/pages/inquiry/InquiryPage';
import InquiryLookupPage from '@/pages/inquiry/InquiryLookupPage';

// 마이페이지
import MyPage from '@/pages/mypage/MyPage';

// 관리자
import AdminDashboard from '@/pages/admin/AdminDashboard';
import AdminUsers from '@/pages/admin/AdminUsers';
import AdminAssociates from '@/pages/admin/AdminAssociates';
import AdminInquiries from '@/pages/admin/AdminInquiries';
import AdminScraps from '@/pages/admin/AdminScraps';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      // 공개 페이지
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'signup', element: <SignupPage /> },
      { path: 'verify-email', element: <VerifyEmailPage /> },
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
      { path: 'inquiry', element: <InquiryPage /> },
      { path: 'inquiry/lookup', element: <InquiryLookupPage /> },

      // 게시판
      { path: 'board/:boardType', element: <BoardListPage /> },
      { path: 'board/:boardType/:postId', element: <PostDetailPage /> },
      {
        path: 'board/:boardType/write',
        element: (
          <ProtectedRoute minRole="MEMBER">
            <PostWritePage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'board/:boardType/:postId/edit',
        element: (
          <ProtectedRoute minRole="MEMBER">
            <PostEditPage />
          </ProtectedRoute>
        ),
      },

      // 행사
      { path: 'events', element: <EventListPage /> },
      {
        path: 'events/write',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <EventWritePage />
          </ProtectedRoute>
        ),
      },
      { path: 'events/:eventId', element: <EventDetailPage /> },
      {
        path: 'events/:eventId/edit',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <EventEditPage />
          </ProtectedRoute>
        ),
      },

      // 마이페이지
      {
        path: 'mypage/*',
        element: (
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        ),
      },

      // 관리자
      {
        path: 'admin',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <AdminDashboard />
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/users',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <AdminUsers />
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/associates',
        element: (
          <ProtectedRoute minRole="ADMIN">
            <AdminAssociates />
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/inquiries',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <AdminInquiries />
          </ProtectedRoute>
        ),
      },
      {
        path: 'admin/scraps',
        element: (
          <ProtectedRoute minRole="OPERATOR">
            <AdminScraps />
          </ProtectedRoute>
        ),
      },

      // 404
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

export default router;
