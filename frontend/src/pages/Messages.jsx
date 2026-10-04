import { useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import BackButton from '../components/ui/BackButton';
import Avatar from '../components/ui/Avatar';
import Layout from '../components/layout/Layout';
import { listConversations, listMessages } from '../api/messages';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';

/**
 * "2m" / "1h" / "Yesterday" / "12 Aug".
 *
 * Deliberately hand-rolled rather than Intl.RelativeTimeFormat: this is the only place that needs
 * it, and the thresholds are the ones people read at a glance in a message list, not the ones a
 * general-purpose formatter would round to.
 */
const relativeTime = (value) => {
  if (!value) return '';
  const then = new Date(value);
  if (Number.isNaN(then.getTime())) return '';

  const minutes = Math.round((Date.now() - then.getTime()) / 60000);
  if (minutes < 1) return 'now';
  if (minutes < 60) return `${minutes}m`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h`;

  const days = Math.round(hours / 24);
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days}d`;

  return then.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short' });
};

const Messages = () => {
  const { user } = useAuth();
  const myId = user?.id ?? null;

  /**
   * The conversation list carries no preview text and no unread count, so each thread is fetched to
   * get its last message. That is N+1 by necessity rather than by choice, bounded by the caller's
   * own thread count, and the requests are independent so they go out together.
   *
   * A thread that fails to load degrades to showing the listing name with no preview, which is far
   * better than dropping the conversation from the list entirely.
   */
  const loadConversations = useCallback(async () => {
    const conversations = await listConversations();

    return Promise.all(conversations.map(async (conversation) => {
      const other = conversation.buyer?.id === myId ? conversation.seller : conversation.buyer;

      let last = null;
      let unread = 0;
      try {
        const messages = await listMessages(conversation.id);
        last = messages.length > 0 ? messages[messages.length - 1] : null;
        unread = messages.filter(
          (entry) => entry.sender?.id !== myId && !entry.readAt,
        ).length;
      } catch {
        // Left as null/0; the row still renders.
      }

      return {
        id: conversation.id,
        name: other?.name ?? 'Unknown user',
        avatar: other?.avatarUrl || null,
        productName: conversation.product?.name ?? null,
        preview: last?.body ?? null,
        time: last?.sentAt ?? conversation.lastMessageAt,
        unread,
      };
    }));
  }, [myId]);

  const { data, loading, error } = useAsync(loadConversations);
  const conversations = useMemo(() => data ?? [], [data]);

  return (
    <Layout>
      <div className="app-container py-6 max-w-2xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Messages</h1>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
            <p className="text-sm text-red-800">{error.message}</p>
          </div>
        )}

        <div className="space-y-1">
          {conversations.map((conversation) => (
            <Link
              key={conversation.id}
              to={`/messages/${conversation.id}`}
              className="w-full flex items-center gap-4 p-3 rounded-2xl hover:bg-lavender/70 active:bg-lavender transition-colors"
            >
              <span className="relative">
                <Avatar src={conversation.avatar} alt={conversation.name} />
                {conversation.unread > 0 && (
                  <span
                    className="absolute -top-0.5 -right-0.5 min-w-3 h-3 px-1 bg-primary rounded-full border-2 border-white flex items-center justify-center text-[8px] font-bold text-white"
                    aria-label={`${conversation.unread} unread`}
                  >
                    {conversation.unread}
                  </span>
                )}
              </span>
              <span className="flex-1 min-w-0">
                <span className="flex items-center justify-between gap-2">
                  <span className="font-semibold text-text-primary truncate">{conversation.name}</span>
                  <span className="text-xs text-text-muted whitespace-nowrap">
                    {relativeTime(conversation.time)}
                  </span>
                </span>
                {conversation.productName && (
                  <span className="block text-xs text-text-muted mt-0.5 truncate">
                    {conversation.productName}
                  </span>
                )}
                <span className={`block text-sm mt-0.5 truncate ${
                  conversation.unread > 0 ? 'text-text-primary font-medium' : 'text-text-secondary'
                }`}
                >
                  {conversation.preview ?? 'No messages yet'}
                </span>
              </span>
            </Link>
          ))}
        </div>

        {!loading && conversations.length === 0 && (
          <div className="py-16 text-center">
            <p className="text-text-secondary">No conversations yet</p>
            <p className="text-sm text-text-muted mt-1">
              Messages from sellers appear here once you ask a seller about a listing.
            </p>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default Messages;