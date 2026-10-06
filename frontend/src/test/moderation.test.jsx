import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Moderation from '../pages/Moderation';
import { AuthProvider } from '../auth/AuthContext';
import { TOKEN_STORAGE_KEY } from '../api/client';
import { listReportsForModeration, resolveReport } from '../api/reports';

/**
 * Reports have always been stored with a `faculty may resolve them` rule behind them and nothing in
 * the app to exercise it: the endpoints existed, the table filled up, and nobody with the role had a
 * screen to work from.
 *
 * What matters here is the split the server makes and how it is presented — a student sees why the
 * page is empty rather than a queue with nothing in it, a faculty account sees every report with its
 * status, and a decision lands as a status change with the note that goes back to the person who
 * reported it.
 */

vi.mock('../api/reports', () => ({
  createReport: vi.fn(),
  listMyReports: vi.fn(),
  listReportsForModeration: vi.fn(),
  resolveReport: vi.fn(),
  withdrawReport: vi.fn(),
}));

const FACULTY = { id: 'f-1', name: 'Prof Dlamini', role: 'FACULTY' };
const STUDENT = { id: 's-1', name: 'Themba', role: 'STUDENT' };

const report = (overrides = {}) => ({
  id: 'rep-1',
  reporter: { id: 'u-2', name: 'Thandi', avatarUrl: null },
  targetType: 'REVIEW',
  targetId: 'rev-1',
  reason: 'Copied my assignment and sold it as notes',
  status: 'OPEN',
  createdAt: '2026-09-01T10:00:00',
  resolvedAt: null,
  resolutionNotes: null,
  ...overrides,
});

const signInAs = (user) => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value');
  localStorage.setItem('prm.user', JSON.stringify(user));
};

const renderQueue = () => render(
  <MemoryRouter initialEntries={['/moderation']}>
    <AuthProvider>
      <Routes>
        <Route path="/moderation" element={<Moderation />} />
        <Route path="/login" element={<p>Sign in to continue</p>} />
      </Routes>
    </AuthProvider>
  </MemoryRouter>,
);

const rowFor = (text) => screen.getByText(text).closest('li');

beforeEach(() => {
  localStorage.clear();
  listReportsForModeration.mockReset();
  resolveReport.mockReset();
  listReportsForModeration.mockResolvedValue([]);
  resolveReport.mockResolvedValue(report({ id: 'rep-1', status: 'RESOLVED' }));
});

afterEach(() => {
  localStorage.clear();
});

describe('Moderation', () => {
  it('shows a faculty account every report waiting on a decision', async () => {
    signInAs(FACULTY);
    listReportsForModeration.mockResolvedValue([
      report(),
      report({
        id: 'rep-2',
        targetType: 'PRODUCT',
        targetId: 'prod-9',
        reason: 'The lamp is not what the photos show',
        status: 'RESOLVED',
        resolutionNotes: 'Listing withdrawn',
      }),
    ]);

    renderQueue();

    const first = await screen.findByText('Copied my assignment and sold it as notes');
    expect(within(rowFor('Copied my assignment and sold it as notes')).getByText('Open')).toBeInTheDocument();
    expect(first).toBeInTheDocument();
    expect(within(rowFor('The lamp is not what the photos show')).getByText('Resolved')).toBeInTheDocument();
    expect(within(rowFor('The lamp is not what the photos show')).getByText('Listing withdrawn')).toBeInTheDocument();
    expect(listReportsForModeration).toHaveBeenCalledTimes(1);
  });

  it('filters the queue by status', async () => {
    signInAs(FACULTY);
    listReportsForModeration.mockResolvedValue([
      report(),
      report({ id: 'rep-2', reason: 'Left rubbish at the collection point', status: 'RESOLVED' }),
    ]);

    renderQueue();
    await screen.findByText('Copied my assignment and sold it as notes');

    fireEvent.click(screen.getByRole('button', { name: 'Resolved' }));

    expect(screen.getByRole('button', { name: 'Resolved' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.queryByText('Copied my assignment and sold it as notes')).not.toBeInTheDocument();
    expect(screen.getByText('Left rubbish at the collection point')).toBeInTheDocument();
  });

  it('resolves a report with the note that goes back to the reporter', async () => {
    signInAs(FACULTY);
    listReportsForModeration.mockResolvedValue([report()]);

    renderQueue();
    const loaded = await screen.findByText('Copied my assignment and sold it as notes');
    const row = loaded.closest('li');
    await within(row).findByRole('button', { name: 'Resolve' });

    fireEvent.change(within(row).getByLabelText('Notes for the reporter'), {
      target: { value: 'Warning sent to the seller' },
    });
    fireEvent.click(within(row).getByRole('button', { name: 'Resolve' }));

    await waitFor(() => expect(resolveReport).toHaveBeenCalledWith('rep-1', {
      status: 'RESOLVED',
      notes: 'Warning sent to the seller',
    }));
    expect(await within(rowFor('Copied my assignment and sold it as notes')).findByText('Resolved')).toBeInTheDocument();
  });

  it('says so when the report has left the queue by the time a decision reaches it', async () => {
    signInAs(FACULTY);
    listReportsForModeration.mockResolvedValue([report()]);
    resolveReport.mockRejectedValue({ status: 404, message: '' });

    renderQueue();
    const loaded = await screen.findByText('Copied my assignment and sold it as notes');
    const row = loaded.closest('li');
    fireEvent.click(await within(row).findByRole('button', { name: 'Resolve' }));

    expect(await within(row).findByRole('alert')).toHaveTextContent('no longer in the queue');
    expect(within(row).getByText('Open')).toBeInTheDocument();
  });

  it('tells a student why the page is empty, without asking the server for a queue they cannot have', async () => {
    signInAs(STUDENT);

    renderQueue();

    expect(await screen.findByText('Moderation is for faculty accounts')).toBeInTheDocument();
    expect(listReportsForModeration).not.toHaveBeenCalled();
  });

  it('reports a failure to load as a failure, not an empty queue', async () => {
    signInAs(FACULTY);
    listReportsForModeration.mockRejectedValue({ status: 500, message: '' });

    renderQueue();

    expect(await screen.findByRole('alert')).toHaveTextContent('The queue could not be loaded.');
    expect(screen.queryByText('Nothing to moderate')).not.toBeInTheDocument();
  });
});
