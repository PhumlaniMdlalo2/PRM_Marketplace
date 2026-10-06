import { useCallback, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { MessageCircle, Pencil, Send, ThumbsUp, Trash2 } from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import Button from '../components/ui/Button';
import Layout from '../components/layout/Layout';
import {
  createComment,
  deleteComment,
  deletePost,
  getLikeCount,
  getPost,
  hasLikedPost,
  listComments,
  toggleLike,
  updateComment,
  updatePost,
} from '../api/bulletin';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';

const relativeTime = (value) => {
  if (!value) return '';
  const then = new Date(value);
  if (Number.isNaN(then.getTime())) return '';
  const minutes = Math.round((Date.now() - then.getTime()) / 60000);
  if (minutes < 60) return `${Math.max(minutes, 1)}m`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.round(hours / 24);
  if (days < 7) return `${days}d`;
  return then.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short' });
};

/**
 * The server returns one flat list in which a reply names its parent by id, so the tree is rebuilt
 * here. Depth is capped because the shape is client-assembled from ids that came off the wire: a
 * cycle or a chain deeper than anyone will read should render as text, not hang the page.
 */
const MAX_DEPTH = 6;

const POST_TITLE_LIMIT = 200;
const POST_BODY_LIMIT = 5000;
const COMMENT_BODY_LIMIT = 2000;

/**
 * Turns a failed edit or delete into something worth showing. The server answers 400 and 404 with
 * an empty body, and the shared client would otherwise dress both of them up as "Cannot reach the
 * server", which is a sentence that sends people to check a wifi setting that is fine.
 */
const describeFailure = (caught, { notFound, badRequest }) => {
  if (caught?.status === 404) return notFound;
  if (caught?.status === 400) return badRequest;
  return caught?.message ?? 'That change could not be saved.';
};

const nest = (comments) => {
  const nodes = new Map();
  for (const comment of comments) {
    nodes.set(comment.id, { ...comment, children: [] });
  }

  const roots = [];
  for (const node of nodes.values()) {
    const parent = node.parentId ? nodes.get(node.parentId) : null;
    // A parent that is not in this page, or that is the node itself, leaves it at the top level.
    if (parent && parent !== node) parent.children.push(node);
    else roots.push(node);
  }
  return roots;
};

const PostComments = () => {
  const { id } = useParams();
  const { user, isAuthenticated } = useAuth();

  const loadThread = useCallback(async () => {
    const [post, comments] = await Promise.all([getPost(id), listComments(id)]);

    let liked = false;
    let likes = post.likeCount ?? 0;
    if (isAuthenticated) {
      try {
        [liked, likes] = await Promise.all([hasLikedPost(id), getLikeCount(id)]);
      } catch {
        // Fall back to whatever the post already reported.
      }
    }

    return {
      post: {
        title: post.title,
        body: post.body,
        category: post.category,
        imageUrl: post.imageUrl ?? null,
        authorId: post.author?.id ?? null,
        authorName: post.author?.name ?? 'Unknown user',
        authorAvatar: post.author?.avatarUrl || null,
        time: relativeTime(post.createdAt),
        comments: comments.length,
        liked,
        likes,
      },
      comments: comments.map((comment) => ({
        id: comment.id,
        parentId: comment.parentId ?? null,
        authorId: comment.author?.id ?? null,
        authorName: comment.author?.name ?? 'Unknown user',
        authorAvatar: comment.author?.avatarUrl || null,
        body: comment.body,
        time: relativeTime(comment.createdAt),
      })),
    };
  }, [id, isAuthenticated]);

  const { data, loading, error, setData } = useAsync(loadThread);
  const navigate = useNavigate();
  const [draft, setDraft] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState(null);
  const [likeBusy, setLikeBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [editingPost, setEditingPost] = useState(false);
  const [titleDraft, setTitleDraft] = useState('');
  const [bodyDraft, setBodyDraft] = useState('');
  const [confirmingPostDelete, setConfirmingPostDelete] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [commentDraft, setCommentDraft] = useState('');
  const [confirmingCommentId, setConfirmingCommentId] = useState(null);

  const post = data?.post ?? null;
  const threads = nest(data?.comments ?? []);
  const replyTarget = (data?.comments ?? []).find((comment) => comment.id === replyTo) ?? null;

  const handleSend = async () => {
    const body = draft.trim();
    if (!body || sending) return;

    setSending(true);
    setSendError(null);
    try {
      await createComment(id, body, replyTo);
      // Refetched instead of appended locally: the server owns the post's comment count and decides
      // whether the new entry is a reply, so an appended entry would have to guess at both.
      const next = await loadThread();
      if (next) setData(next);
      setDraft('');
      setReplyTo(null);
    } catch (caught) {
      setSendError(caught.message);
    } finally {
      setSending(false);
    }
  };

  const onToggleLike = async () => {
    if (!isAuthenticated || likeBusy || !post) return;
    setLikeBusy(true);
    setSendError(null);
    try {
      const liked = await toggleLike(id);
      const likes = await getLikeCount(id);
      setData((current) => (
        current ? { ...current, post: { ...current.post, liked, likes } } : current
      ));
    } catch (caught) {
      setSendError(caught.message);
    } finally {
      setLikeBusy(false);
    }
  };

  /** The server refuses an edit or a delete that the caller did not write, so this is only ever a
   *  shortcut past a control that would have come back rejected anyway. */
  const owns = (authorId) => Boolean(
    isAuthenticated && authorId && user?.id && String(authorId) === String(user.id),
  );

  /** Refetched rather than patched by hand for anything the server owns — a delete takes its replies
   *  with it, and the post's comment count is the server's to restate. Read quietly: the mutation has
   *  already been accepted, so a re-read that fails must not put an error in front of the reader about
   *  something that worked. */
  const refreshQuietly = async () => {
    try {
      const next = await loadThread();
      if (next) setData(next);
    } catch {
      // Left as it is. The next visit to this thread picks up what the server holds.
    }
  };

  const startPostEdit = () => {
    setActionError(null);
    setTitleDraft(post?.title ?? '');
    setBodyDraft(post?.body ?? '');
    setEditingPost(true);
    setConfirmingPostDelete(false);
  };

  const savePost = async () => {
    const title = titleDraft.trim();
    const body = bodyDraft.trim();
    if (!title || !body || saving) return;
    setSaving(true);
    setActionError(null);
    try {
      await updatePost(id, { title, body, category: post?.category, imageUrl: post?.imageUrl });
    } catch (caught) {
      setActionError(describeFailure(caught, {
        notFound: 'That post is no longer yours to change.',
        badRequest: 'That post could not be saved.',
      }));
      setSaving(false);
      return;
    }
    // Shown from what was written rather than from the re-read, so the edit is on screen the moment
    // the server says yes.
    setData((current) => (
      current
        ? { ...current, post: { ...current.post, title, body } }
        : current
    ));
    setEditingPost(false);
    setSaving(false);
    await refreshQuietly();
  };

  const removePost = async () => {
    if (saving) return;
    setSaving(true);
    setActionError(null);
    try {
      await deletePost(id);
      navigate('/bulletin');
    } catch (caught) {
      setActionError(describeFailure(caught, {
        notFound: 'That post is already gone.',
        badRequest: 'That post could not be deleted.',
      }));
      setConfirmingPostDelete(false);
      setSaving(false);
    }
  };

  const startCommentEdit = (comment) => {
    setActionError(null);
    setCommentDraft(comment.body ?? '');
    setEditingCommentId(comment.id);
    setConfirmingCommentId(null);
  };

  const saveComment = async () => {
    const body = commentDraft.trim();
    if (!body || saving || !editingCommentId) return;
    const target = editingCommentId;
    setSaving(true);
    setActionError(null);
    try {
      await updateComment(target, body);
    } catch (caught) {
      setActionError(describeFailure(caught, {
        notFound: 'That comment is no longer yours to change.',
        badRequest: 'That comment could not be saved.',
      }));
      setSaving(false);
      return;
    }
    setData((current) => (
      current
        ? {
          ...current,
          comments: current.comments.map((entry) => (
            entry.id === target ? { ...entry, body } : entry
          )),
        }
        : current
    ));
    setEditingCommentId(null);
    setSaving(false);
    await refreshQuietly();
  };

  const removeComment = async () => {
    if (saving || !confirmingCommentId) return;
    setSaving(true);
    setActionError(null);
    try {
      await deleteComment(confirmingCommentId);
    } catch (caught) {
      if (caught?.status !== 404) {
        setActionError(describeFailure(caught, {
          notFound: 'That comment is already gone.',
          badRequest: 'That comment could not be deleted.',
        }));
        setConfirmingCommentId(null);
        setSaving(false);
        return;
      }
      // Already gone: take it off the screen rather than argue about who deleted it first.
    }
    setConfirmingCommentId(null);
    setSaving(false);
    await refreshQuietly();
  };

  const renderComment = (comment, depth = 0) => (
    <div key={comment.id} className={depth > 0 ? 'ml-8 mt-3' : ''}>
      <div className="flex items-start gap-3">
        <Avatar src={comment.authorAvatar} alt={comment.authorName} size="sm" />
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2">
            <h4 className="font-semibold text-text-primary text-sm truncate">{comment.authorName}</h4>
            <span className="text-xs text-text-muted whitespace-nowrap">{comment.time}</span>
          </div>

          {editingCommentId === comment.id ? (
            <div className="mt-1">
              <label htmlFor={`comment-${comment.id}`} className="sr-only">Edit your comment</label>
              <textarea
                id={`comment-${comment.id}`}
                value={commentDraft}
                onChange={(e) => setCommentDraft(e.target.value)}
                rows={3}
                maxLength={COMMENT_BODY_LIMIT}
                autoFocus
                className="w-full px-3 py-2 bg-lavender rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all resize-y"
              />
              <div className="mt-1.5 flex items-center justify-end gap-3">
                <button
                  onClick={() => setEditingCommentId(null)}
                  disabled={saving}
                  className="text-xs font-medium text-text-muted hover:text-text-primary disabled:opacity-50"
                >
                  Cancel
                </button>
                <Button
                  size="sm"
                  onClick={saveComment}
                  disabled={saving || !commentDraft.trim()}
                >
                  {saving ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </div>
          ) : (
            <>
              <p className="mt-1 text-text-secondary text-[15px] leading-relaxed whitespace-pre-wrap break-words">
                {comment.body}
              </p>
              {confirmingCommentId === comment.id ? (
                <div
                  className="mt-2 p-2.5 bg-red-50 border border-red-200 rounded-xl flex flex-wrap items-center justify-between gap-2"
                  role="alert"
                >
                  <p className="text-xs text-red-800">Delete this comment? Replies go with it.</p>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setConfirmingCommentId(null)}
                      disabled={saving}
                      className="text-xs font-medium text-text-muted hover:text-text-primary disabled:opacity-50"
                    >
                      Keep it
                    </button>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={removeComment}
                      disabled={saving}
                    >
                      {saving ? 'Deleting…' : 'Delete for good'}
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-4">
                  {isAuthenticated && depth < MAX_DEPTH - 1 && (
                    <button
                      onClick={() => setReplyTo(replyTo === comment.id ? null : comment.id)}
                      className={`flex items-center gap-1 mt-1.5 text-xs font-medium transition-colors ${
                        replyTo === comment.id ? 'text-primary' : 'text-text-muted hover:text-primary'
                      }`}
                    >
                      <MessageCircle size={13} />
                      {replyTo === comment.id ? 'Cancel reply' : 'Reply'}
                    </button>
                  )}
                  {owns(comment.authorId) && (
                    <button
                      onClick={() => startCommentEdit(comment)}
                      aria-label="Edit your comment"
                      className="flex items-center gap-1 mt-1.5 text-xs font-medium text-text-muted hover:text-primary transition-colors"
                    >
                      <Pencil size={13} />
                      Edit
                    </button>
                  )}
                  {owns(comment.authorId) && (
                    <button
                      onClick={() => {
                        setActionError(null);
                        setConfirmingCommentId(comment.id);
                        setEditingCommentId(null);
                      }}
                      aria-label="Delete your comment"
                      className="flex items-center gap-1 mt-1.5 text-xs font-medium text-text-muted hover:text-error transition-colors"
                    >
                      <Trash2 size={13} />
                      Delete
                    </button>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
      {comment.children.map((child) => renderComment(child, depth + 1))}
    </div>
  );

  return (
    <Layout showNav={false}>
      <div className="app-container py-6 pb-32 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-xl font-bold text-text-primary tracking-tight">
            Comments{' '}
            <span className="text-text-muted font-medium">({post?.comments ?? 0})</span>
          </h1>
        </div>

        {actionError && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{actionError}</p>
          </div>
        )}

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}

        {post && (
          <article className="bg-white border border-border rounded-2xl p-4 mb-6">
            <div className="flex items-start gap-3">
              <Avatar src={post.authorAvatar} alt={post.authorName} size="sm" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <h3 className="font-semibold text-text-primary truncate">{post.authorName}</h3>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="text-xs text-text-muted whitespace-nowrap">{post.time}</span>
                    {owns(post.authorId) && !editingPost && !confirmingPostDelete && (
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={startPostEdit}
                          aria-label="Edit your post"
                          className="p-1 text-text-muted hover:text-primary transition-colors"
                        >
                          <Pencil size={14} />
                        </button>
                        <button
                          onClick={() => {
                            setActionError(null);
                            setConfirmingPostDelete(true);
                          }}
                          aria-label="Delete your post"
                          className="p-1 text-text-muted hover:text-error transition-colors"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {confirmingPostDelete && (
                  <div
                    className="mt-3 p-3 bg-red-50 border border-red-200 rounded-xl flex flex-wrap items-center justify-between gap-2"
                    role="alert"
                  >
                    <p className="text-sm text-red-800">Delete this post and every comment on it?</p>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setConfirmingPostDelete(false)}
                        disabled={saving}
                        className="text-xs font-medium text-text-muted hover:text-text-primary disabled:opacity-50"
                      >
                        Keep it
                      </button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={removePost}
                        disabled={saving}
                      >
                        {saving ? 'Deleting…' : 'Delete for good'}
                      </Button>
                    </div>
                  </div>
                )}

                {editingPost ? (
                  <div className="mt-2 space-y-2">
                    <label htmlFor="post-title" className="sr-only">Post title</label>
                    <input
                      id="post-title"
                      type="text"
                      value={titleDraft}
                      onChange={(e) => setTitleDraft(e.target.value)}
                      maxLength={POST_TITLE_LIMIT}
                      className="w-full px-3 py-2 bg-lavender rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all"
                    />
                    <label htmlFor="post-body" className="sr-only">Post content</label>
                    <textarea
                      id="post-body"
                      value={bodyDraft}
                      onChange={(e) => setBodyDraft(e.target.value)}
                      rows={6}
                      maxLength={POST_BODY_LIMIT}
                      autoFocus
                      className="w-full px-3 py-2 bg-lavender rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all resize-y"
                    />
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs text-text-muted">
                        {bodyDraft.length} / {POST_BODY_LIMIT}
                      </span>
                      <div className="flex items-center gap-3">
                        <button
                          onClick={() => setEditingPost(false)}
                          disabled={saving}
                          className="text-xs font-medium text-text-muted hover:text-text-primary disabled:opacity-50"
                        >
                          Cancel
                        </button>
                        <Button
                          size="sm"
                          onClick={savePost}
                          disabled={saving || !titleDraft.trim() || !bodyDraft.trim()}
                        >
                          {saving ? 'Saving…' : 'Save changes'}
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <>
                    {post.title && (
                      <h4 className="mt-2 font-semibold text-text-primary text-[15px] leading-snug">
                        {post.title}
                      </h4>
                    )}
                    <p className="mt-1 text-text-secondary leading-relaxed text-wrap-pretty whitespace-pre-wrap">
                      {post.body}
                    </p>
                    {post.category && (
                      <div className="mt-2.5 flex gap-1.5 flex-wrap">
                        <span className="text-xs font-medium text-primary bg-primary-muted px-2 py-0.5 rounded-md">
                          #{post.category}
                        </span>
                      </div>
                    )}
                  </>
                )}
                <div className="mt-3.5 pt-3 border-t border-border flex items-center gap-6">
                  <button
                    onClick={onToggleLike}
                    disabled={!isAuthenticated || likeBusy}
                    aria-pressed={post.liked}
                    className={`flex items-center gap-1.5 transition-colors disabled:opacity-60 ${
                      post.liked ? 'text-primary' : 'text-text-secondary hover:text-primary'
                    }`}
                  >
                    <ThumbsUp size={16} className={post.liked ? 'fill-primary' : ''} />
                    <span className="text-sm font-medium">{post.likes}</span>
                  </button>
                  <span className="flex items-center gap-1.5 text-text-secondary">
                    <MessageCircle size={16} />
                    <span className="text-sm font-medium">{post.comments}</span>
                  </span>
                </div>
              </div>
            </div>
          </article>
        )}

        {!loading && !error && threads.length === 0 && (
          <p className="text-center text-sm text-text-muted py-10">
            No comments yet. Be the first to reply.
          </p>
        )}

        <div className="space-y-4">
          {threads.map((comment) => renderComment(comment))}
        </div>
      </div>

      <div className="fixed bottom-0 left-0 right-0 bg-white/95 backdrop-blur border-t border-border p-4 z-40 safe-area-inset-bottom">
        <div className="app-container flex flex-col gap-2 max-w-2xl mx-auto">
          {replyTarget && (
            <div className="flex items-center justify-between gap-2 text-xs text-text-muted">
              <span className="truncate">
                Replying to <span className="font-medium text-text-secondary">{replyTarget.authorName}</span>
              </span>
              <button onClick={() => setReplyTo(null)} className="hover:text-text-primary shrink-0">
                Cancel
              </button>
            </div>
          )}

          {sendError && (
            <p className="text-sm text-red-700" role="alert">{sendError}</p>
          )}

          {!isAuthenticated ? (
            <p className="text-sm text-text-secondary text-center py-2">
              <Link to="/login" state={{ from: `/bulletin/${id}` }} className="text-primary font-medium">
                Sign in
              </Link>{' '}
              to join the conversation.
            </p>
          ) : (
            <div className="flex items-center gap-3">
              <Avatar size="sm" src={user?.avatarUrl || null} alt={user?.name ?? 'You'} />
              <input
                type="text"
                value={draft}
                onChange={(e) => {
                  setDraft(e.target.value);
                  if (sendError) setSendError(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSend();
                }}
                placeholder={replyTarget ? 'Write a reply…' : 'Add a comment...'}
                aria-label={replyTarget ? 'Write a reply' : 'Add a comment'}
                className="flex-1 px-4 py-2.5 bg-lavender rounded-full text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all duration-200 text-sm"
              />
              <button
                onClick={handleSend}
                disabled={!draft.trim() || sending}
                className="w-10 h-10 bg-primary rounded-full flex items-center justify-center text-white hover:bg-primary-hover active:scale-90 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
                aria-label={replyTarget ? 'Post reply' : 'Post comment'}
              >
                <Send size={16} />
              </button>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default PostComments;