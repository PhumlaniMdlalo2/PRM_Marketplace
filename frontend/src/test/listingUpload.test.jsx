import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ListingEditor from '../components/listing/ListingEditor';

/**
 * The listing photo upload is the only multipart call in the app. These cases pin the upload
 * path through the shared editor and the two pages that wire it into their submit flow.
 */

vi.mock('../api/products', () => ({
  create: vi.fn(),
  getMyVendorProfile: vi.fn(),
  getById: vi.fn(),
  update: vi.fn(),
  retire: vi.fn(),
  reactivate: vi.fn(),
}));

vi.mock('../api/uploads', () => ({
  uploadImage: vi.fn(),
}));

const _VENDOR_ID = 'vendor-1';

/**
 * The listing photo upload is the only multipart call in the app. These cases pin the upload
 * path through the shared editor and the two pages that wire it into their submit flow.
 */

afterEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  sessionStorage.clear();
});

describe('ListingEditor photo upload UI', () => {
  const baseValues = {
    name: 'Test product',
    category: 'ELECTRONICS',
    price: '100',
    stock: '1',
    description: 'A test product',
    location: 'Cape Town, Western Cape',
    condition: 'NEW',
    active: true,
    imageUrl: '',
  };

  it('shows the empty state when no image is set', () => {
    render(
      <ListingEditor
        values={baseValues}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={vi.fn()}
      />
    );

    expect(screen.getByText(/No photo yet\. Pick one below/)).toBeInTheDocument();
    expect(screen.getByLabelText('Product image')).toBeInTheDocument();
  });

  it('shows the preview when imageUrl is provided', () => {
    render(
      <ListingEditor
        values={{ ...baseValues, imageUrl: '/api/uploads/images/existing.png' }}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={vi.fn()}
      />
    );

    expect(screen.getByRole('img')).toHaveAttribute('src', '/api/uploads/images/existing.png');
  });

  it('calls onPickImage when a file is chosen', async () => {
    const onPickImage = vi.fn();
    const user = userEvent.setup();

    render(
      <ListingEditor
        values={baseValues}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={onPickImage}
      />
    );

    const input = screen.getByLabelText('Product image');
    await user.upload(input, new File(['fake'], 'photo.png', { type: 'image/png' }));

    expect(onPickImage).toHaveBeenCalledWith(expect.any(File));
  });

  it('disables the input and shows status while uploading', async () => {
    userEvent.setup();

    render(
      <ListingEditor
        values={baseValues}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={vi.fn()}
        uploading={true}
      />
    );

    expect(screen.getByLabelText('Product image')).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Uploading photo…');
  });

  it('shows the upload error when provided', () => {
    render(
      <ListingEditor
        values={baseValues}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={vi.fn()}
        uploadError='That image is too large to upload'
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent('That image is too large to upload');
  });

  it('allows re-picking the same file by clearing the input value', async () => {
    const onPickImage = vi.fn();
    const user = userEvent.setup();

    render(
      <ListingEditor
        values={baseValues}
        errors={{}}
        onChange={vi.fn()}
        onSubmit={vi.fn()}
        submitLabel="Save"
        onPickImage={onPickImage}
      />
    );

    const input = screen.getByLabelText('Product image');
    const file = new File(['fake'], 'photo.png', { type: 'image/png' });

    await user.upload(input, file);
    expect(onPickImage).toHaveBeenCalledTimes(1);

    await user.upload(input, file);
    expect(onPickImage).toHaveBeenCalledTimes(2);
  });
});