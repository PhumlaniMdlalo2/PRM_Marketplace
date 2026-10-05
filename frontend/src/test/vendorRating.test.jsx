import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProductDetails from '../pages/ProductDetails'
import { AuthProvider } from '../auth/AuthContext'
import api from '../api/client'

/**
 * A seller's rating covers every listing they have, so the count printed beside it has to be the
 * seller's own review total. `vendor_profiles.rating_avg` existed from the first migration with
 * nothing ever writing it; `rating_count` arrived with it so the pair could be trusted together.
 *
 * Getting this wrong is easy and quiet: the number on screen stays a plausible-looking small count,
 * so a seller with forty reviews across five listings shows as "1 review" against their five-listing
 * average and reads as thinly reviewed.
 */

const PRODUCT_ID = '6f1a1f9e-0000-4000-8000-000000000001'

const productWithVendor = (vendor) => ({
  id: PRODUCT_ID,
  name: 'Refurbished laptop',
  description: 'Works well',
  price: 4500,
  category: 'Electronics',
  condition: 'USED',
  city: 'Cape Town',
  vendor,
})

/** Drops `ratingCount`, standing in for a profile written before that column existed. */
const withoutVendorCount = ({ ratingCount: _omitted, ...rest }) => rest

const VENDOR = {
  id: '11111111-1111-1111-1111-111111111111',
  businessName: 'Acme Repairs',
  verified: true,
  ratingAvg: 4.5,
  ratingCount: 40,
}

/**
 * Hands back `product` for the listing, and plausible shapes for everything else the page fetches.
 * The category call matters: the page maps its result with `.filter`, so answering it with the
 * product object crashes the render.
 */
const serveProduct = (product, reviews = []) => {
  api.defaults.adapter = async (config) => {
    const url = String(config.url)
    const data = url.includes('/reviews')
      ? reviews
      : url.includes('/category/')
        ? []
        : product
    return { data, status: 200, statusText: 'OK', headers: {}, config }
  }
}

const renderDetails = () =>
  render(
    <AuthProvider>
      <MemoryRouter initialEntries={[`/product/${PRODUCT_ID}`]}>
        <Routes>
          <Route path="/product/:id" element={<ProductDetails />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )

const reviewCountText = () => screen.getByText(/\(\d+ reviews?\)/).textContent

beforeEach(() => {
  vi.restoreAllMocks()
})

describe('the seller card on a listing', () => {
  it('counts the seller"s reviews, not the reviews on this listing', async () => {
    // One review on this product, forty across the seller's catalogue.
    serveProduct(productWithVendor(VENDOR), [{ id: 'r1', rating: 5, comment: 'Great' }])

    renderDetails()

    await waitFor(() => expect(screen.getByText('4.5')).toBeInTheDocument())
    expect(reviewCountText()).toBe('(40 reviews)')
  })

  it('agrees with the reviews endpoint for a seller with one listing', async () => {
    // The two counts coincide here, so this pins that the vendor count is not off by being a
    // different number entirely.
    serveProduct(productWithVendor({ ...VENDOR, ratingCount: 1 }), [
      { id: 'r1', rating: 5, comment: 'Great' },
    ])

    renderDetails()

    await waitFor(() => expect(screen.getByText('(1 review)')).toBeInTheDocument())
  })

  it('singularises a seller with exactly one review', async () => {
    serveProduct(productWithVendor({ ...VENDOR, ratingCount: 1 }))

    renderDetails()

    await waitFor(() => expect(screen.getByText('(1 review)')).toBeInTheDocument())
  })

  it('falls back to this listing"s reviews when the backend sends no vendor count', async () => {
    // Any profile written before the rating_count migration reports zero reviews no matter how busy
    // it is, so a seller with a real average should not be shown as having none.
    serveProduct(productWithVendor(withoutVendorCount(VENDOR)), [
      { id: 'r1', rating: 5, comment: 'Great' },
      { id: 'r2', rating: 4, comment: 'Fine' },
    ])

    renderDetails()

    await waitFor(() => expect(screen.getByText('(2 reviews)')).toBeInTheDocument())
  })

  it('shows no ratings yet when the seller has an average of zero', async () => {
    serveProduct(productWithVendor({ ...VENDOR, ratingAvg: null, ratingCount: 0 }))

    renderDetails()

    // An absent rating and a 0.00 are different things. Showing "0.0 (0 reviews)" would read as a
    // damning score rather than as a seller nobody has reviewed yet.
    await waitFor(() => expect(screen.getByText(/no ratings yet/i)).toBeInTheDocument())
    expect(screen.queryByText(/\(\d+ reviews?\)/)).toBeNull()
  })

  it('shows the verified badge only when the backend says so', async () => {
    serveProduct(productWithVendor(VENDOR))
    const { unmount } = renderDetails()
    await waitFor(() => expect(screen.getByText(/verified seller/i)).toBeInTheDocument())
    unmount()

    serveProduct(productWithVendor({ ...VENDOR, verified: false }))
    renderDetails()
    await waitFor(() => expect(screen.getByText('Acme Repairs')).toBeInTheDocument())
    expect(screen.queryByText(/verified seller/i)).toBeNull()
  })
})