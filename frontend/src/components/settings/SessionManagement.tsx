import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Alert,
  CircularProgress,
  Divider,
  Tooltip,
} from '@mui/material';
import {
  Computer,
  PhoneAndroid,
  Tablet,
  Delete,
  Logout,
  LocationOn,
  AccessTime,
} from '@mui/icons-material';
import { authApi, getErrorMessage } from '@/api';
import type { Session } from '@/types';

/**
 * Get device icon based on device type string
 */
const getDeviceIcon = (deviceType: string) => {
  const lower = deviceType.toLowerCase();
  if (lower.includes('iphone') || lower.includes('android') || lower.includes('mobile')) {
    return <PhoneAndroid />;
  }
  if (lower.includes('ipad') || lower.includes('tablet')) {
    return <Tablet />;
  }
  return <Computer />;
};

/**
 * Format date for display
 */
const formatDate = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} minute${diffMins === 1 ? '' : 's'} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
  if (diffDays < 7) return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;

  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
};

/**
 * SessionManagement - List and manage active sessions
 * Shows all devices where user is logged in with ability to revoke access
 */
const SessionManagement: React.FC = () => {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revokeDialogOpen, setRevokeDialogOpen] = useState(false);
  const [revokeAllDialogOpen, setRevokeAllDialogOpen] = useState(false);
  const [sessionToRevoke, setSessionToRevoke] = useState<Session | null>(null);
  const [isRevoking, setIsRevoking] = useState(false);

  /**
   * Fetch active sessions
   */
  const fetchSessions = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await authApi.getSessions();
      setSessions(response.sessions);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  /**
   * Handle revoking a single session
   */
  const handleRevokeSession = async () => {
    if (!sessionToRevoke) return;

    try {
      setIsRevoking(true);
      await authApi.revokeSession(sessionToRevoke.id);
      setSessions((prev) => prev.filter((s) => s.id !== sessionToRevoke.id));
      setRevokeDialogOpen(false);
      setSessionToRevoke(null);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsRevoking(false);
    }
  };

  /**
   * Handle revoking all sessions
   */
  const handleRevokeAllSessions = async () => {
    try {
      setIsRevoking(true);
      await authApi.revokeAllSessions();
      // This will log out the current user as well
      // The axios interceptor will handle redirect to login
    } catch (err) {
      setError(getErrorMessage(err));
      setIsRevoking(false);
    }
    setRevokeAllDialogOpen(false);
  };

  /**
   * Open revoke dialog for a specific session
   */
  const openRevokeDialog = (session: Session) => {
    setSessionToRevoke(session);
    setRevokeDialogOpen(true);
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6" component="h2">
              Active Sessions
            </Typography>
            {sessions.length > 1 && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<Logout />}
                onClick={() => setRevokeAllDialogOpen(true)}
                size="small"
              >
                Logout all devices
              </Button>
            )}
          </Box>

          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            These are the devices that are currently logged into your account.
            Revoke any session that you do not recognize.
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <List>
            {sessions.map((session, index) => (
              <React.Fragment key={session.id}>
                {index > 0 && <Divider component="li" />}
                <ListItem
                  sx={{
                    py: 2,
                    backgroundColor: session.isCurrent ? 'action.selected' : 'transparent',
                    borderRadius: 1,
                  }}
                >
                  <ListItemIcon>{getDeviceIcon(session.deviceType)}</ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body1">{session.deviceType}</Typography>
                        {session.isCurrent && (
                          <Chip
                            label="Current"
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        )}
                      </Box>
                    }
                    secondary={
                      <Box sx={{ mt: 0.5 }}>
                        {session.location && (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                            <LocationOn sx={{ fontSize: 14, color: 'text.secondary' }} />
                            <Typography variant="caption" color="text.secondary">
                              {session.location}
                            </Typography>
                          </Box>
                        )}
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.5 }}>
                          <AccessTime sx={{ fontSize: 14, color: 'text.secondary' }} />
                          <Typography variant="caption" color="text.secondary">
                            Last active: {formatDate(session.lastActive)}
                          </Typography>
                        </Box>
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    {session.isCurrent ? (
                      <Tooltip title="Cannot revoke current session. Use logout instead.">
                        <span>
                          <IconButton edge="end" disabled aria-label="Cannot revoke current session">
                            <Delete />
                          </IconButton>
                        </span>
                      </Tooltip>
                    ) : (
                      <Tooltip title="Revoke this session">
                        <IconButton
                          edge="end"
                          aria-label="Revoke session"
                          onClick={() => openRevokeDialog(session)}
                          color="error"
                        >
                          <Delete />
                        </IconButton>
                      </Tooltip>
                    )}
                  </ListItemSecondaryAction>
                </ListItem>
              </React.Fragment>
            ))}
          </List>

          {sessions.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
              No active sessions found.
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Revoke Single Session Dialog */}
      <Dialog
        open={revokeDialogOpen}
        onClose={() => !isRevoking && setRevokeDialogOpen(false)}
        aria-labelledby="revoke-dialog-title"
        aria-describedby="revoke-dialog-description"
      >
        <DialogTitle id="revoke-dialog-title">Revoke session?</DialogTitle>
        <DialogContent>
          <DialogContentText id="revoke-dialog-description">
            This will log out the device &quot;{sessionToRevoke?.deviceType}&quot;
            {sessionToRevoke?.location && ` in ${sessionToRevoke.location}`}.
            The user will need to log in again on that device.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRevokeDialogOpen(false)} disabled={isRevoking}>
            Cancel
          </Button>
          <Button
            onClick={handleRevokeSession}
            color="error"
            variant="contained"
            disabled={isRevoking}
          >
            {isRevoking ? <CircularProgress size={20} color="inherit" /> : 'Revoke'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Revoke All Sessions Dialog */}
      <Dialog
        open={revokeAllDialogOpen}
        onClose={() => !isRevoking && setRevokeAllDialogOpen(false)}
        aria-labelledby="revoke-all-dialog-title"
        aria-describedby="revoke-all-dialog-description"
      >
        <DialogTitle id="revoke-all-dialog-title">Logout all devices?</DialogTitle>
        <DialogContent>
          <DialogContentText id="revoke-all-dialog-description">
            This will log out all devices, including this one. You will need to
            log in again. Use this if you suspect unauthorized access to your account.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRevokeAllDialogOpen(false)} disabled={isRevoking}>
            Cancel
          </Button>
          <Button
            onClick={handleRevokeAllSessions}
            color="error"
            variant="contained"
            disabled={isRevoking}
          >
            {isRevoking ? <CircularProgress size={20} color="inherit" /> : 'Logout all'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default SessionManagement;
