package igrus.web.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("IGRUS Web API")
                .description("""
                        인하대학교 IGRUS 동아리 웹사이트 API

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

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("""
                        JWT Access Token을 입력하세요.

                        형식: Bearer {token} (Bearer 접두사는 자동으로 추가됩니다)

                        예시: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
                        """);
    }
}