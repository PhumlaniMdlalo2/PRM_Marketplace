import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Send } from 'lucide-react';
import Avatar from '../components/ui/Avatar';
import BackButton from '../components/ui/BackButton';
import { getConversation, listMessages, markConversationRead, sendMessage } from '../api/messages';
import { useAuth } from '../auth/useAuth';

/**
 * Message bubbles are aligned by comparing the sender against the signed-in user, not by trusting a
 * client-side "mine" flag: the server returns who actually sent each message.
 */
const clockTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString('en-ZA', {
    hour: '2-digit',
    minute: '2-digit',
  });
};

const dayLabel = (value) => {
  if (!value) return 'Today';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Today';
  return date.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' });
};

const Conversation = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const myId = user?.id ?? null;

  const [conversation, setConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [sendError, setSendError] = useState(null);
  const [sending, setSending] = useState(false);

  const scroller = useRef(null);
  const pinnedToBottom = useRef(true);

  const loadThread = useCallback(async () => {
    setLoadError(null);
    try {
      // The conversation is only needed for the header (who am I talking to, about what). The
      // server rejects a thread the caller is not a participant in, so a bad id lands here as an
      // error rather than as somebody else's messages.
      const [details, thread] = await Promise.all([
        getConversation(id),
        listMessages(id),
      ]);
      setConversation(details);
      setMessages(thread);
      return details;
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      setLoading(true);
      try {
        const details = await loadThread();
        if (cancelled) return;

        // Opening a thread is what marks it read. Failure here is not worth an error banner — the
        // messages are on screen and the badge will correct itself the next time the list loads.
        try {
          await markConversationRead(id);
        } catch {
          // ignored on purpose
        }
        return details;
      } catch (caught) {
        if (!cancelled) setLoadError(caught);
        return null;
      }
    })();

    return () => { cancelled = true; };
  }, [loadThread, id]);

  // Follow new messages only while the reader is already at the bottom. Yanking the view down while
  // someone is reading further up is worse than making them scroll.
  useEffect(() => {
    const node = scroller.current;
    if (node && pinnedToBottom.current) node.scrollTop = node.scrollHeight;
  }, [messages]);

  const onScroll = () => {
    const node = scroller.current;
    if (!node) return;
    pinnedToBottom.current = node.scrollHeight - node.scrollTop - node.clientHeight < 80;
  };

  const handleSend = async () => {
    const trimmed = draft.trim();
    if (!trimmed || sending) return;

    setSending(true);
    setSendError(null);
    try {
      const sent = await sendMessage(id, trimmed);
      setMessages((current) => [...current, sent]);
      setDraft('');
      pinnedToBottom.current = true;
    } catch (caught) {
      // The draft is left in the box: the text is the user's, and losing it because the request
      // failed would be the worst possible outcome of a failed send.
      setSendError(caught.message);
    } finally {
      setSending(false);
    }
  };

  const other = conversation
    ? (conversation.buyer?.id === myId ? conversation.seller : conversation.buyer)
    : null;

  return (
    <div className="min-h-dvh bg-white flex flex-col">
      <header className="px-4 py-3.5 border-b border-border">
        <div className="max-w-2xl mx-auto flex items-center gap-3">
          <BackButton />
          <Avatar size="sm" src={other?.avatarUrl || null} alt={other?.name ?? 'Conversation'} />
          <div className="min-w-0">
            <h2 className="font-semibold text-text-primary truncate">{other?.name ?? 'Conversation'}</h2>
            {conversation?.product?.name && (
              <p className="text-xs text-text-muted truncate">About {conversation.product.name}</p>
            )}
          </div>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto py-5" ref={scroller} onScroll={onScroll}>
        <div className="max-w-2xl mx-auto px-4 space-y-3">
          {loadError && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-2xl" role="alert">
              <p className="text-sm text-red-800">{loadError.message}</p>
            </div>
          )}

          {!loading && !loadError && messages.length === 0 && (
            <p className="text-center text-sm text-text-muted py-10">
              No messages yet. Say hello to get started.
            </p>
          )}

          {messages.length > 0 && (
            <div className="text-center mb-3">
              <span className="text-[11px] font-medium text-text-muted bg-lavender px-3 py-1 rounded-full">
                {dayLabel(messages[0]?.sentAt)}
              </span>
            </div>
          )}

          {messages.map((entry) => {
            const mine = entry.sender?.id === myId;
            return (
              <div key={entry.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[78%] px-4 py-2.5 rounded-2xl ${
                    mine
                      ? 'bg-primary text-white rounded-br-md shadow-sm shadow-primary/20'
                      : 'bg-lavender text-text-primary rounded-bl-md'
                  }`}
                >
                  <p className="text-sm leading-relaxed break-words">{entry.body}</p>
                  <p className={`text-[11px] mt-1 ${mine ? 'text-white/70' : 'text-text-muted'}`}>
                    {clockTime(entry.sentAt)}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <footer className="px-4 py-4 border-t border-border bg-white/95 backdrop-blur safe-area-inset-bottom">
        <div className="max-w-2xl mx-auto">
          {sendError && (
            <p className="mb-2 text-sm text-red-700" role="alert">{sendError}</p>
          )}
          <div className="flex items-center gap-3">
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
              placeholder="Type a message..."
              aria-label="Type a message"
              className="flex-1 px-4 py-3 bg-lavender rounded-full text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all duration-200"
            />
            <button
              onClick={handleSend}
              disabled={!draft.trim() || sending || loadError}
              className="w-11 h-11 bg-primary rounded-full flex items-center justify-center text-white hover:bg-primary-hover active:scale-90 transition-all duration-200 disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
              aria-label="Send message"
            >
              <Send size={18} />
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Conversation;