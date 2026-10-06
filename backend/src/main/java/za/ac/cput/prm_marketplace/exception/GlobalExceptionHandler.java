package za.ac.cput.prm_marketplace.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                     HttpServletRequest request) {
        String required = ex.getRequiredType() == null ? "value" : ex.getRequiredType().getSimpleName();
        return build(HttpStatus.BAD_REQUEST,
                "Parameter '" + ex.getName() + "' with value '" + ex.getValue() + "' is not a valid " + required,
                request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex,
                                                   HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body is malformed or contains unreadable values", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex,
                                                   HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /**
     * A request to a URL the application does not serve is a 404, not an internal error. Without
     * this the catch-all below reports every unmapped path as a 500 and logs a stack trace, which
     * hides genuine server faults and makes a retired endpoint look like an outage.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(NoResourceFoundException ex,
                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint " + request.getRequestURI(), request, null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandler(NoHandlerFoundException ex,
                                                  HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint " + request.getRequestURI(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "Method " + request.getMethod() + " is not supported for this endpoint", request, null);
    }

    /**
     * A request sent with the wrong content type is the caller's mistake, not a fault here.
     *
     * <p>Without this the catch-all answered 500 and logged a stack trace, so a client that posted a
     * form to a JSON endpoint looked like a server outage. That hides real faults behind noise, and it
     * misdirects whoever is on call towards the server instead of the caller. The list of types this
     * endpoint accepts is in the message, which is the one thing the caller needs to fix the request.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex,
                                                              HttpServletRequest request) {
        String supported = ex.getSupportedMediaTypes().isEmpty()
                ? "This endpoint accepts application/json"
                : "This endpoint accepts " + ex.getSupportedMediaTypes();
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, supported, request, null);
    }

    /**
     * A file larger than the multipart limit is refused while the request is still being parsed,
     * before the upload service can apply its own size rule. Without this the catch-all below
     * answered 500 for what is a very ordinary "that photo is too big", and logged it as a server
     * fault.
     *
     * <p>The message deliberately does not restate the limit: the number the container enforced
     * here (6 MB, a backstop) sits above the rule the service enforces (5 MB), and quoting the
     * wrong one would leave two different answers to the same question. Everything at or below
     * the container's ceiling reaches the service and gets the precise message there.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                      HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "That image is too large to upload", request, null);
    }

    /**
     * A multipart body the container could not parse at all - truncated, malformed, or with a
     * part that is not a file. Also the caller's mistake rather than a fault here, so it is a 400
     * and the parser's internal text stays out of the response.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Object> handleMultipart(MultipartException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "That upload could not be read. Try another file",
                request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<Object> build(HttpStatus status, String message,
                                         HttpServletRequest request, Map<String, String> fieldErrors) {
        za.ac.cput.prm_marketplace.dto.ApiError body =
                za.ac.cput.prm_marketplace.dto.ApiError.of(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI(),
                        fieldErrors
                );
        return ResponseEntity.status(status).body(body);
    }
}