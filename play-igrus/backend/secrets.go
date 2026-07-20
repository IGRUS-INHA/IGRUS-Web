package main

import (
	"context"
	"encoding/json"
	"fmt"

	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
)

// loadDSNFromSecrets 는 Secrets Manager 시크릿(JSON {"dsn": "..."})에서 DB DSN 을 읽는다.
// prod 에서 EC2 인스턴스 롤로 접근. 로컬은 DSN env 를 직접 쓴다.
func loadDSNFromSecrets(ctx context.Context, name string) (string, error) {
	cfg, err := awsconfig.LoadDefaultConfig(ctx)
	if err != nil {
		return "", err
	}
	out, err := secretsmanager.NewFromConfig(cfg).GetSecretValue(ctx, &secretsmanager.GetSecretValueInput{
		SecretId: &name,
	})
	if err != nil {
		return "", err
	}
	var v struct {
		DSN string `json:"dsn"`
	}
	if err := json.Unmarshal([]byte(*out.SecretString), &v); err != nil {
		return "", err
	}
	if v.DSN == "" {
		return "", fmt.Errorf("시크릿 %s 에 dsn 키가 없습니다", name)
	}
	return v.DSN, nil
}
