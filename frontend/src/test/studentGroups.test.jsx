import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import StudentGroups from '../pages/StudentGroups';
import { TOKEN_STORAGE_KEY } from '../api/client';
import {
  createStudentGroupPost, joinStudentGroup, listStudentGroupPosts, listStudentGroups,
} from '../api/studentGroups';

vi.mock('../api/studentGroups', () => ({
  createStudentGroup: vi.fn(),
  createStudentGroupPost: vi.fn(),
  joinStudentGroup: vi.fn(),
  leaveStudentGroup: vi.fn(),
  listStudentGroupPosts: vi.fn(),
  listStudentGroups: vi.fn(),
}));

const GROUP = {
  id: 'group-1',
  name: 'Computing society',
  description: 'Projects, study sessions and course questions.',
  campus: 'Cape Town',
  memberCount: 12,
  joined: true,
};

const DISCUSSION = {
  id: 'post-1',
  title: 'Study group this Thursday',
  body: 'Bring your networking notes.',
};

const renderGroups = () => render(
  <AuthProvider>
    <MemoryRouter>
      <StudentGroups />
    </MemoryRouter>
  </AuthProvider>,
);

beforeEach(() => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value');
  localStorage.setItem('prm.user', JSON.stringify({
    id: 'student-1',
    name: 'Test student',
    role: 'STUDENT',
    campus: 'Cape Town',
    verified: true,
  }));
  listStudentGroups.mockResolvedValue([GROUP]);
  listStudentGroupPosts.mockResolvedValue([DISCUSSION]);
  createStudentGroupPost.mockResolvedValue({ id: 'post-2', title: 'New topic', body: 'Details' });
  joinStudentGroup.mockResolvedValue(undefined);
});

describe('student campus groups', () => {
  it('shows discussions only after loading the signed-in student’s campus group', async () => {
    renderGroups();

    expect(await screen.findByRole('heading', { name: GROUP.name })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: DISCUSSION.title })).toHaveAttribute(
      'href',
      `/bulletin/${DISCUSSION.id}`,
    );
    expect(listStudentGroupPosts).toHaveBeenCalledWith(GROUP.id);
  });

  it('lets a student join a campus group and then load its private discussions', async () => {
    const user = userEvent.setup();
    listStudentGroups
      .mockResolvedValueOnce([{ ...GROUP, joined: false }])
      .mockResolvedValueOnce([GROUP]);
    listStudentGroupPosts.mockResolvedValue([]);
    renderGroups();

    await screen.findByRole('heading', { name: GROUP.name });
    await user.click(screen.getByRole('button', { name: 'Join' }));

    await waitFor(() => expect(joinStudentGroup).toHaveBeenCalledWith(GROUP.id));
    expect(await screen.findByText('No discussions yet. Start one above.')).toBeInTheDocument();
  });

  it('posts new topics into the selected private group', async () => {
    const user = userEvent.setup();
    renderGroups();

    await screen.findByRole('heading', { name: GROUP.name });
    await user.type(screen.getByLabelText('Start a discussion'), 'New topic');
    await user.type(screen.getByLabelText('Discussion text'), 'Details for the group');
    await user.click(screen.getByRole('button', { name: 'Post to group' }));

    await waitFor(() => expect(createStudentGroupPost).toHaveBeenCalledWith(GROUP.id, {
      title: 'New topic',
      body: 'Details for the group',
    }));
    expect(await screen.findByRole('link', { name: 'New topic' })).toBeInTheDocument();
  });
});
