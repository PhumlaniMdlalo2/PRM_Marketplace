import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Bulletin from '../pages/Bulletin';
import { getLikeCount, hasLikedPost, listPosts, toggleLike } from '../api/bulletin';

vi.mock('../api/bulletin', () => ({
  getLikeCount: vi.fn(),
  hasLikedPost: vi.fn(),
  listPosts: vi.fn(),
  toggleLike: vi.fn(),
}));

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ isAuthenticated: true }),
}));

vi.mock('../components/layout/Layout', () => ({
  default: ({ children }) => children,
}));

const posts = [
  {
    id: 'new',
    title: 'Fresh lab coat offer',
    body: 'A recent post to find quickly.',
    category: 'market',
    imageUrl: null,
    createdAt: '2026-10-06T12:00:00',
    author: { name: 'Ayesha', avatarUrl: null },
    likeCount: 2,
    commentCount: 1,
  },
  {
    id: 'popular',
    title: 'Study group for accounting',
    body: 'Looking for other first years.',
    category: 'study',
    imageUrl: null,
    createdAt: '2026-10-05T12:00:00',
    author: { name: 'Lutho', avatarUrl: null },
    likeCount: 16,
    commentCount: 8,
  },
];

const renderFeed = () => render(
  <MemoryRouter>
    <Bulletin />
  </MemoryRouter>,
);

beforeEach(() => {
  vi.clearAllMocks();
  listPosts.mockResolvedValue(posts);
  hasLikedPost.mockResolvedValue(false);
  toggleLike.mockResolvedValue(true);
  getLikeCount.mockResolvedValue(3);
});

describe('Reddit-style bulletin feed', () => {
  it('sorts recent discussions first in the New view', async () => {
    renderFeed();
    fireEvent.click(await screen.findByRole('button', { name: 'New' }));

    const articles = await screen.findAllByRole('article');
    expect(within(articles[0]).getByRole('heading', { name: 'Fresh lab coat offer' })).toBeInTheDocument();
    expect(within(articles[1]).getByRole('heading', { name: 'Study group for accounting' })).toBeInTheDocument();
  });

  it('filters discussions by search text and topic', async () => {
    renderFeed();
    await screen.findByRole('heading', { name: 'Fresh lab coat offer' });

    fireEvent.click(screen.getAllByRole('button', { name: 'study' })[0]);
    expect(screen.getByRole('heading', { name: 'Study group for accounting' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Fresh lab coat offer' })).not.toBeInTheDocument();

    fireEvent.change(screen.getByRole('searchbox', { name: 'Search discussions' }), {
      target: { value: 'does not exist' },
    });
    expect(screen.getByText('No discussions match that')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(screen.getByRole('heading', { name: 'Study group for accounting' })).toBeInTheDocument();
  });

  it('uses the existing like service as an upvote and updates the displayed score', async () => {
    renderFeed();
    const upvote = await screen.findByRole('button', { name: 'Upvote Fresh lab coat offer' });
    fireEvent.click(upvote);

    expect(toggleLike).toHaveBeenCalledWith('new');
    expect(await screen.findByRole('button', { name: 'Remove upvote from Fresh lab coat offer' }))
      .toHaveAttribute('aria-pressed', 'true');
    expect(within(upvote.closest('article')).getByText('3')).toBeInTheDocument();
  });
});
