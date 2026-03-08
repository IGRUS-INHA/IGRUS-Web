package igrus.web.storage.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.StorageApi;
import igrus.web.generated.model.ApiConfirmUploadRequest;
import igrus.web.generated.model.ApiConfirmUploadResponse;
import igrus.web.generated.model.ApiCreatePresignedUrlRequest;
import igrus.web.generated.model.ApiCreatePresignedUrlResponse;
import igrus.web.generated.model.ApiDownloadUrlResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.storage.service.DownloadUrlService;
import igrus.web.storage.service.FileDeleteService;
import igrus.web.storage.service.PresignedUrlService;
import igrus.web.storage.service.UploadConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StorageController implements StorageApi {

    private final PresignedUrlService presignedUrlService;
    private final UploadConfirmService uploadConfirmService;
    private final DownloadUrlService downloadUrlService;
    private final FileDeleteService fileDeleteService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiCreatePresignedUrlResponse> createUploadUrl(
            ApiCreatePresignedUrlRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var internalRequest = new CreatePresignedUrlRequest(
                request.getFileName(), request.getContentType(),
                request.getFileSize(), request.getPurpose());
        var result = presignedUrlService.createUploadUrl(internalRequest, user.userId());
        return ResponseEntity.ok(new ApiCreatePresignedUrlResponse()
                .presignedUrl(result.presignedUrl())
                .objectKey(result.objectKey()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiConfirmUploadResponse> confirmUpload(
            ApiConfirmUploadRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var result = uploadConfirmService.confirmUpload(
                request.getObjectKey(), user.userId());
        return ResponseEntity.ok(new ApiConfirmUploadResponse()
                .status(result.status())
                .objectKey(result.objectKey())
                .reason(result.reason()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiDownloadUrlResponse> createDownloadUrl(String objectKey) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var result = downloadUrlService.createDownloadUrl(objectKey, user.userId());
        return ResponseEntity.ok(new ApiDownloadUrlResponse()
                .presignedUrl(result.presignedUrl()));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(String objectKey) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        fileDeleteService.deleteFile(objectKey, user.userId());
        return ResponseEntity.noContent().build();
    }
}
