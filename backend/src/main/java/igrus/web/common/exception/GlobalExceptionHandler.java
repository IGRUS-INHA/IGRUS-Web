package igrus.web.common.exception;

import igrus.web.community.comment.exception.CommentAccessDeniedException;
import igrus.web.community.like.comment_like.exception.CommentLikeException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.CommentReportException;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.post.exception.InvalidPostOptionException;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostImageLimitExceededException;
import igrus.web.community.post.exception.PostAnonymousUnchangeableException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.exception.PostRateLimitExceededException;
import igrus.web.community.post.exception.PostTitleTooLongException;
import igrus.web.security.auth.common.exception.account.AccountRecoverableException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 복구 가능한 탈퇴 계정 예외 처리.
     * 클라이언트가 복구 플로우로 이동할 수 있도록 추가 정보를 포함합니다.
     */
    @ExceptionHandler(AccountRecoverableException.class)
    public ResponseEntity<AccountRecoverableErrorResponse> handleAccountRecoverableException(AccountRecoverableException e) {
        log.info("AccountRecoverableException: studentId={}, recoveryDeadline={}", e.getStudentId(), e.getRecoveryDeadline());
        ErrorCode errorCode = e.getErrorCode();
        AccountRecoverableErrorResponse response = AccountRecoverableErrorResponse.of(
                errorCode,
                e.getStudentId(),
                e.getRecoveryDeadline()
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    // ========== Post 관련 예외 ==========

    /**
     * 게시글을 찾을 수 없을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFoundException(PostNotFoundException e) {
        log.warn("PostNotFoundException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 게시글 접근 권한이 없을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePostAccessDeniedException(PostAccessDeniedException e) {
        log.warn("PostAccessDeniedException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 게시글 제목이 너무 길 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostTitleTooLongException.class)
    public ResponseEntity<ErrorResponse> handlePostTitleTooLongException(PostTitleTooLongException e) {
        log.warn("PostTitleTooLongException: actualLength={}", e.getActualLength());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 게시글 이미지 개수 제한을 초과했을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostImageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePostImageLimitExceededException(PostImageLimitExceededException e) {
        log.warn("PostImageLimitExceededException: maxAllowed={}, actualCount={}", e.getMaxAllowed(), e.getActualCount());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 게시글 작성 속도 제한을 초과했을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePostRateLimitExceededException(PostRateLimitExceededException e) {
        log.warn("PostRateLimitExceededException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * 게시글 옵션이 유효하지 않을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(InvalidPostOptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPostOptionException(InvalidPostOptionException e) {
        log.warn("InvalidPostOptionException: optionName={}, boardCode={}", e.getOptionName(), e.getBoardCode());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 삭제된 게시글에 접근할 때 발생하는 예외 처리.
     */
    @ExceptionHandler(PostDeletedException.class)
    public ResponseEntity<ErrorResponse> handlePostDeletedException(PostDeletedException e) {
        log.warn("PostDeletedException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.GONE).body(response);
    }

    /**
     * 익명 설정 변경 시도 시 발생하는 예외 처리.
     */
    @ExceptionHandler(PostAnonymousUnchangeableException.class)
    public ResponseEntity<ErrorResponse> handlePostAnonymousUnchangeableException(PostAnonymousUnchangeableException e) {
        log.warn("익명 설정 변경 시도");
        ErrorCode errorCode = ErrorCode.POST_ANONYMOUS_UNCHANGEABLE;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    // ========== Comment 관련 예외 ==========

    /**
     * 댓글을 찾을 수 없을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFoundException(CommentNotFoundException e) {
        log.warn("CommentNotFoundException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 댓글 접근 권한이 없을 때 발생하는 예외 처리.
     */
    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDeniedException(CommentAccessDeniedException e) {
        log.warn("CommentAccessDeniedException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 잘못된 댓글 요청일 때 발생하는 예외 처리.
     */
    @ExceptionHandler(InvalidCommentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCommentException(InvalidCommentException e) {
        log.warn("InvalidCommentException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 댓글 좋아요 관련 예외 처리.
     */
    @ExceptionHandler(CommentLikeException.class)
    public ResponseEntity<ErrorResponse> handleCommentLikeException(CommentLikeException e) {
        log.warn("CommentLikeException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 댓글 신고 관련 예외 처리.
     */
    @ExceptionHandler(CommentReportException.class)
    public ResponseEntity<ErrorResponse> handleCommentReportException(CommentReportException e) {
        log.warn("CommentReportException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(CustomBaseException.class)
    public ResponseEntity<ErrorResponse> handleCustomBaseException(CustomBaseException e) {
        log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, "요청 본문을 읽을 수 없습니다");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        String message = "필수 파라미터 누락: " + e.getParameterName();
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.ACCESS_DENIED);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.warn("AuthorizationDeniedException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(ErrorCode.ACCESS_DENIED);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: {}", e.getMessage(), e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
