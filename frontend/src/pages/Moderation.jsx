import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { Gavel } from 'lucide-react';
import Layout from '../components/layout/Layout';
import BackButton from '../components/ui/BackButton';
import Button from '../components/ui/Button';
import EmptyState from '../components/ui/EmptyState';
import { listReportsForModeration, resolveReport } from '../api/reports';
import { useAuth } from '../auth/useAuth';
import { useAsync } from '../hooks/useAsync';

/**
 * The moderation queue.
 *
 * The backend has always had a faculty-only view of every report filed against the marketplace and
 * a route to decide on one, and nothing in the app called either: complaints arrived, sat in the
 * table, and the only people who could act on them had no screen to act from.
 *
 * The role check below is a courtesy rather than a control — the service answers a non-faculty
 * caller with an empty list, and every route here is protected server-side too. Its purpose is that
 * a student who follows the link is told why the page is empty instead of being shown a queue that
 * mysteriously has nothing in it.
 */
const TARGET_LABELS = {
  PRODUCT: 'Listing',
  USER: 'Account',
  BULLETIN_POST: 'Bulletin post',
  COMMENT: 'Comment',
  REVIEW: 'Review',
  MESSAGE: 'Message',
};

const STATUS_LABELS = {
  OPEN: 'Open',
  UNDER_REVIEW: 'Under review',
  RESOLVED: 'Resolved',
};

const STATUS_TONES = {
  OPEN: 'bg-warning/10 text-warning',
  UNDER_REVIEW: 'bg-primary-muted text-primary',
  RESOLVED: 'bg-lavender text-text-muted',
};

const FILTERS = [
  { label: 'All', value: null },
  { label: 'Open', value: 'OPEN' },
  { label: 'Under review', value: 'UNDER_REVIEW' },
  { label: 'Resolved', value: 'RESOLVED' },
];

/** Where a report points, when it points at something with a page in this app. */
const targetPath = (report) => {
  if (report.targetType === 'PRODUCT') return `/product/${report.targetId}`;
  if (report.targetType === 'BULLETIN_POST') return `/bulletin/${report.targetId}`;
  return null;
};

const ReportRow = ({ report, onChanged }) => {
  const [notes, setNotes] = useState(report.resolutionNotes ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const apply = async (status) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await resolveReport(report.id, { status, notes });
      onChanged(updated);
    } catch (caught) {
      setError(caught?.status === 404
        ? 'That report is no longer in the queue.'
        : caught?.message || 'The decision could not be saved.');
    } finally {
      setBusy(false);
    }
  };

  const path = targetPath(report);
  const reporterName = report.reporter?.name ?? 'Someone';

  return (
    <li className="p-4">
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className={`text-[11px] font-medium px-2 py-0.5 rounded-md ${STATUS_TONES[report.status] ?? STATUS_TONES.OPEN}`}>
              {STATUS_LABELS[report.status] ?? report.status}
            </span>
            <span className="text-sm font-semibold text-text-primary">
              {TARGET_LABELS[report.targetType] ?? report.targetType}
            </span>
            {path && (
              <Link to={path} className="text-xs font-medium text-primary hover:underline">
                Open
              </Link>
            )}
          </div>
          <p className="mt-1.5 text-sm text-text-secondary break-words">{report.reason}</p>
          <p className="mt-1 text-xs text-text-muted">
            {reporterName}
            {report.createdAt && (
              <>
                {' · '}
                {new Date(report.createdAt).toLocaleString('en-ZA', {
                  day: 'numeric',
                  month: 'short',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </>
            )}
          </p>
        </div>
      </div>

      <div className="mt-3 flex flex-wrap items-end gap-3">
        <div className="flex-1 min-w-[220px]">
          <label htmlFor={`notes-${report.id}`} className="block text-xs font-medium text-text-muted mb-1">
            Notes for the reporter
          </label>
          <textarea
            id={`notes-${report.id}`}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            maxLength={1000}
            placeholder="Optional"
            className="w-full px-3 py-2 bg-lavender rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all resize-y"
          />
        </div>
        <div className="flex items-center gap-2">
          {report.status !== 'UNDER_REVIEW' && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => apply('UNDER_REVIEW')}
              disabled={busy}
            >
              {busy ? 'Saving…' : 'Mark under review'}
            </Button>
          )}
          {report.status !== 'RESOLVED' && (
            <Button
              size="sm"
              onClick={() => apply('RESOLVED')}
              disabled={busy}
            >
              {busy ? 'Saving…' : 'Resolve'}
            </Button>
          )}
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-2 text-xs text-error">{error}</p>
      )}
    </li>
  );
};

