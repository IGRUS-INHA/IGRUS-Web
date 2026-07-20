import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { bootstrapAuth } from "./api/client";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import SubmitPage from "./pages/SubmitPage";
import MyPage from "./pages/MyPage";
import AdminPage from "./pages/AdminPage";
import "./index.css";

// www 등 다른 *.igrus.co.kr 에서 로그인한 세션을 refresh 쿠키로 복원
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
      { path: "admin", element: <AdminPage /> },
    ],
  },
]);

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
);
