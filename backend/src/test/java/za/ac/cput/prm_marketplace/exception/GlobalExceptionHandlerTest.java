package za.ac.cput.prm_marketplace.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import za.ac.cput.prm_marketplace.dto.ApiError;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/42");
        request.setRequestURI("/api/products/42");
        return request;
    }

    private ApiError body(ResponseEntity<Object> response) {
        assertThat(response.getBody()).isInstanceOf(ApiError.class);
        return (ApiError) response.getBody();
    }

    @Test
    @DisplayName("ResourceNotFoundException maps to 404 and echoes the message")
    void notFound_mapsTo404() {
        ResponseEntity<Object> response =
                handler.handleNotFound(new ResourceNotFoundException("Product not found"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body(response).status()).isEqualTo(404);
        assertThat(body(response).message()).isEqualTo("Product not found");
        assertThat(body(response).path()).isEqualTo("/api/products/42");
        assertThat(body(response).fieldErrors()).isNull();
    }

    @Test
    @DisplayName("BadRequestException maps to 400")
    void badRequest_mapsTo400() {
        ResponseEntity<Object> response =
                handler.handleBadRequest(new BadRequestException("Quantity must be positive"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Quantity must be positive");
    }

    @Test
    @DisplayName("UnauthorizedException maps to 401")
    void unauthorized_mapsTo401() {
        ResponseEntity<Object> response =
                handler.handleUnauthorized(new UnauthorizedException("Token expired"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(response).message()).isEqualTo("Token expired");
    }

    @Test
    @DisplayName("ConflictException maps to 409")
    void conflict_mapsTo409() {
        ResponseEntity<Object> response =
                handler.handleConflict(new ConflictException("Email already registered"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body(response).message()).isEqualTo("Email already registered");
    }

    @Test
    @DisplayName("IllegalArgumentException maps to 400 rather than 500")
    void illegalArgument_mapsTo400() {
        ResponseEntity<Object> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad id"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("type mismatch reports the parameter name and expected type")
    void typeMismatch_describesTheOffendingParameter() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "id", null, null);

        ResponseEntity<Object> response = handler.handleTypeMismatch(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message())
                .contains("'id'")
                .contains("not-a-uuid")
                .contains("UUID");
    }

    @Test
    @DisplayName("type mismatch without a required type falls back to 'value'")
    void typeMismatch_withoutRequiredType_fallsBack() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("x", null, "page", null, null);

        assertThat(body(handler.handleTypeMismatch(ex, request())).message()).contains("value");
    }

    @Test
    @DisplayName("a missing required request parameter maps to 400")
    void missingParameter_mapsTo400() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("body", "String");

        ResponseEntity<Object> response = handler.handleMissingParameter(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).contains("body");
    }

    @Test
    @DisplayName("malformed body maps to 400 and does not leak parser internals")
    void unreadableBody_mapsTo400() {
        ResponseEntity<Object> response = handler.handleUnreadable(
                new HttpMessageNotReadableException("Cannot deserialize value", null), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message())
                .contains("malformed")
                .doesNotContain("Cannot deserialize value");
    }

    @Test
    @DisplayName("validation failure reports each field error once")
    void validationFailure_includesFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Name is required"));
        bindingResult.addError(new FieldError("request", "email", "Email is invalid"));
        bindingResult.addError(new FieldError("request", "name", "duplicate message"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new MethodParameter(handler.getClass().getDeclaredMethods()[0], -1), bindingResult);

        ResponseEntity<Object> response = handler.handleValidation(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("Validation failed");
        assertThat(body(response).fieldErrors())
                .containsEntry("name", "Name is required")
                .containsEntry("email", "Email is invalid")
                .hasSize(2);
    }

    @Test
    @DisplayName("the wrong content type is a 415 naming what is accepted, not a 500")
    void unsupportedMediaType_mapsTo415AndSaysWhatIsAccepted() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                "Content-Type 'application/x-www-form-urlencoded' is not supported",
                List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = handler.handleUnsupportedMediaType(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(body(response).status()).isEqualTo(415);
        assertThat(body(response).message()).contains("application/json");
    }

    @Test
    @DisplayName("an unsupported content type with nothing advertised still names JSON")
    void unsupportedMediaType_withoutSupportedTypes_fallsBack() {
        ResponseEntity<Object> response = handler.handleUnsupportedMediaType(
                new HttpMediaTypeNotSupportedException("no match"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(body(response).message()).contains("application/json");
    }

    @Test
    @DisplayName("an image over the multipart limit is a 400, not a server fault")
    void maxUploadSize_mapsTo400() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(
                6_291_456L, new IllegalStateException("Size exceeded for multipart part"));

        ResponseEntity<Object> response = handler.handleMaxUploadSize(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).isEqualTo("That image is too large to upload");
    }

    @Test
    @DisplayName("a multipart body that will not parse is a 400 without the parser's text")
    void multipartFailure_mapsTo400() {
        ResponseEntity<Object> response = handler.handleMultipart(
                new MultipartException("Failed to parse multipart request"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).message()).contains("could not be read");
        assertThat(body(response).message()).doesNotContain("Failed to parse");
    }

    @Test
    @DisplayName("unexpected exceptions become a generic 500 without leaking details")
    void unexpected_mapsToGeneric500() {
        ResponseEntity<Object> response =
                handler.handleUnexpected(new RuntimeException("connection pool exhausted"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body(response).message()).isEqualTo("An unexpected error occurred");
        assertThat(body(response).status()).isEqualTo(500);
        assertThat(body(response).message()).doesNotContain("connection pool exhausted");
    }

    @Test
    @DisplayName("error body always carries a timestamp, reason phrase and path")
    void errorBody_carriesReasonAndPath() {
        ApiError error = body(handler.handleNotFound(new ResourceNotFoundException("nope"), request()));

        assertThat(error.error()).isEqualTo("Not Found");
        assertThat(error.path()).isEqualTo("/api/products/42");
        assertThat(error.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("each status uses the matching reason phrase")
    void reasonPhrase_matchesStatus() {
        assertThat(body(handler.handleBadRequest(new BadRequestException("x"), request())).error())
                .isEqualTo("Bad Request");
        assertThat(body(handler.handleUnauthorized(new UnauthorizedException("x"), request())).error())
                .isEqualTo("Unauthorized");
        assertThat(body(handler.handleConflict(new ConflictException("x"), request())).error())
                .isEqualTo("Conflict");
    }

    @Test
    @DisplayName("ApiError.of without field errors leaves the map null")
    void apiErrorFactory_withoutFieldErrors() {
        ApiError error = ApiError.of(404, "Not Found", "gone", "/x");
        assertThat(error.fieldErrors()).isNull();
        assertThat(error.timestamp()).isNotNull();

        ApiError withErrors = ApiError.of(400, "Bad Request", "bad", "/x", Map.of("a", "b"));
        assertThat(withErrors.fieldErrors()).containsEntry("a", "b");
    }
}
