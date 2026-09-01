package za.ac.cput.prm_marketplace.controller;

import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.service.IUserService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUserService userService;

    private User buildUser(UUID id) {
        return new User.Builder()
                .setId(id)
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
                .build();
    }

    @Test
    void createReturnsCreatedWhenServiceSucceeds() throws Exception {
        User user = buildUser(null);
        User saved = buildUser(UUID.randomUUID());
        when(userService.create(any(User.class))).thenReturn(saved);

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void createReturnsBadRequestWhenServiceRejects() throws Exception {
        User user = buildUser(null);
        when(userService.create(any(User.class))).thenReturn(null);

        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readReturnsUserWhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.read(id)).thenReturn(buildUser(id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void readReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.read(id)).thenReturn(null);

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsOkWhenServiceSucceeds() throws Exception {
        User user = buildUser(UUID.randomUUID());
        when(userService.update(any(User.class))).thenReturn(user);

        mockMvc.perform(put("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    @Test
    void updateReturnsNotFoundWhenServiceRejects() throws Exception {
        User user = buildUser(UUID.randomUUID());
        when(userService.update(any(User.class))).thenReturn(null);

        mockMvc.perform(put("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNoContentWhenDeleted() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllReturnsListOfUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getAll()).thenReturn(List.of(buildUser(id)));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("jane@example.com"));
    }

    @Test
    void findByEmailReturnsUserWhenFound() throws Exception {
        User user = buildUser(UUID.randomUUID());
        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/email/{email}", "jane@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void findByEmailReturnsNotFoundWhenMissing() throws Exception {
        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/email/{email}", "missing@example.com"))
                .andExpect(status().isNotFound());
    }
}
