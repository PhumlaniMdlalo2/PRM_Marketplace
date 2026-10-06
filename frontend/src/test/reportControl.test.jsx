import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import ReportControl from '../components/report/ReportControl';
import { AuthProvider } from '../auth/AuthContext';
import { TOKEN_STORAGE_KEY } from '../api/client';
import { createReport } from '../api/reports';

/**
 * The report button that sits under a review, a post, a comment or a listing.
 *
 * The complaints it exists to fix are in the old form in Settings: it offered a target type the
 * server rejects, asked for an id out of a URL, and could only be reached by somebody who already
 * knew reporting existed. This file pins down the replacement — the target arrives with the control,
 * the reason is the only thing asked for, and the two ways it fails (a reader who is not signed in,
 * and a server that refuses) both land somewhere sensible.
 */

vi.mock('../api/reports', () => ({
  createReport: vi.fn(),
  listMyReports: vi.fn(),
  listReportsForModeration: vi.fn(),
  resolveReport: vi.fn(),
  withdrawReport: vi.fn(),
}));

const LoginMarker = () => {
  const location = useLocation();
  return (
    <div>
      <p>pathname:{location.pathname}</p>
      <p>from:{location.state?.from ?? 'none'}</p>
    </div>
  );
};

const signIn = () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'test.token.value');
  localStorage.setItem('prm.user', JSON.stringify({ id: 'u-1', name: 'Me' }));
};

const renderControl = (props = {}) => render(
  <MemoryRouter initialEntries={['/product/p-1']}>
    <AuthProvider>
      <Routes>
        <Route
          path="/product/p-1"
          element={(
            <ReportControl
              targetType="REVIEW"
              targetId="rev-1"
              label="Report this review"
              {...props}
            />
          )}
        />
        <Route path="/login" element={<LoginMarker />} />
      </Routes>
    </AuthProvider>
  </MemoryRouter>,
);

const reasonField = () => screen.getByPlaceholderText('Describe the problem');

const reasonGone = () => expect(screen.queryByPlaceholderText('Describe the problem')).toBeNull();

beforeEach(() => {
  localStorage.clear();
  createReport.mockReset();
  createReport.mockResolvedValue({ id: 'rep-9', status: 'OPEN' });
});

afterEach(() => {
  localStorage.clear();
});

describe('ReportControl', () => {
  it('sends a reader who is not signed in to sign in, keeping the page they were on', async () => {
    renderControl();

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));

    expect(await screen.findByText('pathname:/login')).toBeInTheDocument();
    expect(screen.getByText('from:/product/p-1')).toBeInTheDocument();
    expect(createReport).not.toHaveBeenCalled();
  });

  it('will not file an empty reason', async () => {
    signIn();
    renderControl();

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));
    expect(reasonField()).toBeInTheDocument();

    expect(screen.getByRole('button', { name: 'File report' })).toBeDisabled();
    fireEvent.submit(reasonField().closest('form'));
    expect(createReport).not.toHaveBeenCalled();
  });

  it('files the report against the thing the control sits next to', async () => {
    signIn();
    renderControl({ targetType: 'COMMENT', targetId: 'c-7' });

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));
    fireEvent.change(reasonField(), { target: { value: '  Selling stolen notes  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'File report' }));

    await waitFor(() => expect(createReport).toHaveBeenCalledWith({
      targetType: 'COMMENT',
      targetId: 'c-7',
      reason: 'Selling stolen notes',
    }));
    expect(await screen.findByRole('status')).toHaveTextContent('Report filed.');
    reasonGone();
  });

  it('explains a refusal in its own words, since an empty 400 would read as a dropped connection', async () => {
    signIn();
    createReport.mockRejectedValue({ status: 400, message: '' });
    renderControl();

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));
    fireEvent.change(reasonField(), { target: { value: 'spam' } });
    fireEvent.click(screen.getByRole('button', { name: 'File report' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('That report was not accepted.');
    expect(reasonField()).toBeInTheDocument();
  });

  it('shows the server message for anything else and leaves the reason to try again', async () => {
    signIn();
    createReport.mockRejectedValue({ status: 503, message: 'Moderation is offline' });
    renderControl();

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));
    fireEvent.change(reasonField(), { target: { value: 'spam' } });
    fireEvent.click(screen.getByRole('button', { name: 'File report' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Moderation is offline');
    expect(reasonField()).toBeInTheDocument();
  });

  it('closes when the reader changes their mind', async () => {
    signIn();
    renderControl();

    fireEvent.click(screen.getByRole('button', { name: 'Report this review' }));
    fireEvent.change(reasonField(), { target: { value: 'not sure any more' } });
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    reasonGone();
    expect(screen.getByRole('button', { name: 'Report this review' })).toBeInTheDocument();
    expect(createReport).not.toHaveBeenCalled();
  });
});
