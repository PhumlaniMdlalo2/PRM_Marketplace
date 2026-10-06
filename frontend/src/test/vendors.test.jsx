import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Vendors from '../pages/Vendors';
import VendorStore from '../pages/VendorStore';
import { AuthProvider } from '../auth/AuthContext';
import { getVendorProfile, listVendorProfiles } from '../api/vendorProfile';
import { listByVendor } from '../api/products';

/**
 * The catalogue always had sellers behind it and the app never showed them: `GET
 * /api/vendor-profiles` was public from the day it was written and nothing called it, so a shop
 * could only be reached by way of a single listing — and a seller with nothing listed at that
 * moment had no page anyone could land on, share or find by name.
 *
 * The cases worth pinning down are the joins: the directory's filter, the shop page that needs the
 * profile and its listings together, and the 404 that is how an id nobody recognises arrives.
 */

vi.mock('../api/vendorProfile', () => ({
  listVendorProfiles: vi.fn(),
  getVendorProfile: vi.fn(),
  getMyVendorProfile: vi.fn(),
  createVendorProfile: vi.fn(),
  updateVendorProfile: vi.fn(),
}));

vi.mock('../api/products', () => ({
  listByVendor: vi.fn(),
  getById: vi.fn(),
  search: vi.fn(),
}));

const shop = (overrides = {}) => ({
  id: 'vendor-1',
  businessName: 'Campus Ceramics',
  verified: true,
  ratingAvg: 4.6,
  ratingCount: 12,
  createdAt: '2025-03-01T10:00:00',
  ...overrides,
});

const listing = (overrides = {}) => ({
  id: 'product-1',
  name: 'Glazed desk lamp',
  price: 250,
  city: 'Cape Town',
  province: 'Western Cape',
  imageUrl: '',
  active: true,
  ...overrides,
});

const renderDirectory = () => render(
  <MemoryRouter initialEntries={['/vendors']}>
    <AuthProvider>
      <Routes>
        <Route path="/vendors" element={<Vendors />} />
        <Route path="/vendors/:id" element={<VendorStore />} />
        <Route path="/login" element={<p>Sign in to continue</p>} />
      </Routes>
    </AuthProvider>
  </MemoryRouter>,
);

beforeEach(() => {
  listVendorProfiles.mockResolvedValue([]);
  getVendorProfile.mockResolvedValue(shop());
  listByVendor.mockResolvedValue([listing()]);
});

describe('Vendors', () => {
  it('lists every shop the directory returns', async () => {
    listVendorProfiles.mockResolvedValue([
      shop(),
      shop({ id: 'vendor-2', businessName: 'Second Hand Books', verified: false, ratingAvg: null, ratingCount: 0 }),
    ]);
    renderDirectory();

    expect(await screen.findByText('Campus Ceramics')).toBeInTheDocument();
    expect(screen.getByText('Second Hand Books')).toBeInTheDocument();
    expect(screen.getByText('Verified')).toBeInTheDocument();
    expect(screen.getByText('4.6 · 12 ratings')).toBeInTheDocument();
    expect(screen.getByText('No ratings yet')).toBeInTheDocument();
  });

  it('filters the directory as the shopper types', async () => {
    listVendorProfiles.mockResolvedValue([
      shop(),
      shop({ id: 'vendor-2', businessName: 'Bike Spares' }),
    ]);
    renderDirectory();

    fireEvent.change(await screen.findByLabelText('Search sellers'), {
      target: { value: 'bike' },
    });

    expect(screen.getByText('Bike Spares')).toBeInTheDocument();
    expect(screen.queryByText('Campus Ceramics')).toBeNull();
  });

  it('says so plainly when nobody has registered a shop', async () => {
    renderDirectory();

    expect(await screen.findByText('No sellers yet')).toBeInTheDocument();
    expect(listVendorProfiles).toHaveBeenCalledTimes(1);
  });

  it('explains a directory that could not be loaded', async () => {
    listVendorProfiles.mockRejectedValue({ status: 500, message: '' });
    renderDirectory();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The directory could not be loaded. Please try again.',
    );
  });

  it('takes the shopper from a shop in the directory to its page', async () => {
    listVendorProfiles.mockResolvedValue([shop()]);
    renderDirectory();

    fireEvent.click(await screen.findByRole('link', { name: /Campus Ceramics/ }));

    expect(await screen.findByRole('heading', { name: 'Shop' })).toBeInTheDocument();
    expect(getVendorProfile).toHaveBeenCalledWith('vendor-1');
    expect(listByVendor).toHaveBeenCalledWith('vendor-1');
    expect(await screen.findByRole('heading', { name: 'Glazed desk lamp' })).toBeInTheDocument();
  });
});

describe('VendorStore', () => {
  it('shows the shop and everything it has listed', async () => {
    render(
      <MemoryRouter initialEntries={['/vendors/vendor-1']}>
        <AuthProvider>
          <Routes>
            <Route path="/vendors/:id" element={<VendorStore />} />
            <Route path="/login" element={<p>Sign in to continue</p>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('heading', { name: 'Shop' })).toBeInTheDocument();
    expect(screen.getByText('Campus Ceramics')).toBeInTheDocument();
    expect(screen.getByText('4.6 · 12 ratings')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Glazed desk lamp' })).toBeInTheDocument();
    expect(screen.getByText('Member since 2025')).toBeInTheDocument();
  });

  it('says so when the shop has nothing listed', async () => {
    listByVendor.mockResolvedValue([]);
    render(
      <MemoryRouter initialEntries={['/vendors/vendor-1']}>
        <AuthProvider>
          <Routes>
            <Route path="/vendors/:id" element={<VendorStore />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText('Nothing listed right now')).toBeInTheDocument();
    expect(screen.getByText('Campus Ceramics')).toBeInTheDocument();
  });

  it('tells the shopper when the shop is not on the directory', async () => {
    getVendorProfile.mockRejectedValue({ status: 404, message: '' });
    render(
      <MemoryRouter initialEntries={['/vendors/vendor-1']}>
        <AuthProvider>
          <Routes>
            <Route path="/vendors/:id" element={<VendorStore />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This shop is not on the directory.',
    );
    expect(await screen.findByRole('link', { name: 'Back to all sellers' })).toBeInTheDocument();
  });

  it('sends a signed-out visitor to sign in rather than dropping the save', async () => {
    render(
      <MemoryRouter initialEntries={['/vendors/vendor-1']}>
        <AuthProvider>
          <Routes>
            <Route path="/vendors/:id" element={<VendorStore />} />
            <Route path="/login" element={<p>Sign in to continue</p>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole('button', { name: 'Add Glazed desk lamp to favourites' }),
    );

    expect(await screen.findByText('Sign in to continue')).toBeInTheDocument();
  });

  it('ignores a listings response that is not a list', async () => {
    listByVendor.mockResolvedValue({});
    render(
      <MemoryRouter initialEntries={['/vendors/vendor-1']}>
        <AuthProvider>
          <Routes>
            <Route path="/vendors/:id" element={<VendorStore />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText('Nothing listed right now')).toBeInTheDocument();
    await waitFor(() => expect(listByVendor).toHaveBeenCalledWith('vendor-1'));
  });
});
