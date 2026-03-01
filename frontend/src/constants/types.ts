// Menu sections
export const MENU_SECTIONS = {
  HOME: "home",
  COMMUNITY: "community",
  EVENTS: "events",
  SUPPORT: "support",
  PROFILE: "profile",
  ADMIN: "admin",
  AUTH: "auth",
} as const;

// Theme modes
export const THEME_MODES = {
  DARK: "dark",
  LIGHT: "light",
} as const;

// Account statuses
export const ACCOUNT_STATUSES = {
  ACTIVE: "Active",
  INACTIVE: "Inactive",
  SUSPENDED: "Suspended",
  WITHDRAWN: "Withdrawn",
} as const;

// Board types
export const BOARD_TYPES = {
  NOTICES: "notices",
  GENERAL: "general",
  INSIGHT: "insight",
} as const;

// Event statuses
export const EVENT_STATUSES = {
  OPEN: "Open",
  CLOSED: "Closed",
  FULL: "Full",
} as const;

// Inquiry statuses
export const INQUIRY_STATUSES = {
  RECEIVED: "Received",
  IN_PROGRESS: "In Progress",
  COMPLETED: "Completed",
} as const;

// User roles
export const USER_ROLES = {
  MEMBER: "member",
  ADMIN: "admin",
} as const;
