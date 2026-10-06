package za.ac.cput.prm_marketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.prm_marketplace.dto.ImageUploadResponse;
import za.ac.cput.prm_marketplace.service.IImageUploadService;

/**
 * Accepts the photograph a seller puts on a listing.
 *
 * <p>Storing and serving are separate on purpose. This route only writes bytes to disk and hands
 * back their address; reading them back is the static mapping in {@code StorageConfig}, so there is
 * no handler here that opens a file named by the caller.
 *
 * <p>Any signed-in caller may upload. The bytes are not attached to anything yet - the product
 * service already decides whether an address belongs to a listing and whose listing it is.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final IImageUploadService imageUploadService;

    public UploadController(IImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    /**
     * The part is optional in the signature rather than required: a request that arrives without
     * one then fails in the service with "Choose an image to upload" - the same message an empty
     * file gets - instead of the framework's missing-part error, which carries none of that text.
     */
    @PostMapping("/images")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImageUploadResponse(imageUploadService.store(file)));
    }
}
