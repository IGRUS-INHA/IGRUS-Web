// =============================================================================
// Role & Permission Types
// =============================================================================

/** User roles in hierarchy order (lowest to highest) */
export const ROLES = {
  ASSOCIATE: 'ASSOCIATE',
  MEMBER: 'MEMBER',
  OPERATOR: 'OPERATOR',
  ADMIN: 'ADMIN',
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];
export type RoleOrNull = Role | null;

/** Role hierarchy for permission checking */
export const ROLE_HIERARCHY: readonly RoleOrNull[] = [
  null,
  'ASSOCIATE',
  'MEMBER',
  'OPERATOR',
  'ADMIN',
] as const;

// =============================================================================
// Board Types
// =============================================================================

export const BOARDS = {
  NOTICES: 'notices',
  GENERAL: 'general',
  INSIGHT: 'insight',
} as const;

export type BoardType = (typeof BOARDS)[keyof typeof BOARDS];

// =============================================================================
// Status Types
// =============================================================================

export const USER_STATUS = {
  ACTIVE: 'ACTIVE',
  SUSPENDED: 'SUSPENDED',
  WITHDRAWN: 'WITHDRAWN',
} as const;

export type UserStatus = (typeof USER_STATUS)[keyof typeof USER_STATUS];

export const EVENT_STATUS = {
  UPCOMING: 'UPCOMING',
  ONGOING: 'ONGOING',
  COMPLETED: 'COMPLETED',
  CLOSED: 'CLOSED',
} as const;

export type EventStatus = (typeof EVENT_STATUS)[keyof typeof EVENT_STATUS];

export const REGISTRATION_STATUS = {
  CONFIRMED: 'CONFIRMED',
  WAITING: 'WAITING',
  CANCELLED: 'CANCELLED',
} as const;

export type RegistrationStatus =
  (typeof REGISTRATION_STATUS)[keyof typeof REGISTRATION_STATUS];

export const INQUIRY_STATUS = {
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  ANSWERED: 'ANSWERED',
} as const;

export type InquiryStatus =
  (typeof INQUIRY_STATUS)[keyof typeof INQUIRY_STATUS];

export const INQUIRY_TYPE = {
  JOIN: 'JOIN',
  EVENT: 'EVENT',
  REPORT: 'REPORT',
  ACCOUNT: 'ACCOUNT',
  TECHNICAL: 'TECHNICAL',
  GENERAL: 'GENERAL',
  OTHER: 'OTHER',
} as const;

export type InquiryType = (typeof INQUIRY_TYPE)[keyof typeof INQUIRY_TYPE];

// =============================================================================
// Pagination & Filtering
// =============================================================================

export interface PaginationParams {
  page?: number;
  size?: number;
}

export interface PaginationInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export const SEARCH_TYPE = {
  TITLE: 'title',
  CONTENT: 'content',
  TITLE_CONTENT: 'title_content',
} as const;

export type SearchType = (typeof SEARCH_TYPE)[keyof typeof SEARCH_TYPE];

export const SORT_TYPE = {
  LATEST: 'latest',
  POPULAR: 'popular',
} as const;

export type SortType = (typeof SORT_TYPE)[keyof typeof SORT_TYPE];

export interface SearchParams extends PaginationParams {
  keyword?: string;
  searchType?: SearchType;
  sortType?: SortType;
  category?: string;
}

// =============================================================================
// Theme
// =============================================================================

export const THEME = {
  LIGHT: 'light',
  DARK: 'dark',
} as const;

export type Theme = (typeof THEME)[keyof typeof THEME];

// =============================================================================
// Toast
// =============================================================================

export const TOAST_TYPE = {
  DEFAULT: 'default',
  SUCCESS: 'success',
  ERROR: 'error',
  WARNING: 'warning',
} as const;

export type ToastType = (typeof TOAST_TYPE)[keyof typeof TOAST_TYPE];

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
  title?: string;
  duration?: number;
}

export type ToastInput = Omit<Toast, 'id'>;
