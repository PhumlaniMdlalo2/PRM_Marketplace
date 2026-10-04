import { api } from './client';

/**
 * Conversations and messages.
 *
 * Every read and write is scoped to the token on the server: the conversation list is the caller's,
 * `read`/`send`/`markRead` all check that the caller is a participant, and there is no
 * "conversationId from the client decides who I am" anywhere in these calls.
 */

export const listConversations = async () => {
  const { data } = await api.get('/conversations');
  return data;
};

export const getConversation = async (id) => {
  const { data } = await api.get(`/conversations/${id}`);
  return data;
};

/** Total unread across every conversation. */
export const unreadCount = async () => {
  const { data } = await api.get('/conversations/unread-count');
  return data ?? 0;
};

/**
 * Opens, or reuses, the thread between the caller and a seller about a listing.
 *
 * `productId` is optional and is only used to label the thread; the server pairs the two accounts
 * rather than creating a duplicate conversation per listing.
 */
export const startConversation = async (sellerId, productId) => {
  const params = productId ? { sellerId, productId } : { sellerId };
  const { data } = await api.post('/conversations/start', null, { params });
  return data;
};

export const deleteConversation = async (id) => {
  await api.delete(`/conversations/${id}`);
};

/** The whole thread, oldest first. */
export const listMessages = async (conversationId) => {
  const { data } = await api.get(`/messages/conversation/${conversationId}`);
  return data;
};

/**
 * Sends a message.
 *
 * The text travels as a `body` query parameter rather than in a request body, because that is how
 * the endpoint is written. See the note on MessageController — this belongs in the body, and until
 * it moves, message text will appear in server access logs. Nothing on the client can prevent that,
 * so it is flagged rather than worked around.
 */
export const sendMessage = async (conversationId, body) => {
  const { data } = await api.post(
    `/messages/conversation/${conversationId}/send`,
    null,
    { params: { body } },
  );
  return data;
};

/** Marks the other party's messages as read. Answers with how many it updated. */
export const markConversationRead = async (conversationId) => {
  const { data } = await api.patch(`/messages/conversation/${conversationId}/read`);
  return data?.markedRead ?? 0;
}