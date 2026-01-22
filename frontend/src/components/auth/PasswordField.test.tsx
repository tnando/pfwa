import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/testUtils';
import userEvent from '@testing-library/user-event';
import PasswordField from './PasswordField';

describe('PasswordField', () => {
  it('should render with label', () => {
    render(<PasswordField label="Password" />);
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
  });

  it('should render as password type by default', () => {
    render(<PasswordField label="Password" />);
    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');
  });

  it('should toggle password visibility when clicking the toggle button', async () => {
    const user = userEvent.setup();
    render(<PasswordField label="Password" />);

    const input = screen.getByLabelText('Password');
    expect(input).toHaveAttribute('type', 'password');

    // Find and click the visibility toggle button
    const toggleButton = screen.getByRole('button', { name: 'Show password' });
    await user.click(toggleButton);

    expect(input).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument();
  });

  it('should hide password after showing it', async () => {
    const user = userEvent.setup();
    render(<PasswordField label="Password" />);

    const input = screen.getByLabelText('Password');

    // Show password
    await user.click(screen.getByRole('button', { name: 'Show password' }));
    expect(input).toHaveAttribute('type', 'text');

    // Hide password
    await user.click(screen.getByRole('button', { name: 'Hide password' }));
    expect(input).toHaveAttribute('type', 'password');
  });

  it('should not show toggle button when showToggle is false', () => {
    render(<PasswordField label="Password" showToggle={false} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('should render with error state', () => {
    render(<PasswordField label="Password" error helperText="Password is required" />);
    expect(screen.getByText('Password is required')).toBeInTheDocument();
  });

  it('should render as disabled', () => {
    render(<PasswordField label="Password" disabled />);
    expect(screen.getByLabelText('Password')).toBeDisabled();
  });

  it('should accept and display input value', async () => {
    const user = userEvent.setup();
    render(<PasswordField label="Password" />);

    const input = screen.getByLabelText('Password');
    await user.type(input, 'secret123');

    expect(input).toHaveValue('secret123');
  });

  it('should forward ref correctly', () => {
    const ref = { current: null };
    render(<PasswordField label="Password" inputRef={ref} />);
    expect(ref.current).toBeInstanceOf(HTMLInputElement);
  });
});
