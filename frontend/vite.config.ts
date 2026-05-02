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
    plugins: [
      react(),
      tailwindcss(),
      {
        name: "staging-noindex",
        transformIndexHtml(html) {
          if (env.VITE_NOINDEX === "true") {
            return html.replace(
              "</head>",
              '    <meta name="robots" content="noindex, nofollow">\n  </head>',
            );
          }
          return html;
        },
        generateBundle() {
          if (env.VITE_NOINDEX === "true") {
            this.emitFile({
              type: "asset",
              fileName: "robots.txt",
              source: "User-agent: *\nDisallow: /\n",
            });
          }
        },
      },
    ],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    define: {
      __FEATURE_COMMUNITY__: env.FEATURE_COMMUNITY !== "false",
      __FEATURE_SEARCH__: env.FEATURE_SEARCH !== "false",
      __FEATURE_PROFILE_EDIT__: env.FEATURE_PROFILE_EDIT !== "false",
      __FEATURE_EVENTS__: env.FEATURE_EVENTS !== "false",
      __FEATURE_INSTAGRAM__: env.FEATURE_INSTAGRAM !== "false",
      __FEATURE_DARK_MODE__: env.FEATURE_DARK_MODE !== "false",
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
