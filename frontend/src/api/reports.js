import api from './client';

/**
 * Problem reports. The reporter and status used to come from the request body, so a complaint could
 * be filed in another person's name and pre-marked as resolved; both are now the server's to set,
 * which is why create only sends what the reporter actually knows.
 */

export const listMyReports = async () => {
  const response = await api.get('/reports');
  return response.data;
};

/**
 * @param {object} report  targetType, targetId, reason
 * @throws {Object} the normalised client error, with `status` 400 when the report is not accepted
 */
export const createReport = async (report) => {
  const response = await api.post('/reports', report);
  return response.data;
};

/**
 * Every report, for moderation.
 *
 * The route is open to any signed-in caller and the service decides what comes back: a faculty
 * account gets the queue, and anyone else gets an empty list rather than a refusal. Reading that
 * answer as "there is nothing to do" is correct for both, which is why there is no role check on
 * this side of it.
 *
 * @returns {Promise<Array>} the reports, always an array
 */
export const listReportsForModeration = async () => {
  const response = await api.get('/reports/moderation/all');
  return Array.isArray(response.data) ? response.data : [];
};

/**
 * Puts a report into a new status, with a note for the reporter.
 *
 * `status` and `notes` travel as query parameters because that is the shape the route declares;
 * the body is deliberately empty so nothing can be smuggled past the parameter binding.
 *
 * @param {string} id  the report being decided
 * @param {object} decision  `status` (OPEN | UNDER_REVIEW | RESOLVED) and optional `notes`
 * @throws {Object} the normalised client error, with `status` 404 when the caller is not faculty
 *   or the report does not exist — the route answers 404 for both
 */
export const resolveReport = async (id, { status, notes } = {}) => {
  const params = { status };
  const trimmed = (notes ?? '').trim();
  if (trimmed) params.notes = trimmed;
  const response = await api.patch(`/reports/${id}/status`, null, { params });
  return response.data;
};

/**
 * Withdraws one of the caller's own reports. Somebody else's is reported as not found.
 */
export const withdrawReport = async (id) => {
  await api.delete(`/reports/${id}`);
};