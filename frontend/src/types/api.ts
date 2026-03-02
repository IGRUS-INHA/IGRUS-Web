import type {
  User,
  Post,
  PostDetail,
  Event,
  EventDetail,
  Inquiry,
} from "./entities";
import type {
  Role,
  BoardType,
  PaginationInfo,
  SearchParams,
  InquiryType,
  EventStatus,
} from "./common";

// =============================================================================
// Fetch Config Types
// =============================================================================

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface FetchConfig {
  url: string;
  method: HttpMethod;
  params?: Record<string, unknown>;
  data?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

// =============================================================================
// Generic API Response Types
// =============================================================================

export interface ApiResponse<T> {
  data: T;
  message?: string;
  code?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pagination: PaginationInfo;
}

export interface ApiError {
  code: string;
  message: string;
  errors?: Record<string, string>;
  status?: number;
}

// =============================================================================
// Auth API Types
// =============================================================================

export interface LoginRequest {
  studentId: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  code?: string;
  recoverable?: boolean;
}

export interface SignupRequest {
  studentId: string;
  email: string;
  password: string;
  name: string;
  department?: string;
}

export interface SignupResponse {
  message: string;
  email: string;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
}

export interface RecoverAccountRequest {
  studentId: string;
  password: string;
}

// =============================================================================
// Posts API Types
// =============================================================================

export interface PostListParams extends SearchParams {
  board?: BoardType;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  category: string;
  isAnonymous?: boolean;
  isQuestion?: boolean;
  attachments?: File[];
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
  category?: string;
  isAnonymous?: boolean;
  isQuestion?: boolean;
}

export interface CreateCommentRequest {
  content: string;
  isAnonymous?: boolean;
  parentId?: string;
}

// =============================================================================
// Events API Types
// =============================================================================

export interface EventListParams extends SearchParams {
  status?: EventStatus;
  upcoming?: boolean;
}

export interface CreateEventRequest {
  title: string;
  description: string;
  content?: string;
  startDate: string;
  endDate?: string;
  location: string;
  capacity?: number;
  registrationDeadline?: string;
  image?: File;
}

export interface UpdateEventRequest {
  title?: string;
  description?: string;
  content?: string;
  startDate?: string;
  endDate?: string;
  location?: string;
  capacity?: number;
  registrationDeadline?: string;
  image?: File;
}

// =============================================================================
// Users API Types
// =============================================================================

export interface UpdateProfileRequest {
  name?: string;
  email?: string;
  phone?: string;
  bio?: string;
  profileImage?: File;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface WithdrawRequest {
  password: string;
  reason?: string;
}

// =============================================================================
// Inquiries API Types
// =============================================================================

export interface CreateInquiryRequest {
  category: InquiryType;
  title: string;
  content: string;
  email?: string;
  name?: string;
}

export interface InquiryLookupRequest {
  inquiryNumber: string;
  email: string;
}

export interface InquiryReplyRequest {
  content: string;
}

// =============================================================================
// Admin API Types
// =============================================================================

export interface AdminDashboardData {
  totalUsers: number;
  pendingAssociates: number;
  todayPosts: number;
  todayComments: number;
  upcomingEvents: number;
  pendingInquiries: number;
  recentActivity: AdminActivity[];
}

export interface AdminActivity {
  id: string;
  type: string;
  description: string;
  createdAt: string;
  userId?: string;
  userName?: string;
}

export interface AdminUserListParams extends SearchParams {
  role?: Role;
  status?: string;
}

export interface UpdateUserRoleRequest {
  role: Role;
}

export interface UpdateUserStatusRequest {
  status: string;
  reason?: string;
}

export interface UploadMembersRequest {
  file: File;
  semester: string;
}

// =============================================================================
// Re-export entity types for convenience
// =============================================================================

export type { User, Post, PostDetail, Event, EventDetail, Inquiry };
