import api from './client';

/**
 * Notifications are the caller's own, so every endpoint here is scoped by the token rather than by a
 * user id in the path: there is no way to ask for somebody else's.
 *
 * The backend has had these endpoints all along, along with the payment and order notifications that
 * fill them, but nothing in the app called them, which made them write-only. Anything the server
 * writes into a notification was invisible to the person it was written for.
 */

/** The caller's notifications, newest first as the server returns them. */
export const listNotifications = async () => {
  const { data } = await api.get('/notifications');
  // Guarded rather than coalesced with `??`, because an unexpected body is not necessarily null: a
  // proxy answering with an object would otherwise reach `notifications.map` and take the panel down.
  return Array.isArray(data) ? data : [];
};

/** How many the caller has not read yet, for the badge. */
export const countUnreadNotifications = async () => {
  const { data } = await api.get('/notifications/unread/count');
  const count = Number(data);
  return Number.isFinite(count) ? count : 0;
};

/**
 * Marks one as read and returns the updated copy.
 *
 * @returns {Promise<object|null>} the notification, or null when the server refused it
 */
export const markNotificationRead = async (notificationId) => {
  const { data } = await api.patch(`/notifications/${notificationId}/read`);
  return data ?? null;
};

/** Marks all of them read. Returns how many the server changed. */
export const markAllNotificationsRead = async () => {
  const { data } = await api.patch('/notifications/read-all');
  const changed = Number(data);
  return Number.isFinite(changed) ? changed : 0;
};

/**
 * Removes one.
 *
 * Treated as success on 404, because the server answers 404 both for a notification that is not
 * there and for one that is not the caller's, and the caller's question either way is whether it is
 * still in the list.
 *
 * @returns {boolean} true when it is gone afterwards
 */
export const dismissNotification = async (notificationId) => {
  try {
    await api.delete(`/notifications/${notificationId}`);
    return true;
  } catch (error) {
    if (error.status === 404) return true;
    throw error;
  }
};