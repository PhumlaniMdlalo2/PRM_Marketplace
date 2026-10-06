import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PostComments from '../pages/PostComments';
import { AuthProvider } from '../auth/AuthContext';
import { TOKEN_STORAGE_KEY } from '../api/client';
import {
  createComment,
  deleteComment,
  deletePost,
  getLikeCount,
  getPost,
  hasLikedPost,
  listComments,
  updateComment,
  updatePost,
} from '../api/bulletin';

/**
 * The bulletin always let a reader write, and never let them take any of it back: a typo could not
 * be fixed, a reply could not be pulled, a mistaken post could not be removed, and the only place
 * any of those controls exist in the API was unreachable from the interface.
 *
 * What these cases pin down is the boundary the server enforces — an edit or a delete only lands for
 * the person who wrote the thing — and the two ways a delete can end: confirmed, or already gone by
 * the time the request arrives. Every failure comes back with an empty body, so the message shown
 * has to be written here rather than read off the response.
 */

vi.mock('../api/bulletin', () => ({
  createComment: vi.fn(),
  deleteComment: vi.fn(),
  deletePost: vi.fn(),
  getLikeCount: vi.fn(),
  getPost: vi.fn(),
  hasLikedPost: vi.fn(),
  listComments: vi.fn(),
  toggleLike: vi.fn(),
  updateComment: vi.fn(),
  updatePost: vi.fn(),
}));

const ME = 'user-1';
const THEM = 'user-2';

const post = (overrides = {}) => ({
  id: 'post-1',
  title: 'Selling my lab coat',
  body: 'Barely worn, size M, collection in C-town.',
  category: 'market',
  imageUrl: 'https://example.test/coat.jpg',
  commentCount: 2,
  likeCount: 3,
  createdAt: '2026-09-01T10:00:00',
  author: { id: ME, name: 'Me', avatarUrl: null },
  ...overrides,
});

const comment = (overrides = {}) => ({
  id: 'c-1',
  parentId: null,
  body: 'Is it still available?',
  createdAt: '2026-09-02T09:00:00',
  author: { id: THEM, name: 'Them', avatarUrl: null },
  ...overrides,
});

const signIn = () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value');
  localStorage.setItem('prm.user', JSON.stringify({ id: ME, name: 'Me' }));
};

