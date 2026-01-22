import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';
import ProtectedRoute from './ProtectedRoute';
import { AuthProvider } from '@/context';

const theme = createTheme();

// Mock the auth API
vi.mock('@/api', () => ({
  authApi: {
    refreshToken: vi.fn(),
    logout: vi.fn(),
    login: vi.fn(),
    register: vi.fn(),
  },
  getErrorMessage: vi.fn(() => 'Error'),
}));

const renderWithProviders = (
  _ui: React.ReactElement,
  { initialRoute = '/protected' } = {}
) => {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[initialRoute]}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<div>Login Page</div>} />
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div>Protected Content</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  );
};

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should show loading state initially when user is stored', async () => {
    // Simulate stored user (triggers token refresh)
    localStorage.getItem = vi.fn().mockReturnValue(
      JSON.stringify({ id: '1', email: 'test@example.com', firstName: null, lastName: null })
    );

    renderWithProviders(<div>Test</div>);

    // Should show loading while checking auth
    expect(screen.getByText('Checking authentication...')).toBeInTheDocument();
  });

  it('should redirect to login when not authenticated', async () => {
    // No stored user
    localStorage.getItem = vi.fn().mockReturnValue(null);

    renderWithProviders(<div>Test</div>);

    // Should redirect to login page
    expect(await screen.findByText('Login Page')).toBeInTheDocument();
  });

  it('should render children when authenticated', async () => {
    // Mock successful auth state - need to mock the refresh token to succeed
    const { authApi } = await import('@/api');
    (authApi.refreshToken as ReturnType<typeof vi.fn>).mockResolvedValueOnce({ expiresIn: 900 });

    localStorage.getItem = vi.fn().mockReturnValue(
      JSON.stringify({ id: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User' })
    );

    renderWithProviders(<div>Test</div>);

    // Wait for auth check to complete
    expect(await screen.findByText('Protected Content')).toBeInTheDocument();
  });
});
