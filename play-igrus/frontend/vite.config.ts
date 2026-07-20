import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  // 로컬 개발: play 백엔드는 localhost:8080, igrus 인증은 staging api 프록시.
  // 프록시가 refresh 쿠키의 Domain=igrus.co.kr 를 localhost 로 바꿔줘야
  // 로컬에서도 httpOnly refresh 쿠키가 동작한다. (www vite.config.ts 와 동일 패턴)
  const playTarget = env.VITE_PLAY_PROXY_TARGET || "http://localhost:8080";
  const igrusTarget = env.VITE_IGRUS_PROXY_TARGET || "https://staging-api.igrus.co.kr:8080";

  return {
    plugins: [react()],
    resolve: {
      alias: {
        "styled-system": path.resolve(__dirname, "./styled-system"),
      },
    },
    server: {
      proxy: {
        "/api": { target: playTarget, changeOrigin: true },
        "/images": { target: playTarget, changeOrigin: true },
        "/igrus-api": {
          target: igrusTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (p) => p.replace(/^\/igrus-api/, ""),
          cookieDomainRewrite: { "igrus.co.kr": "localhost", ".igrus.co.kr": "localhost" },
        },
      },
    },
  };
});
