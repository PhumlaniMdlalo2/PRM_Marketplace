package za.ac.cput.prm_marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.prm_marketplace.exception.BadRequestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageUploadServiceImpl implements IImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(ImageUploadServiceImpl.class);

    /** The prefix StorageConfig serves. A stored file's address is this plus the name below. */
    private static final String PUBLIC_PREFIX = "/api/uploads/images/";

    /**
     * Accepted content types and the extension this server gives them. The extension comes from
     * the type, never from the file name the caller sent: a name is part of a path, and a path is
     * the one thing that must not be taken from outside.
     */
    private static final Map<String, String> EXTENSIONS_BY_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final Path directory;
    private final long maxBytes;

    public ImageUploadServiceImpl(
            @Value("${app.storage.images-dir}") String imagesDirectory,
            @Value("${app.storage.max-image-bytes}") long maxImageBytes) {
        this.directory = Paths.get(imagesDirectory).toAbsolutePath().normalize();
        this.maxBytes = maxImageBytes;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS_BY_TYPE.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Upload a JPEG, PNG, WebP or GIF image");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Images must be " + maxBytes / (1024 * 1024) + " MB or smaller");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("That image could not be read. Choose another file");
        }
        if (!looksLike(bytes, contentType)) {
            throw new BadRequestException("That file is not a valid image. Choose a JPEG, PNG, WebP or GIF");
        }

        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directory);
            Files.write(directory.resolve(name), bytes);
        } catch (IOException e) {
            log.error("Could not store an upload in {}", directory, e);
            throw new IllegalStateException("Image storage is unavailable", e);
        }
        return PUBLIC_PREFIX + name;
    }

    /**
     * Checks the first bytes against the type the request claims.
     *
     * <p>The content type is a header the caller wrote, so on its own it proves nothing: a script
     * labelled {@code image/png} would otherwise be stored and later served as an image. This is
     * the check that makes the accepted-type list mean something.
     */
    private static boolean looksLike(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> startsWith(bytes, 0, PNG_MAGIC);
            case "image/gif" -> startsWith(bytes, 0, ascii("GIF87a"))
                    || startsWith(bytes, 0, ascii("GIF89a"));
            case "image/webp" -> startsWith(bytes, 0, ascii("RIFF"))
                    && startsWith(bytes, 8, ascii("WEBP"));
            default -> false;
        };
    }

    private static boolean startsWith(byte[] bytes, int offset, byte[] expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
