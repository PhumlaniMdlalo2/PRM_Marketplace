package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IConversationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IConversationService conversationService;

    private UUID callerId;
    private UUID otherPartyId;
    private UUID conversationId;
    private UUID productId;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        otherPartyId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        productId = UUID.randomUUID();
        conversation = new Conversation.Builder()
                .setId(conversationId)
                .setBuyer(buildUser(callerId))
                .setSeller(buildUser(otherPartyId))
                .build();
    }

    @Test
    @DisplayName("the conversation list is the caller's, and the /user/{userId} route is gone")
    void getAll_isScopedToTheCaller() throws Exception {
        when(conversationService.getForUser(callerId)).thenReturn(List.of(conversation));

        mockMvc.perform(get("/api/conversations").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId.toString()));

        verify(conversationService).getForUser(callerId);

        // The old route returned another person's threads on request.
        mockMvc.perform(get("/api/conversations/user/" + otherPartyId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("opening a thread makes the caller the buyer")
    void start_callerIsAlwaysTheBuyer() throws Exception {
        when(conversationService.getOrCreate(eq(callerId), eq(otherPartyId), eq(productId)))
                .thenReturn(conversation);

        mockMvc.perform(post("/api/conversations/start")
                        .param("sellerId", otherPartyId.toString())
                        .param("productId", productId.toString())
                        .with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));

        verify(conversationService).getOrCreate(callerId, otherPartyId, productId);
    }

    @Test
    @DisplayName("opening a thread without a product is allowed")
    void start_withoutProduct() throws Exception {
        when(conversationService.getOrCreate(eq(callerId), eq(otherPartyId), eq(null)))
                .thenReturn(conversation);

        mockMvc.perform(post("/api/conversations/start")
                        .param("sellerId", otherPartyId.toString())
                        .with(asStudent(callerId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("opening a thread that cannot be started returns 400")
    void start_returnsBadRequestWhenRefused() throws Exception {
        when(conversationService.getOrCreate(any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/conversations/start")
                        .param("sellerId", otherPartyId.toString())
                        .with(asStudent(callerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a conversation cannot be created from a body naming other people")
    void create_bodyEndpointIsGone() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .with(asStudent(callerId))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"buyer\":{\"id\":\"" + callerId
                                + "\"},\"seller\":{\"id\":\"" + otherPartyId + "\"}}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("a conversation cannot be edited from a body")
    void update_bodyEndpointIsGone() throws Exception {
        mockMvc.perform(put("/api/conversations/" + conversationId)
                        .with(asStudent(callerId))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("reading a thread the caller is not part of returns 404")
    void read_returnsNotFoundForNonParticipant() throws Exception {
        when(conversationService.read(conversationId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/conversations/" + conversationId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reading the caller's own thread succeeds")
    void read_returnsOwnThread() throws Exception {
        when(conversationService.read(conversationId, callerId)).thenReturn(conversation);

        mockMvc.perform(get("/api/conversations/" + conversationId).with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }

    @Test
    @DisplayName("the old participants route that took a userId is gone")
    void read_legacyParticipantsRouteIsGone() throws Exception {
        mockMvc.perform(get("/api/conversations/" + conversationId + "/participants/" + callerId)
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting a thread the caller is not part of returns 404")
    void delete_returnsNotFoundForNonParticipant() throws Exception {
        when(conversationService.delete(conversationId, callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/conversations/" + conversationId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the unread count is the caller's, and the /user/{userId} route is gone")
    void unreadCount_isScopedToTheCaller() throws Exception {
        when(conversationService.unreadCount(callerId)).thenReturn(3L);

        mockMvc.perform(get("/api/conversations/unread-count").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        mockMvc.perform(get("/api/conversations/user/" + otherPartyId + "/unread-count")
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("conversation endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conversations/" + conversationId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/conversations/start").param("sellerId", otherPartyId.toString()))
                .andExpect(status().isUnauthorized());
        verify(conversationService, never()).getForUser(any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }
}