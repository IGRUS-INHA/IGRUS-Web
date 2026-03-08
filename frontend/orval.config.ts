import { defineConfig } from "orval";

export default defineConfig({
  api: {
    input: {
      target: "../openapi/bundled.yaml",
    },
    output: {
      mode: "tags-split",
      target: "./src/api/model/endpoints.ts",
      schemas: "./src/api/model/models",
      clean: ["./src/api/model"],
      prettier: true,
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
