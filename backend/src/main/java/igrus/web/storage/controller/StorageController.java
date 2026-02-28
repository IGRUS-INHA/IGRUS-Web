package igrus.web.storage.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.generated.api.StorageApi;
import igrus.web.generated.model.ConfirmUpload200Response;
import igrus.web.generated.model.ConfirmUploadRequest;
import igrus.web.generated.model.CreateDownloadUrl200Response;
import igrus.web.generated.model.CreateUploadUrl200Response;
import igrus.web.generated.model.CreateUploadUrlRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.storage.dto.ConfirmUploadResponse;
import igrus.web.storage.dto.CreatePresignedUrlRequest;
import igrus.web.storage.dto.CreatePresignedUrlResponse;
import igrus.web.storage.dto.DownloadUrlResponse;
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
    public ResponseEntity<CreateUploadUrl200Response> createUploadUrl(
            CreateUploadUrlRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        CreatePresignedUrlRequest internalRequest = new CreatePresignedUrlRequest(
                request.getFileName(), request.getContentType(),
                request.getFileSize(), request.getPurpose());
        CreatePresignedUrlResponse result = presignedUrlService.createUploadUrl(internalRequest, user.userId());
        return ResponseEntity.ok(new CreateUploadUrl200Response()
                .presignedUrl(result.presignedUrl())
                .objectKey(result.objectKey()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConfirmUpload200Response> confirmUpload(
            ConfirmUploadRequest request) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        ConfirmUploadResponse result = uploadConfirmService.confirmUpload(
                request.getObjectKey(), user.userId());
        return ResponseEntity.ok(new ConfirmUpload200Response()
                .status(result.status())
                .objectKey(result.objectKey())
                .reason(result.reason()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreateDownloadUrl200Response> createDownloadUrl(String objectKey) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        DownloadUrlResponse result = downloadUrlService.createDownloadUrl(objectKey, user.userId());
        return ResponseEntity.ok(new CreateDownloadUrl200Response()
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
