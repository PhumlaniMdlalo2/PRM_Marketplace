import { ArrowBigUp, Clock3, Flame, MessageCircle, Plus, Search, Trophy, X } from 'lucide-react';
import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';
import { getLikeCount, hasLikedPost, listPosts, toggleLike } from '../api/bulletin';

const SORTS = [
  { id: 'hot', label: 'Hot', icon: Flame },
  { id: 'new', label: 'New', icon: Clock3 },
  { id: 'top', label: 'Top', icon: Trophy },
];

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

const sortPosts = (posts, sort) => [...posts].sort((a, b) => {
  const timeA = new Date(a.createdAt).getTime() || 0;
  const timeB = new Date(b.createdAt).getTime() || 0;
  if (sort === 'new') return timeB - timeA;
  if (sort === 'top') return (b.likes - a.likes) || (b.comments - a.comments) || (timeB - timeA);

  const ageA = Math.max((Date.now() - timeA) / 3_600_000, 1);
  const ageB = Math.max((Date.now() - timeB) / 3_600_000, 1);
  const hotA = (a.likes + a.comments + 1) / Math.pow(ageA + 2, 1.35);
  const hotB = (b.likes + b.comments + 1) / Math.pow(ageB + 2, 1.35);
  return hotB - hotA || timeB - timeA;
});

