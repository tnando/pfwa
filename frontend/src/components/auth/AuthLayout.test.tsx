import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/testUtils';
import AuthLayout from './AuthLayout';

describe('AuthLayout', () => {
  it('should render with title', () => {
    render(
      <AuthLayout title="Test Title">
        <div>Content</div>
      </AuthLayout>
    );
    expect(screen.getByRole('heading', { name: 'Test Title' })).toBeInTheDocument();
  });

  it('should render with subtitle when provided', () => {
    render(
      <AuthLayout title="Test Title" subtitle="Test Subtitle">
        <div>Content</div>
      </AuthLayout>
    );
    expect(screen.getByText('Test Subtitle')).toBeInTheDocument();
  });

  it('should not render subtitle when not provided', () => {
    render(
      <AuthLayout title="Test Title">
        <div>Content</div>
      </AuthLayout>
    );
    // Only the title should be in the page title area
    expect(screen.queryByText('Test Subtitle')).not.toBeInTheDocument();
  });

  it('should render children content', () => {
    render(
      <AuthLayout title="Test Title">
        <button>Submit</button>
      </AuthLayout>
    );
    expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument();
  });

  it('should display app branding', () => {
    render(
      <AuthLayout title="Test Title">
        <div>Content</div>
      </AuthLayout>
    );
    expect(screen.getByText('Personal Finance')).toBeInTheDocument();
    expect(screen.getByText('Track your income, expenses, and budgets')).toBeInTheDocument();
  });

  it('should display copyright footer', () => {
    render(
      <AuthLayout title="Test Title">
        <div>Content</div>
      </AuthLayout>
    );
    const currentYear = new Date().getFullYear();
    expect(screen.getByText(new RegExp(`${currentYear}`))).toBeInTheDocument();
    expect(screen.getByText(/PFWA\. All rights reserved/)).toBeInTheDocument();
  });

  it('should render as main element for accessibility', () => {
    render(
      <AuthLayout title="Test Title">
        <div>Content</div>
      </AuthLayout>
    );
    expect(screen.getByRole('main')).toBeInTheDocument();
  });
});