const Moderation = () => {
  const { user } = useAuth();
  const isFaculty = user?.role === 'FACULTY';

  const load = useCallback(async () => {
    if (!isFaculty) return [];
    return listReportsForModeration();
  }, [isFaculty]);

  const { data: reports, loading, error, setData } = useAsync(load);
  const [filter, setFilter] = useState(null);

  const list = reports ?? [];
  const shown = filter ? list.filter((report) => report.status === filter) : list;

  const applyUpdate = useCallback((updated) => {
    setData((current) => (Array.isArray(current)
      ? current.map((report) => (report.id === updated.id ? updated : report))
      : current));
  }, [setData]);

  return (
    <Layout showNav={false}>
      <div className="app-container py-6 pb-24 max-w-3xl mx-auto">
        <div className="flex items-center gap-4 mb-6">
          <BackButton />
          <h1 className="text-xl font-bold text-text-primary tracking-tight">Moderation</h1>
        </div>

        {!isFaculty && (
          <EmptyState
            icon={Gavel}
            title="Moderation is for faculty accounts"
            description="Reports are handled by the faculty account that moderates the marketplace."
            action={
              <Link to="/" className="text-sm font-medium text-primary">Back to the marketplace</Link>
            }
          />
        )}

        {isFaculty && error && (
          <div className="p-4 bg-red-50 border border-red-200 rounded-2xl mb-4" role="alert">
            <p className="text-sm text-red-800">The queue could not be loaded. Please try again.</p>
          </div>
        )}

        {isFaculty && (
          <>
            <div className="flex gap-2 overflow-x-auto pb-4" aria-label="Filter reports">
              {FILTERS.map((option) => (
                <button
                  key={option.label}
                  type="button"
                  aria-pressed={filter === option.value}
                  onClick={() => setFilter(option.value)}
                  className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all duration-200 active:scale-95 ${
                    filter === option.value
                      ? 'bg-primary text-white shadow-md shadow-primary/20'
                      : 'bg-white border border-border text-text-primary hover:bg-lavender'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>

            {loading && list.length === 0 && (
              <div className="bg-white border border-border rounded-2xl divide-y divide-border overflow-hidden">
                <div className="h-20 bg-lavender/70 animate-pulse rounded-none" />
                <div className="h-20 bg-lavender/70 animate-pulse rounded-none" />
              </div>
            )}

            {/* An empty queue and a queue that could not be read are different facts, and saying
                both at once would invite a moderator to believe the calmer of the two. */}
            {!loading && !error && list.length === 0 && (
              <EmptyState
                icon={Gavel}
                title="Nothing to moderate"
                description="No reports have been filed against the marketplace."
              />
            )}

            {!error && list.length > 0 && shown.length === 0 && (
              <EmptyState
                icon={Gavel}
                title="No reports with that status"
                description="Pick another filter to see the rest of the queue."
              />
            )}

            {shown.length > 0 && (
              <ul className="bg-white border border-border rounded-2xl divide-y divide-border overflow-hidden">
                {shown.map((report) => (
                  <ReportRow key={report.id} report={report} onChanged={applyUpdate} />
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </Layout>
  );
};

export default Moderation;