const Bulletin = () => {
  const { isAuthenticated, user } = useAuth();
  const [sort, setSort] = useState('hot');
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');

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
        createdAt: post.createdAt,
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
  const categories = [...new Set((posts ?? []).map((post) => post.category).filter(Boolean))]
    .sort((a, b) => a.localeCompare(b));
  const normalizedSearch = search.trim().toLocaleLowerCase();
  const visiblePosts = sortPosts((posts ?? []).filter((post) => {
    const matchesCategory = !category || post.category === category;
    const searchable = `${post.title} ${post.body} ${post.category ?? ''} ${post.authorName}`
      .toLocaleLowerCase();
    return matchesCategory && (!normalizedSearch || searchable.includes(normalizedSearch));
  }), sort);

  const onToggleLike = async (post) => {
    if (!isAuthenticated) {
      setLikeError('Sign in to upvote discussions.');
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

  const selectCategory = (topic) => setCategory((current) => current === topic ? '' : topic);

  return (
    <Layout>
      <div className="app-container max-w-6xl py-6">
        <div className="grid gap-7 lg:grid-cols-[minmax(0,1fr)_17rem]">
          <main className="min-w-0">
            <header className="mb-5">
              <p className="text-sm font-semibold text-primary">Campus conversations</p>
              <h1 className="mt-1 text-3xl font-bold tracking-tight text-text-primary">Bulletin</h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-text-secondary">
                Ask questions, share useful finds and join in with what’s happening around campus.
              </p>
              {user?.role === 'STUDENT' && (
                <Link to="/student-groups" className="mt-3 inline-flex items-center rounded-lg bg-lavender px-3 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary-muted">
                  Explore student groups
                </Link>
              )}
            </header>

            <label className="flex min-h-11 items-center gap-2 rounded-xl border border-border bg-white px-3 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20">
              <Search size={17} className="shrink-0 text-text-muted" aria-hidden="true" />
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search discussions"
                aria-label="Search discussions"
                className="min-w-0 flex-1 bg-transparent text-sm text-text-primary outline-none placeholder:text-text-muted"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch('')}
                  className="rounded-md p-1 text-text-muted hover:bg-lavender hover:text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                  aria-label="Clear search"
                >
                  <X size={15} aria-hidden="true" />
                </button>
              )}
            </label>

            <nav className="mt-4 flex gap-1 border-b border-border" aria-label="Sort discussions">
              {SORTS.map(({ id, label, icon: Icon }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => setSort(id)}
                  aria-pressed={sort === id}
                  className={`inline-flex min-h-11 items-center gap-2 border-b-2 px-4 text-sm font-semibold transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary ${
                    sort === id
                      ? 'border-primary text-primary'
                      : 'border-transparent text-text-secondary hover:text-text-primary'
                  }`}
                >
                  <Icon size={16} aria-hidden="true" />
                  {label}
                </button>
              ))}
            </nav>

            {error && (
              <div className="mb-4 mt-4 rounded-2xl border border-red-200 bg-red-50 p-4" role="alert">
                <p className="text-sm text-red-800">{error.message}</p>
              </div>
            )}
            {likeError && <p className="mb-4 mt-4 text-sm text-red-700" role="alert">{likeError}</p>}

            {categories.length > 0 && (
              <div className="flex flex-wrap items-center gap-2 py-4" aria-label="Filter by topic">
                <button
                  type="button"
                  onClick={() => setCategory('')}
                  aria-pressed={!category}
                  className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                    !category ? 'bg-primary text-white' : 'bg-lavender text-text-secondary hover:text-text-primary'
                  }`}
                >
                  All topics
                </button>
                {categories.map((topic) => (
                  <button
                    key={topic}
                    type="button"
                    onClick={() => selectCategory(topic)}
                    aria-pressed={category === topic}
                    className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                      category === topic ? 'bg-primary text-white' : 'bg-lavender text-text-secondary hover:text-text-primary'
                    }`}
                  >
                    {topic}
                  </button>
                ))}
              </div>
            )}

            <div className="divide-y divide-border overflow-hidden rounded-2xl border border-border bg-white">
              {visiblePosts.map((post) => (
                <article key={post.id} className="flex gap-3 p-4 transition-colors hover:bg-lavender/20 sm:p-5">
                  <div className="flex w-9 shrink-0 flex-col items-center gap-0.5 rounded-lg bg-lavender/60 py-1">
                    <button
                      type="button"
                      onClick={() => onToggleLike(post)}
                      disabled={busyPost === post.id}
                      aria-label={`${post.liked ? 'Remove upvote from' : 'Upvote'} ${post.title}`}
                      aria-pressed={post.liked}
                      title={isAuthenticated ? 'Upvote' : 'Sign in to upvote'}
                      className={`rounded-md p-1 transition-colors hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary disabled:opacity-50 ${
                        post.liked ? 'text-primary' : 'text-text-secondary hover:text-primary'
                      }`}
                    >
                      <ArrowBigUp size={21} className={post.liked ? 'fill-primary/20' : ''} aria-hidden="true" />
                    </button>
                    <span className={`text-xs font-bold tabular-nums ${post.liked ? 'text-primary' : 'text-text-secondary'}`}>
                      {post.likes}
                    </span>
                  </div>

                  <div className="min-w-0 flex-1">
                    <div className="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-xs text-text-muted">
                      {post.category && (
                        <>
                          <button
                            type="button"
                            onClick={() => selectCategory(post.category)}
                            className="font-semibold text-primary hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                          >
                            {post.category}
                          </button>
                          <span aria-hidden="true">·</span>
                        </>
                      )}
                      <Avatar src={post.authorAvatar} alt={post.authorName} size="sm" />
                      <span className="truncate font-medium text-text-secondary">{post.authorName}</span>
                      <span aria-hidden="true">·</span>
                      <time className="whitespace-nowrap">{post.time}</time>
                    </div>

                    <Link
                      to={`/bulletin/${post.id}`}
                      className="group block rounded-md focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                    >
                      {post.title && (
                        <h2 className="mt-2 text-lg font-semibold leading-snug tracking-tight text-text-primary group-hover:text-primary">
                          {post.title}
                        </h2>
                      )}
                      <p className="mt-1 line-clamp-4 whitespace-pre-wrap text-sm leading-6 text-text-secondary">
                        {post.body}
                      </p>
                      {post.imageUrl && (
                        <img
                          src={post.imageUrl}
                          alt={`Image attached to ${post.title || 'discussion'}`}
                          className="mt-3 max-h-80 w-full rounded-xl border border-border object-cover"
                        />
                      )}
                    </Link>

                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <Link
                        to={`/bulletin/${post.id}`}
                        className="inline-flex min-h-9 items-center gap-2 rounded-lg px-2.5 text-xs font-semibold text-text-secondary transition-colors hover:bg-lavender hover:text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                      >
                        <MessageCircle size={16} aria-hidden="true" />
                        {post.comments} {post.comments === 1 ? 'comment' : 'comments'}
                      </Link>
                      <Link
                        to={`/bulletin/${post.id}`}
                        className="inline-flex min-h-9 items-center rounded-lg px-2.5 text-xs font-semibold text-text-secondary transition-colors hover:bg-lavender hover:text-text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                      >
                        Join discussion
                      </Link>
                    </div>
                  </div>
                </article>
              ))}
            </div>

            {!loading && visiblePosts.length === 0 && (
              <div className="mt-4 rounded-2xl border border-dashed border-border bg-white px-6 py-12 text-center">
                <p className="font-semibold text-text-primary">
                  {(posts ?? []).length === 0 ? 'Nothing on the board yet' : 'No discussions match that'}
                </p>
                <p className="mt-1 text-sm text-text-muted">
                  {(posts ?? []).length === 0
                    ? 'Start a conversation and give the campus something to talk about.'
                    : 'Try another search or topic, or clear your filters.'}
                </p>
                {(search || category) && (
                  <button
                    type="button"
                    onClick={() => {
                      setSearch('');
                      setCategory('');
                    }}
                    className="mt-3 text-sm font-semibold text-primary hover:underline"
                  >
                    Clear filters
                  </button>
                )}
              </div>
            )}
          </main>

          <aside className="hidden lg:block">
            <div className="sticky top-24 rounded-2xl border border-border bg-white p-5">
              <h2 className="font-semibold text-text-primary">A good place to start</h2>
              <p className="mt-2 text-sm leading-6 text-text-secondary">
                Share a clear question, add a topic and keep replies useful to other students.
              </p>
              <Link
                to="/bulletin/create"
                className="mt-4 inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 text-sm font-semibold text-white transition hover:bg-primary-hover active:scale-[0.98] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              >
                <Plus size={17} aria-hidden="true" />
                Start a discussion
              </Link>
              <div className="mt-5 border-t border-border pt-4">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-text-muted">Browse topics</h3>
                {categories.length ? (
                  <ul className="mt-2 space-y-1">
                    {categories.map((topic) => (
                      <li key={topic}>
                        <button
                          type="button"
                          onClick={() => selectCategory(topic)}
                          className={`w-full rounded-lg px-2.5 py-2 text-left text-sm transition-colors hover:bg-lavender focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary ${
                            category === topic ? 'font-semibold text-primary' : 'text-text-secondary'
                          }`}
                        >
                          {topic}
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-2 text-sm text-text-muted">Topics appear as people post.</p>
                )}
              </div>
            </div>
          </aside>
        </div>

        <Link
          to="/bulletin/create"
          className="fixed bottom-24 right-4 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-primary shadow-lg shadow-primary/30 transition-all duration-200 hover:bg-primary-hover hover:shadow-primary/40 active:scale-90 lg:bottom-6"
          aria-label="Create a post"
        >
          <Plus size={26} className="text-white" />
        </Link>
      </div>
    </Layout>
  );
};

export default Bulletin;
