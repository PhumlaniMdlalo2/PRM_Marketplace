import { MessageCircle, ThumbsUp, Plus } from 'lucide-react';
import { useCallback, useState } from 'react';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { Link } from 'react-router-dom';
import { getLikeCount, hasLikedPost, listPosts, toggleLike } from '../api/bulletin';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';

/**
 * "4h" / "2d" / "12 Aug". Same hand-rolled ladder as the messages list.
 */
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

const Bulletin = () => {
  const { isAuthenticated } = useAuth();

  /**
   * Posts carry their own like and comment counts, so the list needs one request. The caller's own
   * liked-state is separate — the count cannot say who liked it — so that is one more call, made
   * only when signed in.
   *
   * A post whose like lookup fails still renders; it just shows as unliked, which is recoverable by
   * tapping the button.
   */
  const loadPosts = useCallback(async () => {
    const posts = await listPosts();

    return Promise.all(posts.map(async (post) => {
      let liked = false;
      if (isAuthenticated) {
        try {
          liked = await hasLikedPost(post.id);
        } catch {
          liked = false;
        }
      }
      return {
        id: post.id,
        title: post.title,
        body: post.body,
        category: post.category,
        imageUrl: post.imageUrl || null,
        authorName: post.author?.name ?? 'Unknown user',
        authorAvatar: post.author?.avatarUrl || null,
        time: relativeTime(post.createdAt),
        likes: post.likeCount ?? 0,
        comments: post.commentCount ?? 0,
        liked,
      };
    }));
  }, [isAuthenticated]);

  const { data: posts, loading, error, setData } = useAsync(loadPosts);
  const [busyPost, setBusyPost] = useState(null);
  const [likeError, setLikeError] = useState(null);

  /**
   * The server decides whether a tap adds or removes a like, and answers with the new state, so the
   * count is read back rather than guessed at with `+ (liked ? 1 : -1)`. Guessing is how two rapid
   * taps end up disagreeing with the database.
   */
  const onToggleLike = async (post) => {
    if (!isAuthenticated) {
      setLikeError('Sign in to like posts.');
      return;
    }
    if (busyPost) return;

    setBusyPost(post.id);
    setLikeError(null);
    try {
      const liked = await toggleLike(post.id);
      const count = await getLikeCount(post.id);
      setData((current) => (current ?? []).map((entry) => (
        entry.id === post.id ? { ...entry, liked, likes: count } : entry
      )));
    } catch (caught) {
      setLikeError(caught.message);
    } finally {
      setBusyPost(null);
    }
  };

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight mb-6">Bulletin</h1>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}
        {likeError && (
          <p className="mb-4 text-sm text-red-700" role="alert">{likeError}</p>
        )}

        <div className="space-y-3.5">
          {(posts ?? []).map((post) => (
            <article key={post.id} className="bg-white border border-border rounded-2xl p-4">
              <div className="flex items-start gap-3">
                <Avatar src={post.authorAvatar} alt={post.authorName} size="sm" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="font-semibold text-text-primary truncate">{post.authorName}</h3>
                    <span className="text-xs text-text-muted whitespace-nowrap">{post.time}</span>
                  </div>

                  {post.title && (
                    <h4 className="mt-2 font-semibold text-text-primary text-[15px] leading-snug">
                      {post.title}
                    </h4>
                  )}
                  <p className="mt-1 text-text-secondary leading-relaxed text-wrap-pretty whitespace-pre-wrap">
                    {post.body}
                  </p>

                  {post.category && (
                    <div className="mt-3 flex gap-1.5 flex-wrap">
                      <span className="text-xs font-medium text-primary bg-primary-muted px-2 py-0.5 rounded-md">
                        #{post.category}
                      </span>
                    </div>
                  )}

                  <div className="mt-4 pt-3 border-t border-border flex items-center gap-6">
                    <button
                      onClick={() => onToggleLike(post)}
                      disabled={busyPost === post.id}
                      className={`flex items-center gap-1.5 transition-all duration-200 active:scale-90 disabled:opacity-50 ${
                        post.liked ? 'text-primary' : 'text-text-secondary hover:text-primary'
                      }`}
                      aria-pressed={post.liked}
                    >
                      <ThumbsUp size={17} className={post.liked ? 'fill-primary' : ''} />
                      <span className="text-sm font-medium">{post.likes}</span>
                    </button>

                    <Link
                      to={`/bulletin/${post.id}`}
                      className="flex items-center gap-1.5 text-text-secondary hover:text-primary transition-colors"
                    >
                      <MessageCircle size={17} />
                      <span className="text-sm font-medium">{post.comments}</span>
                    </Link>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </div>

        {!loading && (posts ?? []).length === 0 && (
          <div className="py-16 text-center">
            <p className="text-text-secondary">Nothing on the board yet</p>
            <p className="text-sm text-text-muted mt-1">Be the first to post something.</p>
          </div>
        )}

        <Link
          to="/bulletin/create"
          className="fixed bottom-24 right-4 lg:bottom-6 w-14 h-14 bg-primary rounded-full flex items-center justify-center shadow-lg shadow-primary/30 hover:bg-primary-hover hover:shadow-primary/40 active:scale-90 transition-all duration-200 z-40"
          aria-label="Create a post"
        >
          <Plus size={26} className="text-white" />
        </Link>
      </div>
    </Layout>
  );
};

export default Bulletin;