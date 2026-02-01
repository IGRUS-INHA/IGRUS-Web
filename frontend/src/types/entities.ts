import type {
  Role,
  UserStatus,
  BoardType,
  EventStatus,
  RegistrationStatus,
  InquiryStatus,
  InquiryType,
} from './common';

// =============================================================================
// User Entity
// =============================================================================

export interface User {
  id?: string;
  studentId: string;
  name: string;
  email: string;
  role: Role;
  status?: UserStatus;
  joinedDate: string;
  profileImage?: string;
}

export interface UserProfile extends User {
  department?: string;
  grade?: number;
  phone?: string;
  bio?: string;
}

// =============================================================================
// Post Entity
// =============================================================================

export interface Author {
  id: string;
  name: string;
  profileImage?: string;
}

export interface Post {
  id: string;
  board: BoardType;
  category: string;
  title: string;
  content: string;
  author: string | Author;
  date: string;
  createdAt?: string;
  updatedAt?: string;
  likes: number;
  comments: number;
  views?: number;
  isLiked?: boolean;
  isBookmarked?: boolean;
  isAnonymous: boolean;
  isQuestion: boolean;
  image?: string;
  tag?: string;
  attachments?: Attachment[];
}

export interface PostDetail extends Post {
  authorId: string;
  commentList?: Comment[];
}

export interface Attachment {
  id: string;
  name: string;
  url: string;
  size: number;
  type: string;
}

// =============================================================================
// Comment Entity
// =============================================================================

export interface Comment {
  id: string;
  postId: string;
  content: string;
  author: string | Author;
  authorId: string;
  createdAt: string;
  updatedAt?: string;
  isAnonymous: boolean;
  parentId?: string;
  replies?: Comment[];
}

// =============================================================================
// Event Entity
// =============================================================================

export interface Event {
  id: string;
  title: string;
  description: string;
  date: string;
  startDate?: string;
  endDate?: string;
  location: string;
  image?: string;
  status: EventStatus;
  capacity?: number;
  maxCapacity?: number;
  currentCount?: number;
  attendees?: number;
  registrationDeadline?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface EventDetail extends Event {
  content?: string;
  organizer?: string;
  contactEmail?: string;
  registrations?: EventRegistration[];
  myRegistration?: EventRegistration | null;
}

export interface EventRegistration {
  id: string;
  eventId: string;
  userId: string;
  user?: User;
  status: RegistrationStatus;
  registeredAt: string;
  cancelledAt?: string;
}

// =============================================================================
// Inquiry Entity
// =============================================================================

export interface Inquiry {
  id: string;
  inquiryNumber: string;
  category: InquiryType;
  title: string;
  content: string;
  status: InquiryStatus;
  createdAt: string;
  answeredAt?: string;
  answer?: string;
  email?: string;
  name?: string;
}

export interface InquiryDetail extends Inquiry {
  replies?: InquiryReply[];
  memos?: InquiryMemo[];
}

export interface InquiryReply {
  id: string;
  inquiryId: string;
  content: string;
  createdAt: string;
  author: string;
}

export interface InquiryMemo {
  id: string;
  inquiryId: string;
  content: string;
  createdAt: string;
  author: string;
}