const renderThread = (overrides = {}) => {
  const views = {
    post: post(),
    comments: [
      comment(),
      comment({ id: 'c-2', body: 'Yes, come by after four.', author: { id: ME, name: 'Me' } }),
    ],
    ...overrides,
  };
  getPost.mockResolvedValue(views.post);
  listComments.mockResolvedValue(views.comments);

  return render(
    <MemoryRouter initialEntries={['/bulletin/post-1']}>
      <AuthProvider>
        <Routes>
          <Route path="/bulletin/:id" element={<PostComments />} />
          <Route path="/bulletin" element={<p>Bulletin board</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
};

/** The thread is refetched after every mutation, so the second read is where a change shows up. */
const refetchWith = (comments) => {
  listComments.mockResolvedValue(comments);
};

beforeEach(() => {
  signIn();
  getPost.mockResolvedValue(post());
  listComments.mockResolvedValue([
    comment(),
    comment({ id: 'c-2', body: 'Yes, come by after four.', author: { id: ME, name: 'Me' } }),
  ]);
  getLikeCount.mockResolvedValue(3);
  hasLikedPost.mockResolvedValue(false);
  createComment.mockResolvedValue(comment());
  updateComment.mockResolvedValue(comment());
  deleteComment.mockResolvedValue(undefined);
  updatePost.mockResolvedValue(post());
  deletePost.mockResolvedValue(undefined);
});

describe('PostComments', () => {
  it('renders the thread for a signed-in reader', async () => {
    renderThread();

    expect(await screen.findByText('Selling my lab coat')).toBeInTheDocument();
    expect(screen.getByText('Is it still available?')).toBeInTheDocument();
    expect(screen.getByText('Yes, come by after four.')).toBeInTheDocument();
  });

  it('offers the controls only on what this reader wrote', async () => {
    renderThread();

    await screen.findByText('Selling my lab coat');
    expect(screen.getAllByRole('button', { name: 'Edit your comment' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Delete your comment' })).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Edit your post' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete your post' })).toBeInTheDocument();
  });

  it('hides every control on someone else\'s post', async () => {
    renderThread({ post: post({ author: { id: THEM, name: 'Them' } }) });

    await screen.findByText('Selling my lab coat');
    expect(screen.queryByRole('button', { name: 'Edit your post' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete your post' })).toBeNull();
  });

  it('saves an edited comment and refetches the thread', async () => {
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit your comment' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Edit your comment' }), {
      target: { value: 'Yes, come by after four, or tomorrow.' },
    });
    // What the thread holds once the save lands, so the assertion below can only pass through the
    // refetch that follows it.
    refetchWith([
      comment(),
      comment({
        id: 'c-2',
        body: 'Yes, come by after four, or tomorrow.',
        author: { id: ME, name: 'Me' },
      }),
    ]);
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(updateComment).toHaveBeenCalledWith(
        'c-2',
        'Yes, come by after four, or tomorrow.',
      );
    });
    expect(await screen.findByText('Yes, come by after four, or tomorrow.')).toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Edit your comment' })).toBeNull();
  });

  it('will not send an empty comment', async () => {
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit your comment' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Edit your comment' }), {
      target: { value: '   ' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(updateComment).not.toHaveBeenCalled();
    expect(screen.getByRole('textbox', { name: 'Edit your comment' })).toBeInTheDocument();
  });

  it('explains a rejected edit rather than blaming the connection', async () => {
    updateComment.mockRejectedValue({ status: 400, message: '' });
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit your comment' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Edit your comment' }), {
      target: { value: 'Updated anyway.' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'That comment could not be saved.',
    );
  });

  it('says so when the comment is no longer the reader\'s to change', async () => {
    updateComment.mockRejectedValue({ status: 404, message: '' });
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit your comment' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Edit your comment' }), {
      target: { value: 'Updated anyway.' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'That comment is no longer yours to change.',
    );
  });

  it('asks before deleting a comment, and deletes only on the confirmation', async () => {
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Delete your comment' }));
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Delete this comment? Replies go with it.',
    );
    expect(deleteComment).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Keep it' }));
    expect(deleteComment).not.toHaveBeenCalled();
    expect(screen.queryByRole('alert')).toBeNull();

    // The other comment is what is left once the refetch lands.
    refetchWith([comment()]);
    fireEvent.click(screen.getByRole('button', { name: 'Delete your comment' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete for good' }));

    await waitFor(() => expect(deleteComment).toHaveBeenCalledWith('c-2'));
    await waitFor(() => expect(screen.queryByText('Yes, come by after four.')).toBeNull());
    expect(screen.getByText('Is it still available?')).toBeInTheDocument();
  });

  it('takes a comment that is already gone off the screen without arguing', async () => {
    deleteComment.mockRejectedValue({ status: 404, message: '' });
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Delete your comment' }));
    // The server has already dropped it, so its own answer is what the refetch shows.
    refetchWith([comment()]);
    fireEvent.click(screen.getByRole('button', { name: 'Delete for good' }));

    await waitFor(() => expect(deleteComment).toHaveBeenCalledWith('c-2'));
    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
    expect(screen.queryByText('Yes, come by after four.')).toBeNull();
  });

  it('saves a post edit with the fields that did not change', async () => {
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Edit your post' }));
    fireEvent.change(screen.getByLabelText('Post content'), {
      target: { value: 'Barely worn, size M, open to offers.' },
    });
    // Applied after the first read, so the assertion below can only pass through a refetch.
    getPost.mockResolvedValue(post({ body: 'Barely worn, size M, open to offers.' }));
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(updatePost).toHaveBeenCalledWith('post-1', {
        title: 'Selling my lab coat',
        body: 'Barely worn, size M, open to offers.',
        category: 'market',
        imageUrl: 'https://example.test/coat.jpg',
      });
    });
    expect(await screen.findByText('Barely worn, size M, open to offers.')).toBeInTheDocument();
  });

  it('deletes a post only after confirmation, and leaves the thread', async () => {
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Delete your post' }));
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Delete this post and every comment on it?',
    );
    expect(deletePost).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Keep it' }));
    expect(deletePost).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Delete your post' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete for good' }));

    await waitFor(() => expect(deletePost).toHaveBeenCalledWith('post-1'));
    expect(await screen.findByText('Bulletin board')).toBeInTheDocument();
  });

  it('explains a post that was already gone when the request landed', async () => {
    deletePost.mockRejectedValue({ status: 404, message: '' });
    renderThread();

    fireEvent.click(await screen.findByRole('button', { name: 'Delete your post' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete for good' }));

    expect(await screen.findByText('That post is already gone.')).toBeInTheDocument();
    expect(screen.queryByText('Delete this post and every comment on it?')).toBeNull();
    expect(screen.getByText('Selling my lab coat')).toBeInTheDocument();
  });
});
