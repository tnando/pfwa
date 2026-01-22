import { describe, it, expect } from 'vitest';
import { render, screen } from '@/test/testUtils';
import PasswordStrengthMeter from './PasswordStrengthMeter';

describe('PasswordStrengthMeter', () => {
  it('should not render anything when password is empty', () => {
    const { container } = render(<PasswordStrengthMeter password="" />);
    expect(container.firstChild).toBeNull();
  });

  it('should render strength bar when password is provided', () => {
    render(<PasswordStrengthMeter password="a" />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('should display "Very Weak" for password with only lowercase', () => {
    render(<PasswordStrengthMeter password="abc" />);
    expect(screen.getByText('Very Weak')).toBeInTheDocument();
  });

  it('should display "Weak" for password meeting some requirements', () => {
    render(<PasswordStrengthMeter password="aaaaaaaa" />);
    expect(screen.getByText('Weak')).toBeInTheDocument();
  });

  it('should display "Very Strong" for password meeting all requirements', () => {
    render(<PasswordStrengthMeter password="SecurePass123!" />);
    expect(screen.getByText('Very Strong')).toBeInTheDocument();
  });

  it('should display requirements checklist by default', () => {
    render(<PasswordStrengthMeter password="test" />);
    expect(screen.getByText('At least 8 characters')).toBeInTheDocument();
    expect(screen.getByText('One uppercase letter (A-Z)')).toBeInTheDocument();
    expect(screen.getByText('One lowercase letter (a-z)')).toBeInTheDocument();
    expect(screen.getByText('One number (0-9)')).toBeInTheDocument();
    expect(screen.getByText(/One special character/)).toBeInTheDocument();
  });

  it('should hide requirements checklist when showRequirements is false', () => {
    render(<PasswordStrengthMeter password="test" showRequirements={false} />);
    // With showRequirements=false, the list should be collapsed
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('should show check icons for met requirements', () => {
    render(<PasswordStrengthMeter password="SecurePass123!" />);
    // All requirements should have check icons (success indicators)
    const listItems = screen.getAllByRole('listitem');
    expect(listItems).toHaveLength(5);
  });

  it('should update when password changes', () => {
    const { rerender } = render(<PasswordStrengthMeter password="a" />);
    expect(screen.getByText('Very Weak')).toBeInTheDocument();

    rerender(<PasswordStrengthMeter password="SecurePass123!" />);
    expect(screen.getByText('Very Strong')).toBeInTheDocument();
  });

  it('should show "Fair" for password with 3 requirements met', () => {
    // lowercase + length + digit = 3
    render(<PasswordStrengthMeter password="password1" />);
    expect(screen.getByText('Fair')).toBeInTheDocument();
  });

  it('should show "Strong" for password with 4 requirements met', () => {
    // uppercase + lowercase + length + digit = 4
    render(<PasswordStrengthMeter password="Password1" />);
    expect(screen.getByText('Strong')).toBeInTheDocument();
  });
});
