package igrus.web.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.List;

@Configuration
public class SwaggerConfig {

    public static final String SECURITY_SCHEME_NAME = "BearerAuthentication";

    @Value("${springdoc.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        Server server = new Server()
                .url(serverUrl)
                .description("API Server");

        return new OpenAPI()
                .servers(List.of(server))
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    @Bean
    public OpenApiCustomizer pageableDescriptionCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(op -> {
                        if (op.getParameters() == null) return;
                        op.getParameters().forEach(p -> {
                            switch (p.getName()) {
                                case "page" -> {
                                    p.setDescription("페이지 번호 (0부터 시작)");
                                    p.setExample("0");
                                }
                                case "size" -> {
                                    p.setDescription("페이지당 항목 수");
                                    p.setExample("20");
                                }
                                case "sort" -> {
                                    p.setDescription(
                                            "정렬 조건. " +
                                                    "여러 정렬은 sort를 여러 번 지정합니다. (sort=createdAt,DESC&sort=id,ASC)"
                                    );
                                    p.setExample("createdAt,desc");
                                }
                            }
                        });
                    })
            );
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("IGRUS Web API")
                .description("""
                        인하대학교 정보통신처 소속 컴퓨터 연구 동아리 IGRUS 웹사이트 API

                        ## 인증 방식

                        이 API는 JWT (JSON Web Token) 기반 Bearer 인증을 사용합니다.

                        ### 인증 헤더 형식
                        ```
                        Authorization: Bearer {access_token}
                        ```

                        ### 사용 방법
                        1. 로그인 API를 호출하여 Access Token을 발급받습니다.
                        2. 인증이 필요한 API 호출 시, HTTP 헤더에 위 형식으로 토큰을 포함합니다.

                        ### 예시
                        ```
                        GET /api/v1/users/me
                        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                        ```

                        ### 토큰 정보
                        - **Access Token**: API 요청 인증에 사용 (유효 기간: 설정에 따름)
                        - **Refresh Token**: Access Token 갱신에 사용 (유효 기간: 설정에 따름)

                        ### 주의사항
                        - Access Token이 만료되면 Refresh Token을 사용하여 새로운 Access Token을 발급받아야 합니다.
                        - Refresh Token도 만료되면 다시 로그인해야 합니다.
                        """)
                .version("v1.0.0");
    }

    @Bean
    public OpenApiCustomizer errorResponseSchemaCustomizer() {
        return openApi -> {
            var schemas = openApi.getComponents().getSchemas();
            if (schemas == null || !schemas.containsKey("ErrorResponse")) {
                Schema<?> errorSchema = new Schema<>()
                        .type("object")
                        .description("에러 응답")
                        .addProperty("status", new IntegerSchema()
                                .description("HTTP 상태 코드").example(400))
                        .addProperty("code", new StringSchema()
                                .description("에러 코드").example("AUTH_001"))
                        .addProperty("message", new StringSchema()
                                .description("에러 메시지").example("잘못된 요청입니다."))
                        .addProperty("timestamp", new StringSchema()
                                .format("date-time")
                                .description("에러 발생 시각").example("2024-01-15T10:30:00Z"));
                openApi.getComponents().addSchemas("ErrorResponse", errorSchema);
            }
        };
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Access Token을 입력하세요.

                        형식: Bearer {token} (Bearer 접두사는 자동으로 추가됩니다)

                        예시: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
                        """);
    }
}