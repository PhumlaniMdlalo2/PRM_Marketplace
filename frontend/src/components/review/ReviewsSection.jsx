import { useState } from 'react';
import { Pencil, Send, Star, Trash2, X } from 'lucide-react';
import ReportControl from '../report/ReportControl';
import Button from '../ui/Button';
import { createReview, deleteReview, updateReview } from '../../api/products';

/**
 * The review list, and the forms that write to it.
 *
 * The listing page already fetched these reviews to count them, but nothing ever rendered them: a
 * seller's average was on screen with no way to read a single word behind it, and no way for the
 * buyer who paid to say anything about what arrived. Both are here because they are one feature —
 * a rating you can see but not add is a number nobody can check.
 *
 * Two rules are worth spelling out, because the caller will meet both:
 *
 * - Only a buyer may review, once. The server enforces that, and answers 400 with no body at all,
 *   so there is no message to show. A 400 is therefore translated here rather than forwarded.
 * - Nothing but the rating and the comment may change. The reviewer and the product are fixed when
 *   the review is written, which is why editing sends the id in the body rather than a path.
 */

const MAX_COMMENT_LENGTH = 255;
const RATING_MIN = 1;
const RATING_MAX = 5;

const EMPTY_FORM = { rating: 0, comment: '' };

/** Turns the server's refusal into something a person can act on. */
const describeFailure = (caught) => {
  if (caught?.status === 400) {
    return 'Reviews are for things you bought from this seller, and you may leave one per listing.';
  }
  if (caught?.status === 404) {
    return 'That review is no longer there to change.';
  }
  return caught?.message ?? 'Something went wrong. Please try again.';
};

/** The star row, used both to pick a rating and to show one. */
const Stars = ({ value, onChange, labelText = 'Rating' }) => (
  <span
    className="inline-flex items-center gap-1"
    role={onChange ? 'radiogroup' : undefined}
    aria-label={onChange ? labelText : undefined}
  >
    {Array.from({ length: RATING_MAX }, (_, index) => index + 1).map((star) => {
      const filled = star <= value;
      if (!onChange) {
        return (
          <Star
            key={star}
            size={15}
            className={filled ? 'fill-warning text-warning' : 'text-lavender-dark'}
            aria-hidden="true"
          />
        );
      }
      return (
        <button
          key={star}
          type="button"
          role="radio"
          aria-checked={star === value}
          aria-label={`${star} out of 5`}
          onClick={() => onChange(star)}
          className="p-0.5 rounded transition-transform hover:scale-110 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <Star
            size={22}
            className={filled ? 'fill-warning text-warning' : 'text-lavender-dark'}
          />
        </button>
      );
    })}
  </span>
);

const ReviewCard = ({ review, isMine, onEdit, onDelete }) => (
  <li className="py-4 border-b border-border last:border-0">
    <div className="flex items-start justify-between gap-3">
      <div className="min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <Stars value={review.rating} />
          <span className="text-sm font-medium text-text-primary">
            {isMine ? 'You' : 'Verified buyer'}
          </span>
          {review.createdAt && (
            <span className="text-xs text-text-muted">
              {new Date(review.createdAt).toLocaleDateString('en-ZA')}
            </span>
          )}
        </div>
        <p className="mt-1.5 text-sm text-text-secondary leading-relaxed break-words">
          {review.comment}
        </p>
        {/* Your own review is yours to edit or withdraw, so the only complaint left to file about
            it would be against yourself. */}
        {!isMine && (
          <ReportControl
            targetType="REVIEW"
            targetId={review.id}
            label="Report this review"
            className="mt-2"
          />
        )}
      </div>

      {/* No name is shown for anyone but the reader: the API carries the reviewer's id and
          nothing else, and inventing a display name from an id would be worse than the honest
          "Verified buyer", which is exactly what the server can promise. */}
      {isMine && (
        <div className="flex items-center gap-1 shrink-0">
          <button
            type="button"
            onClick={onEdit}
            className="p-1.5 rounded-lg text-text-muted hover:text-primary hover:bg-primary-muted transition-colors"
            aria-label="Edit your review"
          >
            <Pencil size={15} />
          </button>
          <button
            type="button"
            onClick={onDelete}
            className="p-1.5 rounded-lg text-text-muted hover:text-error hover:bg-error/10 transition-colors"
            aria-label="Delete your review"
          >
            <Trash2 size={15} />
          </button>
        </div>
      )}
    </div>
  </li>
);

