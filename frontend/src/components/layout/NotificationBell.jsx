import { useCallback, useEffect, useRef, useState } from 'react';
import { Bell, CheckCheck, Package, ShoppingBag, Star, Trash2, X } from 'lucide-react';
import {
  countUnreadNotifications,
  dismissNotification,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api/notifications';
import { useAuth } from '../../auth/useAuth';

/**
 * The bell, and the list it opens.
 *
 * This is the only place in the app where a notification can be read. The backend has written them
 * for payments and orders since long before this existed, so until now there was no way for anybody
 * to see one: this panel is the missing half of a feature that only worked in one direction.
 *
 * The unread count is fetched on its own, on a timer, rather than derived from the list, because the
 * list is only fetched when the panel opens. Someone who never opens the panel still has to learn
 * that they have a notification, which is the whole purpose of a badge.
 */

// One request a minute. Long enough that it is not a poll, short enough that a notification sent
// from another tab shows up without a reload.
const REFRESH_INTERVAL_MS = 60_000;

const EMPTY = [];

// The server's enum, mapped to an icon. A type with no entry here falls back to the bell rather
// than rendering nothing, so a new notification type stays visible before someone has drawn art for
// it.
const TYPE_ICONS = {
  ORDER: Package,
  PAYMENT: ShoppingBag,
  REVIEW: Star,
};

const formatWhen = (value) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('en-ZA');
};

/** A relative time while it is short, an exact one once it is not. */
const describeAge = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const minutes = Math.round((Date.now() - date.getTime()) / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString('en-ZA');
};

/**
 * The list itself, split out so both placements render the same one.
 *
 * It is deliberately controlled: every row change is decided in the bell, which owns the unread
 * count and the undo of its own optimistic updates, so this panel holds no state of its own.
 */
const NotificationPanel = ({
  notifications,
  loading,
  failed,
  unread,
  onClose,
  onMarkRead,
  onMarkAllRead,
  onDismiss,
}) => (
  <section
    aria-label="Notifications"
    className="bg-white border border-border rounded-2xl shadow-lg overflow-hidden flex flex-col max-h-[70vh]"
  >
    <header className="flex items-center justify-between px-4 py-3 border-b border-border shrink-0">
      <h2 className="font-semibold text-text-primary">Notifications</h2>
      <div className="flex items-center gap-1">
        {unread > 0 && (
          <button
            type="button"
            onClick={onMarkAllRead}
            className="p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-lavender/60"
            aria-label="Mark all notifications as read"
            title="Mark all as read"
          >
            <CheckCheck size={16} />
          </button>
        )}
        <button
          type="button"
          onClick={onClose}
          className="p-1.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-lavender/60"
          aria-label="Close notifications"
        >
          <X size={16} />
        </button>
      </div>
    </header>

    <div className="overflow-y-auto">
      {loading && (
        <p className="px-4 py-6 text-sm text-text-muted text-center">Loading…</p>
      )}

      {!loading && failed && (
        <p className="px-4 py-6 text-sm text-error text-center" role="alert">
          Notifications could not be loaded.
        </p>
      )}

      {!loading && !failed && notifications.length === 0 && (
        <p className="px-4 py-6 text-sm text-text-muted text-center">
          Nothing here yet. Payments and orders you are involved in will show up.
        </p>
      )}

      {!loading &&
        notifications.map((notification) => {
          const TypeIcon = TYPE_ICONS[notification.type] ?? Bell;
          return (
            <div
              key={notification.id}
              className={`flex gap-3 px-4 py-3 border-b border-border last:border-0 ${
                notification.read ? 'bg-white' : 'bg-primary-muted/40'
              }`}
            >
              <button
                type="button"
                onClick={() => onMarkRead(notification)}
                className="flex gap-3 flex-1 text-left min-w-0"
                aria-label={`Mark "${notification.title}" as read`}
              >
                <span className="w-8 h-8 rounded-full bg-lavender flex items-center justify-center shrink-0">
                  <TypeIcon size={15} className="text-primary" />
                </span>
                <span className="min-w-0">
                  <span className="flex items-baseline gap-2">
                    <span className="font-medium text-text-primary text-sm truncate">
                      {notification.title}
                    </span>
                    <span className="text-[11px] text-text-muted shrink-0">
                      {describeAge(notification.createdAt)}
                    </span>
                  </span>
                  <span className="block text-sm text-text-secondary">
                    {notification.message}
                  </span>
                  {notification.read && notification.readAt && (
                    <span className="block text-[11px] text-text-muted mt-0.5">
                      Read {formatWhen(notification.readAt)}
                    </span>
                  )}
                </span>
              </button>
              <button
                type="button"
                onClick={() => onDismiss(notification)}
                className="p-1.5 rounded-lg text-text-muted hover:text-error hover:bg-error/10 self-start"
                aria-label={`Dismiss "${notification.title}"`}
              >
                <Trash2 size={15} />
              </button>
            </div>
          );
        })}
    </div>
  </section>
);

