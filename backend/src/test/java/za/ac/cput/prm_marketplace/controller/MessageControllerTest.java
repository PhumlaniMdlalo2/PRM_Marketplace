package za.ac.cput.prm_marketplace.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Message;
import za.ac.cput.prm_marketplace.domain.MessageStatus;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IConversationService;
import za.ac.cput.prm_marketplace.service.IMessageService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IMessageService messageService;

    @MockitoBean
    private IConversationService conversationService;

    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private Message message;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        message = buildMessage();
    }

    private User buildUser(UUID userId) {
        return new User.Builder()
                .setId(userId)
                .setName("Participant")
                .setEmail("participant@example.com")
                .setPasswordHash("hash")
                .build();
    }

    private Message buildMessage() {
        User sender = buildUser(senderId);
        Conversation conversation = new Conversation.Builder()
                .setId(conversationId)
                .setBuyer(buildUser(UUID.randomUUID()))
                .setSeller(sender)
                .build();

        return new Message.Builder()
                .setId(id)
                .setConversation(conversation)
                .setSender(sender)
                .setBody("Is this available?")
                .setStatus(MessageStatus.SENT)
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the message")
    void create_returnsCreated() throws Exception {
        when(messageService.create(any(Message.class))).thenReturn(message);

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Is this available?"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(messageService.create(any(Message.class))).thenReturn(null);

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the message")
    void read_returnsMessage() throws Exception {
        when(messageService.read(id)).thenReturn(message);

        mockMvc.perform(get("/api/messages/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown message")
    void read_returnsNotFound() throws Exception {
        when(messageService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/messages/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update forces the path id onto the entity")
    void update_usesPathId() throws Exception {
        when(messageService.read(id)).thenReturn(message);
        when(messageService.update(any(Message.class))).thenReturn(message);

        mockMvc.perform(put("/api/messages/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Message> captor =
                org.mockito.ArgumentCaptor.forClass(Message.class);
        verify(messageService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("update returns 404 when the service refuses")
    void update_returnsNotFound() throws Exception {
        when(messageService.update(any(Message.class))).thenReturn(null);

        mockMvc.perform(put("/api/messages/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(messageService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/messages/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown message")
    void delete_returnsNotFound() throws Exception {
        when(messageService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/messages/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every message")
    void getAll_returnsList() throws Exception {
        when(messageService.getAll()).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getByConversation returns the thread in order")
    void getByConversation_returnsList() throws Exception {
        when(messageService.getByConversation(conversationId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages/conversation/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Is this available?"));
    }

    @Test
    @DisplayName("unreadCount returns a numeric count")
    void unreadCount_returnsNumber() throws Exception {
        when(messageService.unreadCount(conversationId)).thenReturn(2L);

        mockMvc.perform(get("/api/messages/conversation/{conversationId}/unread-count", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    @DisplayName("send creates a message from the request parameters")
    void send_createsMessage() throws Exception {
        when(messageService.send(eq(conversationId), eq(senderId), eq("Hello there")))
                .thenReturn(message);

        mockMvc.perform(post("/api/messages/conversation/{conversationId}/send", conversationId)
                        .param("senderId", senderId.toString())
                        .param("body", "Hello there"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(messageService).send(conversationId, senderId, "Hello there");
    }

    @Test
    @DisplayName("send returns 400 when the service refuses the message")
    void send_returnsBadRequest() throws Exception {
        when(messageService.send(eq(conversationId), eq(senderId), any())).thenReturn(null);

        mockMvc.perform(post("/api/messages/conversation/{conversationId}/send", conversationId)
                        .param("senderId", senderId.toString())
                        .param("body", "Hello there"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("send requires a body parameter")
    void send_withoutBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/messages/conversation/{conversationId}/send", conversationId)
                        .param("senderId", senderId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("send rejects a non-UUID senderId")
    void send_withMalformedSender_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/messages/conversation/{conversationId}/send", conversationId)
                        .param("senderId", "not-a-uuid")
                        .param("body", "Hello"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("markRead reports how many messages were updated")
    void markRead_returnsCount() throws Exception {
        when(messageService.markRead(conversationId, senderId)).thenReturn(3);

        mockMvc.perform(patch("/api/messages/conversation/{conversationId}/read", conversationId)
                        .param("readerId", senderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedRead").value(3));
    }

    @Test
    @DisplayName("markRead returns zero when nothing was updated")
    void markRead_returnsZero() throws Exception {
        when(messageService.markRead(conversationId, senderId)).thenReturn(0);

        mockMvc.perform(patch("/api/messages/conversation/{conversationId}/read", conversationId)
                        .param("readerId", senderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedRead").value(0));
    }

    @Test
    @DisplayName("a message never leaks a participant's password hash")
    void message_doesNotLeakPasswordHash() throws Exception {
        when(messageService.read(id)).thenReturn(message);

        String body = mockMvc.perform(get("/api/messages/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("passwordHash").doesNotContain("\"hash\"");
    }
}
