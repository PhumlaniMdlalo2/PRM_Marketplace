import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import Landing from '../pages/Landing';

describe('landing page marketplace sections', () => {
  it('shows the goods categories by default', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /a marketplace for campus life/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Goods', selected: true })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Textbooks and study materials' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Free items and giveaways' })).toBeInTheDocument();
  });

  it('switches to services and community using the tabs', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('tab', { name: 'Services' }));
    expect(screen.getByRole('heading', { name: 'Tutoring and academic help' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /browse sellers/i })).toHaveAttribute('href', '/vendors');

    fireEvent.click(screen.getByRole('tab', { name: 'Community' }));
    expect(screen.getByRole('heading', { name: 'Accommodation and roommates' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /visit the bulletin/i })).toHaveAttribute('href', '/bulletin');
  });

  it('supports arrow-key tab navigation', () => {
    render(
      <MemoryRouter>
        <Landing />
      </MemoryRouter>,
    );

    const goodsTab = screen.getByRole('tab', { name: 'Goods' });
    goodsTab.focus();
    fireEvent.keyDown(goodsTab, { key: 'ArrowRight' });

    expect(screen.getByRole('tab', { name: 'Services', selected: true })).toHaveFocus();
    expect(screen.getByRole('heading', { name: 'Tutoring and academic help' })).toBeInTheDocument();
  });
});
