package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import za.ac.cput.prm_marketplace.exception.BadRequestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadServiceImplTest {

    private static final long FIVE_MB = 5L * 1024 * 1024;

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG_BYTES = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] WEBP_BYTES = {
            'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0, 0, 0};
    private static final byte[] GIF_BYTES = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0};

    @TempDir
    Path directory;

    private ImageUploadServiceImpl service() {
        return new ImageUploadServiceImpl(directory.toString(), FIVE_MB);
    }

    private static MockMultipartFile image(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private static byte[] padded(byte[] header, int size) {
        byte[] bytes = new byte[size];
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
    }

    @Test
    @DisplayName("a stored image gets a server-chosen name and its bytes land in the directory")
    void store_writesTheBytesUnderANameOfItsOwn() throws IOException {
        String url = service().store(image("holiday photo.png", "image/png", PNG_BYTES));

        assertThat(url).startsWith("/api/uploads/images/").endsWith(".png");
        String name = url.substring("/api/uploads/images/".length(), url.length() - ".png".length());
        assertThatCode(() -> UUID.fromString(name)).doesNotThrowAnyException();

        Path stored = directory.resolve(name + ".png");
        assertThat(stored).exists();
        assertThat(Files.readAllBytes(stored)).isEqualTo(PNG_BYTES);
        // The caller's file name is not part of anything: only the chosen name reaches the disk.
        assertThat(directory.resolve("holiday photo.png")).doesNotExist();
    }

    @Test
    @DisplayName("every accepted type is stored under the extension this server gives it")
    void store_acceptsEachSupportedType() {
        ImageUploadServiceImpl service = service();

        assertThat(service.store(image("a.jpg", "image/jpeg", JPEG_BYTES))).endsWith(".jpg");
        assertThat(service.store(image("b.webp", "image/webp", WEBP_BYTES))).endsWith(".webp");
        assertThat(service.store(image("c.gif", "image/gif", GIF_BYTES))).endsWith(".gif");
        // The content type arrives as a header the caller wrote, so it is read case-insensitively.
        assertThat(service.store(image("d.PNG", "IMAGE/PNG", PNG_BYTES))).endsWith(".png");
    }

    @Test
    @DisplayName("a text file wearing an image's content type is refused on its bytes, not its header")
    void store_rejectsBytesThatDoNotMatchTheClaimedType() {
        MockMultipartFile disguised = image("notreally.png", "image/png",
                "<html><body>definitely not an image</body></html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service().store(disguised))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a valid image");
    }

    @Test
    @DisplayName("a type outside the accepted list is refused")
    void store_rejectsAnUnsupportedContentType() {
        assertThatThrownBy(() -> service().store(image("notes.txt", "text/plain", "hello".getBytes())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("JPEG, PNG, WebP or GIF");
    }

    @Test
    @DisplayName("an empty file is refused with the message a missing file gets")
    void store_rejectsAnEmptyFile() {
        assertThatThrownBy(() -> service().store(image("empty.png", "image/png", new byte[0])))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Choose an image to upload");
    }

    @Test
    @DisplayName("a request that carries no file at all is refused the same way")
    void store_rejectsAMissingFile() {
        assertThatThrownBy(() -> service().store(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Choose an image to upload");
    }

    @Test
    @DisplayName("a file over the limit is refused with the rule stated")
    void store_rejectsAFileOverTheLimit() {
        MockMultipartFile oversized = image("big.png", "image/png", padded(PNG_BYTES, (int) FIVE_MB + 1));

        assertThatThrownBy(() -> service().store(oversized))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Images must be 5 MB or smaller");
    }

    @Test
    @DisplayName("a file exactly at the limit is kept")
    void store_acceptsAFileAtTheLimit() {
        MockMultipartFile atLimit = image("full.png", "image/png", padded(PNG_BYTES, (int) FIVE_MB));

        assertThat(service().store(atLimit)).endsWith(".png");
    }

    @Test
    @DisplayName("the directory is created when it does not exist yet")
    void store_createsAMissingDirectory() {
        Path fresh = directory.resolve("nested").resolve("deeper");
        ImageUploadServiceImpl service = new ImageUploadServiceImpl(fresh.toString(), FIVE_MB);

        String url = service.store(image("first.png", "image/png", PNG_BYTES));

        String name = url.substring(url.lastIndexOf('/') + 1);
        assertThat(fresh.resolve(name)).exists();
    }
}
