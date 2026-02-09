import "dotenv/config";
import { defineConfig } from "orval";

// .env 파일에서 VITE_SWAGGER_URL을 읽어옴
const swaggerSchemaUrl = process.env.VITE_SWAGGER_URL;

if (!swaggerSchemaUrl) {
  throw new Error(
    "VITE_SWAGGER_URL 환경 변수가 설정되지 않았습니다. .env 파일을 확인하세요."
  );
}

export default defineConfig({
  api: {
    input: {
      target: swaggerSchemaUrl,
    },
    output: {
      mode: "tags-split",
      target: "./src/api/model/endpoints.ts",
      schemas: "./src/api/model/models",
      clean: ["./src/api/model"],
      client: "react-query",
      override: {
        query: {
          version: 5,
        },
        mutator: {
          path: "./src/api/client.ts",
          name: "customFetch",
        },
        operations: {
          // */* content-type을 application/json으로 처리
          "*": {
            requestFormat: "json",
          },
        },
      },
    },
  },
});
