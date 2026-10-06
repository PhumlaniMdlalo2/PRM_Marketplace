package za.ac.cput.prm_marketplace.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.support.AuthenticatedRequests;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole upload path with the real service behind it: bytes in, address back, address served.
 *
 * <p>The service itself is covered separately in ImageUploadServiceImplTest; what only shows up
 * here is the wiring - that the controller stores what the multipart part carried, that the
 * address it answers with is the one the static resource mapping serves, and that reading one
 * back needs no token, because an {@code <img>} tag never carries an Authorization header.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UploadControllerTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 'I', 'H', 'D', 'R'};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("an upload is stored and the address it returns serves the same bytes back")
    void upload_isStoredAndServedBack() throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/uploads/images")
                        .file(new MockMultipartFile("file", "holiday.png", "image/png", PNG_BYTES))
                        .with(AuthenticatedRequests.asStudent(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(Matchers.startsWith("/api/uploads/images/")))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String url = body.get("url").asString();

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    @DisplayName("a file whose bytes are not an image is refused with the reason")
    void upload_rejectsAFileDisguisedAsAnImage() throws Exception {
        MockMultipartFile disguised = new MockMultipartFile(
                "file", "evil.png", "image/png", "definitely not a png".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/uploads/images")
                        .file(disguised)
                        .with(AuthenticatedRequests.asStudent(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("That file is not a valid image. Choose a JPEG, PNG, WebP or GIF"));
    }

    @Test
    @DisplayName("a request that carries no file gets the message an empty one would")
    void upload_withoutAFileIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/uploads/images")
                        .with(AuthenticatedRequests.asStudent(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose an image to upload"));
    }

    @Test
    @DisplayName("serving a file that was never uploaded is a 404")
    void servingAnUnknownFile_isNotFound() throws Exception {
        mockMvc.perform(get("/api/uploads/images/no-such-file.png"))
                .andExpect(status().isNotFound());
    }
}
