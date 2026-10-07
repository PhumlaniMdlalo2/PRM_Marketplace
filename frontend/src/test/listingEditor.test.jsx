import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ListingEditor from '../components/listing/ListingEditor';
import {
  CATEGORY_OPTIONS,
  emptyListingForm,
  listingFromProduct,
  toListingPayload,
} from '../lib/listingForm';

const ListingEditorHarness = () => {
  const [values, setValues] = useState(emptyListingForm);

  return (
    <ListingEditor
      values={values}
      errors={{}}
      onChange={(name, value) => setValues((current) => ({ ...current, [name]: value }))}
      onSubmit={vi.fn()}
      submitLabel="Create listing"
      onPickImage={vi.fn()}
    />
  );
};

describe('listing type selection', () => {
  it('keeps product category suggestions unique and in sentence case', () => {
    expect(CATEGORY_OPTIONS).toHaveLength(new Set(CATEGORY_OPTIONS).size);
    expect(CATEGORY_OPTIONS).not.toContain('ELECTRONICS');
    expect(CATEGORY_OPTIONS).not.toContain('FURNITURE');
  });

  it('switches between separate product and service categories', () => {
    render(<ListingEditorHarness />);

    expect(screen.getByRole('button', { name: 'Product' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Electronics' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Tutoring and academic help' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Service' }));

    expect(screen.getByRole('button', { name: 'Service' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Tutoring and academic help' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Electronics' })).not.toBeInTheDocument();
    expect(screen.queryByText('Condition')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Service name')).toBeInTheDocument();
    expect(screen.getByLabelText('How many bookings are available')).toBeInTheDocument();
  });

  it('recognises existing service categories when editing and excludes the UI type from the API payload', () => {
    const values = listingFromProduct({
      name: 'Math tutoring',
      category: 'Tutoring and academic help',
      price: 150,
      stockQuantity: 4,
      condition: 'NEW',
      city: 'Cape Town',
      province: 'Western Cape',
    });

    expect(values.listingType).toBe('SERVICE');
    expect(toListingPayload(values)).not.toHaveProperty('listingType');
  });
});
