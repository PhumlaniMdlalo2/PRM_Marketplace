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
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IConversationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IConversationService conversationService;

    private UUID id;
    private UUID buyerId;
    private UUID sellerId;
    private UUID productId;
    private User buyer;
    private User seller;
    private Product product;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        buyer = buildUser(buyerId, "buyer@example.com");
        seller = buildUser(sellerId, "seller@example.com");
        product = new Product.Builder().id(productId).name("Textbook").build();
        conversation = new Conversation.Builder()
                .setId(id)
                .setBuyer(buyer)
                .setSeller(seller)
                .setProduct(product)
                .build();
    }

    private User buildUser(UUID id, String email) {
        return new User.Builder()
                .setId(id)
                .setName("Participant")
                .setEmail(email)
                .setPasswordHash("hash")
                .build();
    }

    @Test
    @DisplayName("create returns 201 with the conversation")
    void create_returnsCreated() throws Exception {
        when(conversationService.create(any(Conversation.class))).thenReturn(conversation);

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("create returns 400 when the service refuses")
    void create_returnsBadRequest() throws Exception {
        when(conversationService.create(any(Conversation.class))).thenReturn(null);

        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("read returns the conversation")
    void read_returnsConversation() throws Exception {
        when(conversationService.read(id)).thenReturn(conversation);

        mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("read returns 404 for an unknown conversation")
    void read_returnsNotFound() throws Exception {
        when(conversationService.read(id)).thenReturn(null);

        mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update returns 404 when the conversation is unknown")
    void update_returnsNotFound() throws Exception {
        when(conversationService.read(id)).thenReturn(null);

        mockMvc.perform(put("/api/conversations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isNotFound());

        verify(conversationService, never()).update(any());
    }

    @Test
    @DisplayName("update forces the path id onto the entity")
    void update_usesPathId() throws Exception {
        when(conversationService.read(id)).thenReturn(conversation);
        when(conversationService.update(any(Conversation.class))).thenReturn(conversation);

        mockMvc.perform(put("/api/conversations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Conversation> captor =
                org.mockito.ArgumentCaptor.forClass(Conversation.class);
        verify(conversationService).update(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("delete returns 204 on success")
    void delete_returnsNoContent() throws Exception {
        when(conversationService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/conversations/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("delete returns 404 for an unknown conversation")
    void delete_returnsNotFound() throws Exception {
        when(conversationService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/conversations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getAll returns every conversation")
    void getAll_returnsList() throws Exception {
        when(conversationService.getAll()).thenReturn(List.of(conversation));

        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("getForUser returns the user's conversations")
    void getForUser_returnsList() throws Exception {
        when(conversationService.getForUser(buyerId)).thenReturn(List.of(conversation));

        mockMvc.perform(get("/api/conversations/user/{userId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("unreadCount returns a numeric count")
    void unreadCount_returnsNumber() throws Exception {
        when(conversationService.unreadCount(buyerId)).thenReturn(4L);

        mockMvc.perform(get("/api/conversations/user/{userId}/unread-count", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    @DisplayName("readForParticipant returns the conversation to a participant")
    void readForParticipant_allowsParticipant() throws Exception {
        when(conversationService.readForParticipant(id, buyerId)).thenReturn(conversation);

        mockMvc.perform(get("/api/conversations/{id}/participants/{userId}", id, buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @DisplayName("readForParticipant returns 404 for an outsider")
    void readForParticipant_deniesOutsider() throws Exception {
        UUID outsiderId = UUID.randomUUID();
        when(conversationService.readForParticipant(id, outsiderId)).thenReturn(null);

        mockMvc.perform(get("/api/conversations/{id}/participants/{userId}", id, outsiderId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("start reuses an existing conversation for the requested parties")
    void start_returnsExisting() throws Exception {
        when(conversationService.getOrCreate(any(), any(), any())).thenReturn(conversation);

        mockMvc.perform(post("/api/conversations/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        org.mockito.ArgumentCaptor<User> buyerCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.ArgumentCaptor<User> sellerCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.ArgumentCaptor<Product> productCaptor =
                org.mockito.ArgumentCaptor.forClass(Product.class);
        verify(conversationService).getOrCreate(buyerCaptor.capture(), sellerCaptor.capture(),
                productCaptor.capture());

        org.assertj.core.api.Assertions.assertThat(buyerCaptor.getValue().getId()).isEqualTo(buyerId);
        org.assertj.core.api.Assertions.assertThat(sellerCaptor.getValue().getId()).isEqualTo(sellerId);
        org.assertj.core.api.Assertions.assertThat(productCaptor.getValue().getId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("start returns 400 when the service refuses the request")
    void start_returnsBadRequest() throws Exception {
        when(conversationService.getOrCreate(any(), any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/conversations/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversation)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("start returns 400 when the body has no seller")
    void start_withoutSeller_returnsBadRequest() throws Exception {
        String body = "{\"buyer\":{\"id\":\"" + buyerId + "\"}}";

        mockMvc.perform(post("/api/conversations/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).getOrCreate(any(), any(), any());
    }

    @Test
    @DisplayName("a conversation never leaks a participant's password hash")
    void conversation_doesNotLeakPasswordHash() throws Exception {
        when(conversationService.read(id)).thenReturn(conversation);

        String body = mockMvc.perform(get("/api/conversations/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("passwordHash")
                .doesNotContain("\"hash\"");
    }
}
