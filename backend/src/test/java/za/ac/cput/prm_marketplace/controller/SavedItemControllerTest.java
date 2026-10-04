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
import tools.jackson.databind.ObjectMapper;
import za.ac.cput.prm_marketplace.domain.SavedItem;
import za.ac.cput.prm_marketplace.service.ISavedItemService;

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
class SavedItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ISavedItemService savedItemService;

    private UUID callerId;
    private UUID intruderId;
    private UUID productId;
    private UUID savedItemId;
    private SavedItem savedItem;

    @BeforeEach
    void setUp() {
        callerId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        savedItemId = UUID.randomUUID();
        savedItem = new SavedItem.Builder().setId(savedItemId).build();
    }

    @Test
    @DisplayName("the saved list is the caller's, and the /user/{userId} route is gone")
    void getAll_isScopedToTheCaller() throws Exception {
        when(savedItemService.getByUser(callerId)).thenReturn(List.of(savedItem));

        mockMvc.perform(get("/api/saved-items").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedItemId.toString()));

        verify(savedItemService).getByUser(callerId);

        mockMvc.perform(get("/api/saved-items/user/" + intruderId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the saved-item count is the caller's")
    void count_isScopedToTheCaller() throws Exception {
        when(savedItemService.countByUser(callerId)).thenReturn(4L);

        mockMvc.perform(get("/api/saved-items/count").with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));

        verify(savedItemService).countByUser(callerId);
    }

    @Test
    @DisplayName("reading a saved item that is not the caller's returns 404")
    void read_returnsNotFoundForAnotherAccountsItem() throws Exception {
        when(savedItemService.read(savedItemId, callerId)).thenReturn(null);

        mockMvc.perform(get("/api/saved-items/" + savedItemId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting a saved item that is not the caller's returns 404")
    void delete_returnsNotFoundForAnotherAccountsItem() throws Exception {
        when(savedItemService.delete(savedItemId, callerId)).thenReturn(false);

        mockMvc.perform(delete("/api/saved-items/" + savedItemId).with(asStudent(callerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("toggling a save acts on the caller's own list")
    void toggle_scopesToTheToken() throws Exception {
        when(savedItemService.toggle(callerId, productId)).thenReturn(savedItem);

        mockMvc.perform(post("/api/saved-items/product/" + productId + "/toggle")
                        .with(asStudent(callerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItemId.toString()));

        verify(savedItemService).toggle(callerId, productId);
    }

    @Test
    @DisplayName("unsaving reports no content when the item was saved")
    void toggle_returnsNoContentWhenUnsaveing() throws Exception {
        when(savedItemService.toggle(callerId, productId)).thenReturn(null);

        mockMvc.perform(post("/api/saved-items/product/" + productId + "/toggle")
                        .with(asStudent(callerId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("the old toggle route that took an arbitrary userId is gone")
    void toggle_legacyUserIdRouteIsGone() throws Exception {
        mockMvc.perform(post("/api/saved-items/user/" + intruderId + "/product/" + productId + "/toggle")
                        .with(asStudent(callerId)))
                .andExpect(status().isNotFound());

        verifyNoInteractions(savedItemService);
    }

    @Test
    @DisplayName("removing a saved product only touches the caller's list")
    void remove_scopesToTheCaller() throws Exception {
        when(savedItemService.removeByUserAndProduct(callerId, productId)).thenReturn(true);

        mockMvc.perform(delete("/api/saved-items/product/" + productId).with(asStudent(callerId)))
                .andExpect(status().isNoContent());

        verify(savedItemService).removeByUserAndProduct(callerId, productId);
    }

    @Test
    @DisplayName("a body can no longer create a saved item for an arbitrary owner")
    void create_endpointIsGone() throws Exception {
        SavedItem hostile = new SavedItem.Builder().setId(savedItemId).build();

        mockMvc.perform(post("/api/saved-items")
                        .with(asStudent(callerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostile)))
                .andExpect(status().isMethodNotAllowed());

        verify(savedItemService, never()).read(any(), any());
    }

    @Test
    @DisplayName("saved-item endpoints reject anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/saved-items")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/saved-items/product/" + productId + "/toggle"))
                .andExpect(status().isUnauthorized());
        verify(savedItemService, never()).toggle(any(), any());
    }
}