package za.ac.cput.prm_marketplace.exception;

/**
 * The caller is authenticated but is not allowed to perform the request.
 *
 * <p>Distinct from {@link UnauthorizedException}, which means there is no usable identity at all.
 * The existing services handled "you may not touch this row" by returning null and answering 404,
 * which is right when the row's existence is itself sensitive but wrong when it is not — it tells
 * an ordinary user that they are being refused rather than that they are asking the wrong way.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}