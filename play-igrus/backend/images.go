package main

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"image"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"regexp"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/service/s3/types"
	_ "golang.org/x/image/webp"
)

var (
	errTooLarge = errors.New("image too large")
	errNotImage = errors.New("not an image")
)

const maxImageBytes = 4 << 20 // 4MB

var extContentType = map[string]string{
	".png":  "image/png",
	".jpg":  "image/jpeg",
	".webp": "image/webp",
}

var formatExt = map[string]string{"png": ".png", "jpeg": ".jpg", "webp": ".webp"}

// 우리가 생성하는 키 형식만 통과 — 경로 조작 원천 차단.
var keyPattern = regexp.MustCompile(`^[a-f0-9]{32}\.(png|jpg|webp)$`)

// imageStore: prod 는 S3, 로컬 개발/테스트는 디스크.
type imageStore interface {
	put(ctx context.Context, key, contentType string, data []byte) error
	get(ctx context.Context, key string) (io.ReadCloser, string, error)
}

// newImageStore 는 S3_BUCKET 이 있으면 S3(play/ prefix), 없으면 로컬 디렉토리.
func newImageStore(ctx context.Context, dataDir string) (imageStore, error) {
	bucket := os.Getenv("S3_BUCKET")
	if bucket == "" {
		dir := filepath.Join(dataDir, "images")
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return nil, err
		}
		return dirStore{dir: dir}, nil
	}
	cfg, err := awsconfig.LoadDefaultConfig(ctx)
	if err != nil {
		return nil, err
	}
	return s3Store{client: s3.NewFromConfig(cfg), bucket: bucket}, nil
}

type s3Store struct {
	client *s3.Client
	bucket string
}

func (s s3Store) put(ctx context.Context, key, contentType string, data []byte) error {
	_, err := s.client.PutObject(ctx, &s3.PutObjectInput{
		Bucket:      aws.String(s.bucket),
		Key:         aws.String("play/" + key),
		Body:        bytes.NewReader(data),
		ContentType: aws.String(contentType),
	})
	return err
}

func (s s3Store) get(ctx context.Context, key string) (io.ReadCloser, string, error) {
	out, err := s.client.GetObject(ctx, &s3.GetObjectInput{
		Bucket: aws.String(s.bucket),
		Key:    aws.String("play/" + key),
	})
	if err != nil {
		var noKey *types.NoSuchKey
		if errors.As(err, &noKey) {
			return nil, "", errNotFound
		}
		return nil, "", err
	}
	return out.Body, aws.ToString(out.ContentType), nil
}

type dirStore struct {
	dir string
}

func (d dirStore) put(_ context.Context, key, _ string, data []byte) error {
	return os.WriteFile(filepath.Join(d.dir, key), data, 0o644)
}

func (d dirStore) get(_ context.Context, key string) (io.ReadCloser, string, error) {
	f, err := os.Open(filepath.Join(d.dir, key))
	if os.IsNotExist(err) {
		return nil, "", errNotFound
	}
	if err != nil {
		return nil, "", err
	}
	return f, extContentType[filepath.Ext(key)], nil
}

// saveImage 는 업로드 파일이 진짜 이미지(PNG/JPEG/WebP)인지 디코드로 확인한 뒤 저장한다.
// 확장자나 Content-Type 은 클라이언트가 마음대로 보내므로 믿지 않는다.
func (s *server) saveImage(r *http.Request, field string) (string, error) {
	f, _, err := r.FormFile(field)
	if err == http.ErrMissingFile {
		return "", nil // 썸네일/배너는 선택 — 없으면 프론트가 분류 색 플레이스홀더를 띄운다
	}
	if err != nil {
		return "", err
	}
	defer f.Close()

	data, err := io.ReadAll(io.LimitReader(f, maxImageBytes+1))
	if err != nil {
		return "", err
	}
	if len(data) > maxImageBytes {
		return "", errTooLarge
	}
	_, format, err := image.DecodeConfig(bytes.NewReader(data))
	if err != nil {
		return "", errNotImage
	}
	ext, ok := formatExt[format]
	if !ok {
		return "", errNotImage
	}

	var b [16]byte
	if _, err := rand.Read(b[:]); err != nil {
		return "", err
	}
	key := hex.EncodeToString(b[:]) + ext

	if err := s.images.put(r.Context(), key, extContentType[ext], data); err != nil {
		return "", err
	}
	return key, nil
}

// GET /images/{key} — S3(또는 로컬) 이미지를 프록시 스트리밍한다.
// 버킷이 private 이라 비로그인 방문자용 공개 경로가 필요하다.
func (s *server) serveImage(w http.ResponseWriter, r *http.Request) {
	key := r.PathValue("key")
	if !keyPattern.MatchString(key) {
		writeErr(w, http.StatusNotFound, "이미지를 찾을 수 없습니다")
		return
	}
	body, contentType, err := s.images.get(r.Context(), key)
	if err == errNotFound {
		writeErr(w, http.StatusNotFound, "이미지를 찾을 수 없습니다")
		return
	}
	if err != nil {
		writeErr(w, http.StatusInternalServerError, "이미지를 불러올 수 없습니다")
		return
	}
	defer body.Close()

	if contentType == "" {
		contentType = extContentType[filepath.Ext(key)]
	}
	w.Header().Set("Content-Type", contentType)
	w.Header().Set("X-Content-Type-Options", "nosniff")
	// 키가 랜덤 UUID 라 내용이 바뀔 일이 없다 → 1년 immutable 캐시
	w.Header().Set("Cache-Control", "public, max-age=31536000, immutable")
	io.Copy(w, body)
}
