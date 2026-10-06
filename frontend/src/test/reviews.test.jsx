import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ReviewsSection from '../components/review/ReviewsSection';
import { AuthProvider } from '../auth/AuthContext';
import { createReview, deleteReview, updateReview } from '../api/products';

/**
 * The product page could always count a seller's reviews, but it never rendered a single one and
 * there was no way for the buyer who paid to leave one. Everything that makes a review a review —
 * seeing it, writing it, changing your mind, taking it back — lives in this component.
 *
 * The cases below are the ones where the component is on its own: the server's rules have to be
 * explained rather than echoed (it answers 400 with an empty body), and the reader's own review has
 * to be distinguishable from everyone else's without a name on the API to tell them apart.
 */

vi.mock('../api/products', () => ({
  createReview: vi.fn(),
  updateReview: vi.fn(),
  deleteReview: vi.fn(),
}));

const PRODUCT_ID = 'product-1';
const ME = 'user-me';
const THEM = 'user-them';

const review = (overrides = {}) => ({
  id: 'review-1',
  productId: PRODUCT_ID,
  reviewerId: THEM,
  rating: 4,
  comment: 'Arrived as described, two days early.',
  createdAt: '2026-09-01T10:00:00',
  ...overrides,
});

const renderSection = ({
  reviews = [],
  isAuthenticated = true,
  currentUserId = ME,
  onChanged = vi.fn().mockResolvedValue(undefined),
  requireSignIn = vi.fn(),
} = {}) => {
  // AuthProvider and the router are here for the report control that sits under somebody else's
  // review: it reads the session to decide whether to file or to send the reader to sign in, and
  // it navigates when it does the latter.
  render(
    <MemoryRouter>
      <AuthProvider>
        <ReviewsSection
          productId={PRODUCT_ID}
          reviews={reviews}
          onChanged={onChanged}
          isAuthenticated={isAuthenticated}
          currentUserId={currentUserId}
          requireSignIn={requireSignIn}
        />
      </AuthProvider>
    </MemoryRouter>,
  );
  return { onChanged, requireSignIn };
};

const writeReview = (rating, comment) => {
  fireEvent.click(screen.getByRole('radio', { name: `${rating} out of 5` }));
  fireEvent.change(screen.getByLabelText('How was it?'), {
    target: { value: comment },
  });
};

beforeEach(() => {
  createReview.mockResolvedValue(review());
  updateReview.mockResolvedValue(review());
  deleteReview.mockResolvedValue(undefined);
});

describe('ReviewsSection', () => {
  it('shows the reviews that exist, without a name the API does not provide', () => {
    renderSection({ reviews: [review()] });

    expect(screen.getByText('Arrived as described, two days early.')).toBeInTheDocument();
    expect(screen.getByText('Verified buyer')).toBeInTheDocument();
    expect(screen.getByText('1 review')).toBeInTheDocument();
  });

  it('says so plainly when a listing has none', () => {
    renderSection();

    expect(screen.getByText('No reviews on this listing yet.')).toBeInTheDocument();
  });

  it('offers a way in rather than a form to a signed-out visitor', () => {
    const { requireSignIn } = renderSection({ isAuthenticated: false });

    expect(screen.getByText('Bought from this seller? Say how it went.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Sign in to review' }));

    expect(requireSignIn).toHaveBeenCalled();
    expect(screen.queryByLabelText('How was it?')).toBeNull();
  });

  it('writes a review as the signed-in reader', async () => {
    const { onChanged } = renderSection();

    writeReview(5, 'Exactly what I needed.');
    fireEvent.click(screen.getByRole('button', { name: /Post review/ }));

    await waitFor(() => {
      expect(createReview).toHaveBeenCalledWith({
        productId: PRODUCT_ID,
        rating: 5,
        comment: 'Exactly what I needed.',
      });
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('refuses an empty comment without spending a request', async () => {
    renderSection();

    fireEvent.click(screen.getByRole('radio', { name: '3 out of 5' }));
    fireEvent.click(screen.getByRole('button', { name: /Post review/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Say something about it before sending.',
    );
    expect(createReview).not.toHaveBeenCalled();
  });

  it('refuses a submission with no rating rather than sending a zero', async () => {
    renderSection();

    fireEvent.change(screen.getByLabelText('How was it?'), {
      target: { value: 'Fine.' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Post review/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Choose a star rating first.',
    );
    expect(createReview).not.toHaveBeenCalled();
  });

  it('explains the server refusal, which arrives as a bare 400', async () => {
    createReview.mockRejectedValue({
      status: 400,
      // The server sends no body for this, so the client's normaliser has nothing to show.
      message: 'Cannot reach the server. Please check your connection.',
    });
    renderSection();

    writeReview(5, 'Not a buyer.')
    fireEvent.click(screen.getByRole('button', { name: /Post review/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Reviews are for things you bought from this seller, and you may leave one per listing.',
    );
  });

  it('marks the reader\'s own review and offers to change it', () => {
    renderSection({ reviews: [review({ reviewerId: ME, rating: 2 })] });

    expect(screen.getByText('You')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Edit your review' }));

    expect(screen.getByLabelText('Your review')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: '2 out of 5' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });

  it('saves a changed rating and comment against the review\'s own id', async () => {
    const { onChanged } = renderSection({ reviews: [review({ reviewerId: ME })] });

    fireEvent.click(screen.getByRole('button', { name: 'Edit your review' }));
    fireEvent.click(screen.getByRole('radio', { name: '5 out of 5' }));
    fireEvent.change(screen.getByLabelText('Your review'), {
      target: { value: 'Changed my mind, it was excellent.' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Save changes/ }));

    await waitFor(() => {
      expect(updateReview).toHaveBeenCalledWith({
        id: 'review-1',
        rating: 5,
        comment: 'Changed my mind, it was excellent.',
      });
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('leaves the edit without saving anything', () => {
    renderSection({ reviews: [review({ reviewerId: ME })] });

    fireEvent.click(screen.getByRole('button', { name: 'Edit your review' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.getByText('You reviewed this listing already.')).toBeInTheDocument();
    expect(updateReview).not.toHaveBeenCalled();
  });

  it('asks before deleting, and only deletes on the confirmation', async () => {
    const { onChanged } = renderSection({ reviews: [review({ reviewerId: ME })] });

    fireEvent.click(screen.getByRole('button', { name: 'Delete your review' }));
    expect(screen.getByText('You reviewed this listing already.')).toBeInTheDocument();
    expect(deleteReview).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: /Keep it/ }));
    expect(deleteReview).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Delete your review' }));
    fireEvent.click(screen.getByRole('button', { name: /Delete for good/ }));

    await waitFor(() => {
      expect(deleteReview).toHaveBeenCalledWith('review-1');
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('does not offer to edit somebody else\'s review', () => {
    renderSection({ reviews: [review({ reviewerId: THEM })] });

    expect(screen.queryByRole('button', { name: 'Edit your review' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete your review' })).toBeNull();
  });

  it('drops a review that has since gone when the server says 404', async () => {
    deleteReview.mockRejectedValue({ status: 404, message: 'Not found' });
    renderSection({ reviews: [review({ reviewerId: ME })] });

    fireEvent.click(screen.getByRole('button', { name: 'Delete your review' }));
    fireEvent.click(screen.getByRole('button', { name: /Delete for good/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'That review is no longer there to change.',
    );
  });
});