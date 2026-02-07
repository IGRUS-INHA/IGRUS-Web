import { defineConfig } from "orval";

const swaggerSchemaUrl =
  "http://igrus-web-alb-535342735.ap-northeast-2.elb.amazonaws.com/v3/api-docs";

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
