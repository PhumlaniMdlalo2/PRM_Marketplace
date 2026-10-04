import { api } from './client';

/**
 * The bulletin board: posts, their comments, and likes.
 *
 * Reads are public on the server; writes are not. Author and owner come from the token — the create
 * functions below deliberately send only the content fields, because anything else in the body is
 * either ignored or, worse, honoured.
 */

export const listPosts = async () => {
  const { data } = await api.get('/bulletin-posts');
  return data;
};

export const getPost = async (id) => {
  const { data } = await api.get(`/bulletin-posts/${id}`);
  return data;
};

/**
 * Publishes a post.
 *
 * Only title, body, category and imageUrl are sent. The author is taken from the token, so there is
 * nothing here that could attribute a post to somebody else.
 */
export const createPost = async (post) => {
  const { data } = await api.post('/bulletin-posts', {
    title: post.title,
    body: post.body,
    category: post.category ?? null,
    imageUrl: post.imageUrl || null,
  });
  return data;
};

/** Edits an existing post. The server refuses this unless the caller wrote it. */
export const updatePost = async (id, post) => {
  const { data } = await api.put(`/bulletin-posts/${id}`, {
    title: post.title,
    body: post.body,
    category: post.category ?? null,
    imageUrl: post.imageUrl || null,
  });
  return data;
};

/** Deletes a post. The server refuses this unless the caller wrote it. */
export const deletePost = async (id) => {
  await api.delete(`/bulletin-posts/${id}`);
};

/** The whole comment thread on a post. */
export const listComments = async (postId) => {
  const { data } = await api.get(`/comments/post/${postId}`);
  return data;
};

/** Only the top-level comments, with no replies loaded. */
export const listTopLevelComments = async (postId) => {
  const { data } = await api.get(`/comments/post/${postId}/top-level`);
  return data;
};

/** Replies to one comment. */
export const listReplies = async (postId, parentId) => {
  const { data } = await api.get(`/comments/post/${postId}/replies/${parentId}`);
  return data;
};

/**
 * Adds a comment.
 *
 * `postId` and an optional `parentId` identify what is being replied to; the author comes from the
 * token. `reply` is derived server-side from whether a parent was given, so it is not sent.
 */
export const createComment = async (postId, body, parentId = null) => {
  const { data } = await api.post('/comments', {
    post: postId ? { id: postId } : null,
    body,
    parent: parentId ? { id: parentId } : null,
  });
  return data;
};

/**
 * Likes or unlikes a post.
 *
 * The endpoint answers 201 with the new like, or 204 with no body when the like was removed, so the
 * return value is normalised here to the one thing a caller actually wants: whether the post is
 * liked by this caller *now*. Asking the server twice would work too, but the status code already
 * says the answer and one round trip is one too many on a button.
 *
 * @returns {boolean} true when the post is now liked
 */
export const toggleLike = async (postId) => {
  const response = await api.post(`/post-likes/post/${postId}/toggle`);
  return response.status === 201;
};

/** How many likes a post has. */
export const getLikeCount = async (postId) => {
  const { data } = await api.get(`/post-likes/post/${postId}/count`);
  return data ?? 0;
};

/** Whether the signed-in caller has liked a post. */
export const hasLikedPost = async (postId) => {
  const { data } = await api.get(`/post-likes/post/${postId}`);
  return Boolean(data);
};