const ReviewsSection = ({
  productId,
  reviews,
  onChanged,
  isAuthenticated,
  currentUserId,
  requireSignIn,
}) => {
  // Guarded rather than assumed: this list is handed over from a page load, and an unexpected body
  // must not be able to turn "no reviews yet" into an exception.
  const list = Array.isArray(reviews) ? reviews : [];
  const mine = list.find((review) => review.reviewerId === currentUserId) ?? null;

  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const isEditing = editingId !== null;
  const trimmedComment = form.comment.trim();

  const startEditing = () => {
    if (!mine) return;
    setEditingId(mine.id);
    setForm({ rating: mine.rating, comment: mine.comment ?? '' });
    setError(null);
    setConfirmingDelete(false);
  };

  const cancelEditing = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError(null);
  };

  const submit = async (event) => {
    event.preventDefault();

    // Checked here as well as on the server so the usual reasons are refused without a round
    // trip, and with a message rather than a red border afterwards.
    if (form.rating < RATING_MIN || form.rating > RATING_MAX) {
      setError('Choose a star rating first.');
      return;
    }
    if (trimmedComment.length === 0) {
      setError('Say something about it before sending.');
      return;
    }
    if (trimmedComment.length > MAX_COMMENT_LENGTH) {
      setError(`Keep it to ${MAX_COMMENT_LENGTH} characters or fewer.`);
      return;
    }

    setBusy(true);
    setError(null);
    try {
      if (isEditing) {
        await updateReview({ id: editingId, rating: form.rating, comment: trimmedComment });
        cancelEditing();
      } else {
        await createReview({
          productId,
          rating: form.rating,
          comment: trimmedComment,
        });
        setForm(EMPTY_FORM);
      }
      await onChanged();
    } catch (caught) {
      setError(describeFailure(caught));
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    if (!mine || busy) return;
    setBusy(true);
    setError(null);
    try {
      await deleteReview(mine.id);
      setConfirmingDelete(false);
      await onChanged();
    } catch (caught) {
      setError(describeFailure(caught));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="mt-8" aria-labelledby="reviews-heading">
      <div className="flex items-baseline justify-between gap-3">
        <h3 id="reviews-heading" className="text-base font-semibold text-text-primary">
          Reviews
        </h3>
        <span className="text-sm text-text-secondary">
          {list.length} {list.length === 1 ? 'review' : 'reviews'}
        </span>
      </div>

      {!isAuthenticated && (
        <div className="mt-3 p-4 rounded-2xl bg-lavender flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-text-secondary">
            Bought from this seller? Say how it went.
          </p>
          <Button variant="soft" size="sm" onClick={requireSignIn}>
            Sign in to review
          </Button>
        </div>
      )}

      {/* One place for every failure this section can produce — validation, the server's refusal to
          accept the review, and a delete that did not go through — because they all have the same
          answer for the reader: change something and try again. */}
      {error && (
        <p role="alert" className="mt-3 text-sm text-error">
          {error}
        </p>
      )}

      {isAuthenticated && !mine && (
        <form onSubmit={submit} className="mt-3 p-4 rounded-2xl bg-lavender/60 border border-border">
          <label htmlFor="review-comment" className="block text-sm font-medium text-text-primary">
            How was it?
          </label>

          <div className="mt-2">
            <Stars
              value={form.rating}
              onChange={(star) => setForm((current) => ({ ...current, rating: star }))}
              labelText="Choose a rating from 1 to 5"
            />
          </div>

          <textarea
            id="review-comment"
            value={form.comment}
            onChange={(event) =>
              setForm((current) => ({ ...current, comment: event.target.value }))
            }
            maxLength={MAX_COMMENT_LENGTH}
            rows={3}
            placeholder="What arrived, and how did it hold up?"
            className="mt-3 w-full px-4 py-3 bg-white border border-border rounded-xl text-sm text-text-primary placeholder-text-muted focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/10"
          />
          <div className="mt-1 flex items-center justify-between gap-3">
            <p className="text-xs text-text-muted">
              One review per listing, and only if you bought it.
            </p>
            <span className="text-xs text-text-muted">
              {trimmedComment.length}/{MAX_COMMENT_LENGTH}
            </span>
          </div>

          <div className="mt-3 flex justify-end">
            <Button type="submit" size="sm" disabled={busy}>
              <Send size={16} />
              {busy ? 'Sending…' : 'Post review'}
            </Button>
          </div>
        </form>
      )}

      {isAuthenticated && mine && !isEditing && (
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3 p-4 rounded-2xl bg-lavender/60 border border-border">
          <p className="text-sm text-text-secondary">
            You reviewed this listing already.
          </p>
          <div className="flex items-center gap-2">
            {confirmingDelete ? (
              <>
                <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(false)}>
                  Keep it
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={remove}
                  disabled={busy}
                >
                  {busy ? 'Deleting…' : 'Delete for good'}
                </Button>
              </>
            ) : (
              <>
                <Button variant="soft" size="sm" onClick={startEditing}>
                  <Pencil size={15} />
                  Edit
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setConfirmingDelete(true)}>
                  <Trash2 size={15} />
                  Delete
                </Button>
              </>
            )}
          </div>
        </div>
      )}

      {isAuthenticated && mine && isEditing && (
        <form onSubmit={submit} className="mt-3 p-4 rounded-2xl bg-lavender/60 border border-border">
          <div className="flex items-center justify-between">
            <label
              htmlFor="review-comment"
              className="text-sm font-medium text-text-primary"
            >
              Your review
            </label>
            <button
              type="button"
              onClick={cancelEditing}
              className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-lavender"
              aria-label="Stop editing your review"
            >
              <X size={16} />
            </button>
          </div>

          <div className="mt-2">
            <Stars
              value={form.rating}
              onChange={(star) => setForm((current) => ({ ...current, rating: star }))}
              labelText="Change your rating from 1 to 5"
            />
          </div>

          <textarea
            id="review-comment"
            value={form.comment}
            onChange={(event) =>
              setForm((current) => ({ ...current, comment: event.target.value }))
            }
            maxLength={MAX_COMMENT_LENGTH}
            rows={3}
            className="mt-3 w-full px-4 py-3 bg-white border border-border rounded-xl text-sm text-text-primary focus:outline-none focus:border-primary focus:ring-4 focus:ring-primary/10"
          />

          <div className="mt-3 flex justify-end gap-2">
            <Button type="button" variant="ghost" size="sm" onClick={cancelEditing}>
              Cancel
            </Button>
            <Button type="submit" size="sm" disabled={busy}>
              {busy ? 'Saving…' : 'Save changes'}
            </Button>
          </div>
        </form>
      )}

      {list.length === 0 ? (
        <p className="mt-4 text-sm text-text-muted">No reviews on this listing yet.</p>
      ) : (
        <ul className="mt-1 divide-y divide-border">
          {list.map((review) => (
            <ReviewCard
              key={review.id}
              review={review}
              isMine={review.reviewerId === currentUserId}
              onEdit={startEditing}
              onDelete={() => setConfirmingDelete(true)}
            />
          ))}
        </ul>
      )}
    </section>
  );
};

export default ReviewsSection;