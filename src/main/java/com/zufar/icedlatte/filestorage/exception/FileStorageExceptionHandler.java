package com.zufar.icedlatte.filestorage.exception;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class FileStorageExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ProblemDetail> handleFileStorageException(final FileStorageException ex) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}

        var mapping = switch (ex) {
            case FileReadException _ ->
                    new ErrorMapping("exception.file.read_failed", ProblemType.FILE_READ_FAILED, "File read failed", HttpStatus.BAD_REQUEST, ex.getMessage());
            case FileUploadException e when e.getCause() instanceof IllegalStateException ->
                    new ErrorMapping("exception.file.storage_unavailable", ProblemType.FILE_UPLOAD_FAILED, "File upload failed", HttpStatus.SERVICE_UNAVAILABLE, "File storage is not available.");
            case FileUploadException e ->
                    new ErrorMapping("exception.file.upload_failed", ProblemType.FILE_UPLOAD_FAILED, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        };

        if (mapping.status().is5xxServerError()) {
            log.error("{}: status={}", mapping.logTag(), mapping.status().value(), ex);
        } else {
            log.debug("{}: status={}", mapping.logTag(), mapping.status().value());
        }

        ProblemDetail pd = problemDetailFactory
                .build(mapping.typeSlug(), mapping.title(), mapping.status(), mapping.detail());
        return ResponseEntity.status(mapping.status()).body(pd);
    }
}