const NotificationBell = ({ variant = 'icon' }) => {
  const { isAuthenticated } = useAuth();
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState(EMPTY);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const wrapper = useRef(null);

  // Signing out has to clear the badge and the list, but doing that inside an effect would start a
  // render from a render. Adjusting during render, guarded by the change itself, clears them in the
  // same pass and leaves the effect to do only the work that genuinely needs an effect: a request.
  const [wasAuthenticated, setWasAuthenticated] = useState(isAuthenticated);
  if (wasAuthenticated !== isAuthenticated) {
    setWasAuthenticated(isAuthenticated);
    setUnread(0);
    setNotifications(EMPTY);
    setOpen(false);
  }

  // The badge only matters for somebody signed in. Asking anyway would mean a 401 on the login
  // page, and the interceptor would clear a session that was never there.
  // Reads the count as a promise rather than writing state itself, so the effect that drives it
  // hands off instead of writing. The rejection is swallowed for the same reason as below: a badge
  // that cannot be counted is not worth an error on every page in the app.
  const refreshCount = useCallback(
    () => (isAuthenticated
      ? countUnreadNotifications().then(
          (value) => {
            setUnread(value);
            return value;
          },
          () => null,
        )
      : Promise.resolve(null)),
    [isAuthenticated],
  );

  useEffect(() => {
    if (!isAuthenticated) return undefined;

    refreshCount();
    const timer = setInterval(() => {
      refreshCount();
    }, REFRESH_INTERVAL_MS);
    return () => clearInterval(timer);
  }, [isAuthenticated, refreshCount]);

  // Clicking away closes the panel, otherwise it stays over the page it is covering.
  useEffect(() => {
    if (!open) return undefined;

    const onPointerDown = (event) => {
      if (wrapper.current && !wrapper.current.contains(event.target)) setOpen(false);
    };
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setOpen(false);
    };

    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const togglePanel = async () => {
    const next = !open;
    setOpen(next);
    if (!next) return;

    setLoading(true);
    setFailed(false);
    try {
      setNotifications(await listNotifications());
    } catch {
      setNotifications(EMPTY);
      setFailed(true);
    } finally {
      setLoading(false);
    }
  };

  // Each row change is optimistic, because a badge and a row that only move after a round trip feel
  // broken on a tap. Every one of them puts itself back exactly as it was if the server refuses.
  const markRead = async (notification) => {
    if (notification.read) return;
    setNotifications((current) =>
      current.map((item) => (item.id === notification.id ? { ...item, read: true } : item)));
    setUnread((count) => Math.max(0, count - 1));
    try {
      await markNotificationRead(notification.id);
    } catch {
      setNotifications((current) =>
        current.map((item) =>
          item.id === notification.id ? { ...item, read: false } : item));
      setUnread((count) => count + 1);
    }
  };

  const markAllRead = async () => {
    const previous = notifications;
    setNotifications((current) => current.map((item) => ({ ...item, read: true })));
    setUnread(0);
    try {
      await markAllNotificationsRead();
    } catch {
      setNotifications(previous);
      setUnread(previous.filter((item) => !item.read).length);
    }
  };

  const dismiss = async (notification) => {
    const previous = notifications;
    setNotifications((current) => current.filter((item) => item.id !== notification.id));
    if (!notification.read) setUnread((count) => Math.max(0, count - 1));
    try {
      await dismissNotification(notification.id);
    } catch {
      setNotifications(previous);
      setFailed(true);
    }
  };

  if (!isAuthenticated) return null;

  const label = unread > 0 ? `Notifications, ${unread} unread` : 'Notifications';
  const badge = unread > 0 ? (
    <span
      className={`absolute min-w-[16px] h-4 px-1 rounded-full bg-error text-white text-[10px] font-bold flex items-center justify-center ${
        variant === 'tab' ? '-top-0.5 -right-1' : 'top-0.5 right-0.5'
      }`}
    >
      {unread > 9 ? '9+' : unread}
    </span>
  ) : null;

  const panel = (
    <NotificationPanel
      notifications={notifications}
      loading={loading}
      failed={failed}
      unread={unread}
      onClose={() => setOpen(false)}
      onMarkRead={markRead}
      onMarkAllRead={markAllRead}
      onDismiss={dismiss}
    />
  );

  if (variant === 'tab') {
    return (
      <div ref={wrapper} className="relative">
        <button
          type="button"
          onClick={togglePanel}
          aria-expanded={open}
          aria-label={label}
          className="relative flex flex-col items-center gap-1 px-2 py-1.5 rounded-xl text-text-muted hover:text-text-primary transition-all duration-200 active:scale-95"
        >
          <span className="relative p-1.5 rounded-xl">
            <Bell size={20} />
            {badge}
          </span>
          <span className="text-[11px] font-medium">Alerts</span>
        </button>
        {open && (
          <div className="fixed inset-x-0 bottom-[68px] max-w-[480px] mx-auto px-2 z-50">
            {panel}
          </div>
        )}
      </div>
    );
  }

  return (
    <div ref={wrapper} className="relative">
      <button
        type="button"
        onClick={togglePanel}
        aria-expanded={open}
        aria-label={label}
        className="relative p-2 rounded-xl text-text-secondary hover:text-text-primary hover:bg-lavender/60 transition-all duration-200"
      >
        <Bell size={20} />
        {badge}
      </button>
      {open && <div className="absolute right-0 mt-2 w-80 sm:w-96 z-50">{panel}</div>}
    </div>
  );
};

export default NotificationBell;