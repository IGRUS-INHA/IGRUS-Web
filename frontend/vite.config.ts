import path from "path";
import { fileURLToPath } from "url";
import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react-swc";
import tailwindcss from "@tailwindcss/vite";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  const apiTarget =
    env.VITE_API_TARGET || "https://staging-api.igrus.co.kr:8080";

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    define: {
      __FEATURE_COMMUNITY__: env.FEATURE_COMMUNITY !== "false",
      __FEATURE_SEARCH__: env.FEATURE_SEARCH !== "false",
      __FEATURE_PROFILE_EDIT__: env.FEATURE_PROFILE_EDIT !== "false",
    },
    server: {
      proxy: {
        "/api": {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
          cookieDomainRewrite: {
            "igrus.co.kr": "localhost",
            ".igrus.co.kr": "localhost",
          },
        },
      },
    },
  };
});
