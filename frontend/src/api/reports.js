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