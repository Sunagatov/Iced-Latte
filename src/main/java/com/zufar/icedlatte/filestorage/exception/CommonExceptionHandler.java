package com.zufar.icedlatte.filestorage.exception;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CommonExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(FileReadException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleFileReadException(final FileReadException exception) {
        log.debug("exception.file.read_failed: exceptionClass={}, status=400",
                exception.getClass().getSimpleName());
        return problemDetailFactory.build(ProblemType.FILE_READ_FAILED, "File read failed",
                HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ProblemDetail> handleFileUploadException(final FileUploadException exception) {
        if (exception.getCause() instanceof IllegalStateException) {
            log.warn("exception.file.storage_unavailable: status=503");
            ProblemDetail pd = problemDetailFactory.build(ProblemType.FILE_UPLOAD_FAILED, "File upload failed",
                    HttpStatus.SERVICE_UNAVAILABLE, "File storage is not available.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
        }
        log.error("exception.file.upload_failed: exceptionClass={}, status=500",
                exception.getClass().getSimpleName(), exception);
        ProblemDetail pd = problemDetailFactory.build(ProblemType.FILE_UPLOAD_FAILED, "File upload failed",
                HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
