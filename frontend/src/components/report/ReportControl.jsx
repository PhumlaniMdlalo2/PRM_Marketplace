import { useId, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Flag } from 'lucide-react';
import Button from '../ui/Button';
import { createReport } from '../../api/reports';
import { useAuth } from '../../auth/useAuth';

/** The column is `varchar(1000)`, so the field stops at the same length the server accepts. */
const REASON_LIMIT = 1000;

/**
 * Files a report against whatever it is placed next to.
 *
 * Every reportable thing in the app already has an id on screen, so the target is a prop rather
 * than a field to fill in: the old form in Settings asked a reader to paste an id out of the URL
 * and pick a kind from a list that did not match the server's, which is a form that produces
 * rejected reports and teaches people that reporting is broken.
 *
 * A signed-out reader is sent to sign in with the page they were on kept as the return path, the
 * same way every other gated action in the app behaves.
 */
const ReportControl = ({ targetType, targetId, label = 'Report', className = '' }) => {
  const fieldId = useId();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [error, setError] = useState(null);
  const [filed, setFiled] = useState(false);
  const [busy, setBusy] = useState(false);

  const start = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }
    setError(null);
    setOpen(true);
  };

  const submit = async (event) => {
    event.preventDefault();
    const text = reason.trim();
    if (!text || busy) return;

    setBusy(true);
    setError(null);
    try {
      await createReport({ targetType, targetId, reason: text });
      setFiled(true);
      setOpen(false);
      setReason('');
    } catch (caught) {
      // The target id comes from this app, so a refusal is about the body rather than the target,
      // and an empty 400 would otherwise be shown as a connection problem.
      setError(caught?.status === 400
        ? 'That report was not accepted. Shorten the description and try again.'
        : caught?.message || 'The report could not be filed.');
    } finally {
      setBusy(false);
    }
  };

  if (filed) {
    return (
      <div className={className}>
        <p role="status" className="text-xs text-primary font-medium">Report filed.</p>
      </div>
    );
  }

  if (!open) {
    return (
      <div className={className}>
        <button
          type="button"
          onClick={start}
          className="inline-flex items-center gap-1.5 text-xs font-medium text-text-muted hover:text-error transition-colors"
        >
          <Flag size={13} />
          {label}
        </button>
      </div>
    );
  }

  return (
    <form className={className} onSubmit={submit}>
      <label htmlFor={fieldId} className="sr-only">Why are you reporting this?</label>
      <textarea
        id={fieldId}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        rows={3}
        maxLength={REASON_LIMIT}
        autoFocus
        placeholder="Describe the problem"
        className="w-full px-3 py-2 bg-lavender rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:ring-2 focus:ring-primary/30 focus:bg-white transition-all resize-y"
      />
      {error && (
        <p role="alert" className="mt-1.5 text-xs text-error">{error}</p>
      )}
      <div className="mt-2 flex items-center justify-end gap-3">
        <button
          type="button"
          onClick={() => {
            setOpen(false);
            setError(null);
          }}
          disabled={busy}
          className="text-xs font-medium text-text-muted hover:text-text-primary disabled:opacity-50"
        >
          Cancel
        </button>
        <Button type="submit" size="sm" disabled={busy || !reason.trim()}>
          {busy ? 'Filing…' : 'File report'}
        </Button>
      </div>
    </form>
  );
};

export default ReportControl;
