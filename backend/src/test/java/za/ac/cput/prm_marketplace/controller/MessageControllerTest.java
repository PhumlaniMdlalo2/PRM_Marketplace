package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IMessageService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static za.ac.cput.prm_marketplace.support.AuthenticatedRequests.asStudent;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMessageService messageService;

    private UUID callerId;
    private UUID intruderId;
    private UUID conversationId;
    private UUID messageId;
    private Message message;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        message = new Message.Builder()
                .setId(messageId)
                .setConversation(new Conversation.Builder()
                        .setId(conversationId)
                        .setBuyer(buildUser(callerId))
                        .setSeller(buildUser(intruderId))
                        .build())
                .setSender(buildUser(callerId))
                .setBody("Is this still available?")
                .build();
    }

    @Test
    @DisplayName("listing a thread the caller is not part of returns nothing")
    void getByConversation_returnsEmptyForNonParticipant() throws Exception {
        when(messageService.getByConversation(conversationId, callerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/messages/conversation/" + conversationId)
                        .with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(messageService).getByConversation(conversationId, callerId);
    }

    @Test
    @DisplayName("listing the caller's own thread succeeds")
    void getByConversation_returnsOwnThread() throws Exception {
        when(messageService.getByConversation(conversationId, callerId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages/conversation/" + conversationId).with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId.toString()));
    }

    @Test
    @DisplayName("there is no endpoint that lists every message in the marketplace")
    void getAll_isGone() throws Exception {
        mockMvc.perform(get("/api/messages").with(asStudent(callerId)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("sending posts as the caller, not as a senderId in the request")
    void send_ignoresAnySenderIdParameter() throws Exception {
        when(messageService.send(eq(conversationId), eq(callerId), anyString())).thenReturn(message);

        mockMvc.perform(post("/api/messages/conversation/" + conversationId + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Still available?\"}")
                        .with(asStudent(callerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()));

        verify(messageService).send(conversationId, callerId, "Still available?");
    }

    @Test
    @DisplayName("message text is not read from the request line")
    void send_keepsTheBodyOutOfTheRequestLine() throws Exception {
        when(messageService.send(eq(conversationId), eq(callerId), anyString())).thenReturn(message);

        // A `body` parameter alone can no longer satisfy this endpoint. Private correspondence used to
        // travel in the URL, which put it in every access log, proxy log and history entry on the way
        // here, so the text must arrive in the body or not at all.
        mockMvc.perform(post("/api/messages/conversation/" + conversationId + "/send")
                        .param("body", "Still available?")
                        .with(asStudent(callerId)))
                .andExpect(status().is4xxClientError());

        verify(messageService, never()).send(any(UUID.class), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("an empty message is rejected before it reaches the service")
    void send_rejectsAnEmptyBody() throws Exception {
        mockMvc.perform(post("/api/messages/conversation/" + conversationId + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}")
                        .with(asStudent(callerId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("a message longer than the column allows is rejected")
    void send_rejectsAnOverlongBody() throws Exception {
        mockMvc.perform(post("/api/messages/conversation/" + conversationId + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "x".repeat(2001) + "\"}")
                        .with(asStudent(callerId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("sending into a thread the caller is not part of returns 400")
    void send_returnsBadRequestForNonParticipant() throws Exception {
        when(messageService.send(conversationId, callerId, "hello")).thenReturn(null);

        mockMvc.perform(post("/api/messages/conversation/" + conversationId + "/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hello\"}")
                        .with(asStudent(callerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("marking read only ever marks the caller's own messages")
    void markRead_scopesToTheToken() throws Exception {
        when(messageService.markRead(conversationId, callerId)).thenReturn(2);

        mockMvc.perform(patch("/api/messages/conversation/" + conversationId + "/read")
                        .with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedRead").value(2));

        verify(messageService).markRead(conversationId, callerId);
    }

    @Test
    @DisplayName("the unread count is scoped to the caller")
    void unreadCount_isScopedToTheCaller() throws Exception {
        when(messageService.unreadCount(conversationId, callerId)).thenReturn(5L);

        mockMvc.perform(get("/api/messages/conversation/" + conversationId + "/unread-count")
                        .with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    @DisplayName("reading a message the caller is not part of returns 404")
    void read_returnsNotFoundForNonParticipant() throws Exception {
        when(messageService.read(messageId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/messages/" + messageId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("messages cannot be created, edited or deleted over HTTP")
    void crudEndpointsAreGone() throws Exception {
        String body = "{\"body\":\"forged\"}";

        // There is no POST on /api/messages, so the route is absent entirely (404). Accepting
        // this request is what let a caller forge the sender on a private message.
        mockMvc.perform(post("/api/messages")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        // GET /{id} is mapped but PUT is not, so the method is refused rather than the path.
        mockMvc.perform(put("/api/messages/" + messageId)
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/messages/" + messageId).with(asStudent(callerId)))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(messageService);
    }

    @Test
    @DisplayName("message endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/messages/conversation/" + conversationId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/messages/" + messageId)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/messages/conversation/" + conversationId + "/read"))
                .andExpect(status().isUnauthorized());
        verify(messageService, never()).getByConversation(any(), any());
    }

    private User buildUser(UUID id) {
        return new User.Builder().setId(id).setEmail(id + "@example.com").build();
    }
}