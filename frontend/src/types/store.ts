import type { ReactNode } from 'react';
import type { User } from './entities';
import type { Theme, Toast, ToastInput } from './common';

// =============================================================================
// Auth Store Types
// =============================================================================

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

export interface AuthActions {
  login: (studentId: string, password: string) => Promise<void>;
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  updateUser: (userData: Partial<User>) => void;
  logout: () => void;
  isAssociate: () => boolean;
  isMember: () => boolean;
  isOperator: () => boolean;
  isAdmin: () => boolean;
  hasMinRole: (minRole: string) => boolean;
}

export type AuthStore = AuthState & AuthActions;

// =============================================================================
// UI Store Types
// =============================================================================

export interface UIState {
  sidebarOpen: boolean;
  modalOpen: boolean;
  modalContent: ReactNode | null;
  toasts: Toast[];
  theme: Theme;
}

export interface UIActions {
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  openModal: (content: ReactNode) => void;
  closeModal: () => void;
  addToast: (toast: ToastInput) => void;
  removeToast: (id: number) => void;
  toggleTheme: () => void;
  setTheme: (theme: Theme) => void;
}

export type UIStore = UIState & UIActions;

// =============================================================================
// Zustand Persist Types
// =============================================================================

export interface AuthPersistState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

export interface UIPersistState {
  theme: Theme;
}
