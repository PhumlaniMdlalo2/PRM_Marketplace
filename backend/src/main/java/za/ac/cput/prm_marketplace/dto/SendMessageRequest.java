package za.ac.cput.prm_marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/messages/conversation/{conversationId}/send}.
 *
 * <p>This is a request body rather than a {@code body} request parameter because message text is
 * private correspondence between two named accounts, and parameters travel in the request line. That
 * put every message into the access log, any proxy log in between, and the browser's history — none
 * of which the sender or the recipient agreed to share with whoever reads those logs. A message
 * asking about an address, a price, or anything else a buyer would rather not broadcast should not
 * have a wider audience than the two people in the thread.
 *
 * <p>The size limit keeps one message from becoming a bulk-storage route. It matches the ceiling in
 * {@link Message}'s column so the two cannot disagree.
 */
public record SendMessageRequest(
        @NotBlank(message = "Message body is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String body
) {
}