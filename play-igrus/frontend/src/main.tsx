import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { bootstrapAuth } from "./api/client";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import SubmitPage from "./pages/SubmitPage";
import MyPage from "./pages/MyPage";
import ProfilePage from "./pages/ProfilePage";
import AdminPage from "./pages/AdminPage";
import "./index.css";

// 퍼스트파티 refresh 쿠키로 세션 복원, 없으면 igrus SSO 핸드오프 1회 시도
void bootstrapAuth();

const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "login", element: <LoginPage /> },
      { path: "submit", element: <SubmitPage /> },
      { path: "edit/:id", element: <SubmitPage /> },
      { path: "my", element: <MyPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "admin", element: <AdminPage /> },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
