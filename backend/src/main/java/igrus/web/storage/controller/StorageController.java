package igrus.web.storage.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.storage.dto.ConfirmUploadRequest;
import igrus.web.storage.dto.ConfirmUploadResponse;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.storage.dto.CreatePresignedUrlResponse;
import igrus.web.storage.dto.DownloadUrlResponse;
import igrus.web.storage.service.DownloadUrlService;
import igrus.web.storage.service.FileDeleteService;
import igrus.web.storage.service.PresignedUrlService;
import igrus.web.storage.service.UploadConfirmService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * 파일 저장소 컨트롤러.
 * Presigned URL 기반 이미지 업로드/다운로드 및 파일 삭제 API를 제공합니다.
 */
@Tag(name = "Storage", description = "파일 저장소 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final PresignedUrlService presignedUrlService;
    private final UploadConfirmService uploadConfirmService;
    private final DownloadUrlService downloadUrlService;
    private final FileDeleteService fileDeleteService;

    @Operation(summary = "업로드용 Presigned URL 생성", description = "S3에 파일을 업로드하기 위한 Presigned URL을 생성합니다. 인증 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePresignedUrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 크기/타입 제한 위반)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "S3 SDK 장애")
    })
    @PostMapping("/presigned-url")
    public ResponseEntity<CreatePresignedUrlResponse> createUploadUrl(
            @Valid @RequestBody CreatePresignedUrlRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreatePresignedUrlResponse response = presignedUrlService.createUploadUrl(request, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "업로드 완료 확인", description = "S3에 파일 업로드 완료 후 백엔드에 알립니다. S3 HEAD 검증을 수행합니다. 인증 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 완료 확인 성공 (COMPLETED 또는 FAILED)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfirmUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "소유권 불일치"),
            @ApiResponse(responseCode = "404", description = "파일 메타데이터 미존재")
    })
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmUploadResponse> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ConfirmUploadResponse response = uploadConfirmService.confirmUpload(request.objectKey(), user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "다운로드용 Presigned URL 생성", description = "S3에서 파일을 다운로드하기 위한 Presigned URL을 생성합니다. COMPLETED 상태의 파일만 가능합니다. 인증 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다운로드 URL 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DownloadUrlResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "파일 미존재 또는 COMPLETED 상태가 아님"),
            @ApiResponse(responseCode = "500", description = "S3 SDK 장애")
    })
    @GetMapping("/download-url")
    public ResponseEntity<DownloadUrlResponse> createDownloadUrl(
            @Parameter(description = "S3 Object Key") @RequestParam String objectKey,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        DownloadUrlResponse response = downloadUrlService.createDownloadUrl(objectKey, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "파일 삭제", description = "S3 객체와 파일 메타데이터를 삭제합니다. OPERATOR 이상 권한 필요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "파일 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)"),
            @ApiResponse(responseCode = "404", description = "파일 메타데이터 미존재"),
            @ApiResponse(responseCode = "409", description = "참조 무결성 위반 (상위 엔티티에서 참조 중)"),
            @ApiResponse(responseCode = "500", description = "S3 삭제 실패")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "S3 Object Key") @RequestParam String objectKey,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        fileDeleteService.deleteFile(objectKey, user.userId());
        return ResponseEntity.noContent().build();
    }
}
