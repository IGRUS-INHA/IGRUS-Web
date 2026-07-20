# syntax=docker/dockerfile:1
# 빌드 컨텍스트: play-igrus/ (프론트+백 단일 이미지 — Go 서버가 SPA 도 서빙)

FROM node:22-alpine AS web
WORKDIR /app
ENV CI=true
RUN corepack enable
COPY frontend/ .
RUN pnpm install --frozen-lockfile
# 같은 origin 서빙이라 play API 는 상대경로(기본값) — igrus 인증 API 만 절대주소
ENV VITE_IGRUS_API_URL=https://api.igrus.co.kr
RUN pnpm build

FROM golang:1.26-alpine AS build
WORKDIR /src
COPY backend/go.mod backend/go.sum ./
RUN go mod download
COPY backend/ .
# 아키텍처는 빌드 플랫폼을 따름 — prod(EC2 t3=x86_64)는 --platform linux/amd64 로 빌드.
# tzdata 는 time/tzdata import 로 바이너리에 내장.
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /play-server .

# distroless/static: CA 인증서 포함 (S3/SecretsManager HTTPS 호출에 필요)
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /play-server /play-server
COPY --from=web /app/dist /web
ENV STATIC_DIR=/web
EXPOSE 8080
ENTRYPOINT ["/play-server"]
