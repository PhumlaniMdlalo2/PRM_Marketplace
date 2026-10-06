package za.ac.cput.prm_marketplace.service;

import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.prm_marketplace.exception.BadRequestException;

/**
 * Stores the photographs sellers upload for a listing.
 *
 * <p>An upload is only bytes when it arrives here: it is not attached to a product, because the
 * service that writes the product already decides who may set an image and what counts as a
 * listing. The caller puts the returned address into {@code imageUrl} the same way it would any
 * other one.
 */
public interface IImageUploadService {

    /**
     * Writes the file to disk and returns the address it can be read back from.
     *
     * <p>The name on disk is this server's own - a UUID and an extension taken from the content
     * type - because a caller-supplied name is a path, and a path is exactly what must not come
     * from outside.
     *
     * @throws BadRequestException when the file is missing or empty, is not a supported image
     *                             type, is over the size limit, or does not contain what its
     *                             content type claims
     */
    String store(MultipartFile file);
}
