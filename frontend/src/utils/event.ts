import type { Event, EventRegistration, User } from '@/types/entities';
import {
  EVENT_STATUS,
  REGISTRATION_STATUS,
  EVENT_POLICY,
  REGISTRATION_ERROR,
  CANCEL_ERROR,
} from '@/constants/event';
import { canRegisterEvent } from '@/constants/permissions';

interface RegistrationCheckResult {
  canRegister: boolean;
  error?: string;
  errorMessage?: string;
}

interface CancelCheckResult {
  canCancel: boolean;
  error?: string;
  deadline?: Date;
}

interface AvailabilityInfo {
  remaining: number | undefined;
  isFull: boolean;
  waitlistCount: number;
}

interface BadgeInfo {
  label: string;
  variant: string;
}

/**
 * 행사 신청 가능 여부 확인
 */
export function checkCanRegister(
  event: Event,
  user: User | undefined,
  registration?: EventRegistration
): RegistrationCheckResult {
  // 로그인 체크
  if (!user) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.NOT_AUTHENTICATED,
    };
  }

  // 권한 체크 (정회원 이상)
  if (!canRegisterEvent(user.role)) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.NO_PERMISSION,
    };
  }

  // 이미 신청한 경우 (취소된 건 제외)
  if (registration && registration.status !== REGISTRATION_STATUS.CANCELLED) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.ALREADY_REGISTERED,
    };
  }

  // 행사 상태 체크
  const now = new Date();
  const eventStart = new Date(event.startDate ?? event.date);
  const registrationDeadline = event.registrationDeadline
    ? new Date(event.registrationDeadline)
    : eventStart;

  if (event.status === EVENT_STATUS.CLOSED) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.EVENT_CLOSED,
    };
  }

  if (event.status === EVENT_STATUS.COMPLETED) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.EVENT_COMPLETED,
    };
  }

  if (now >= eventStart) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.EVENT_STARTED,
    };
  }

  if (now >= registrationDeadline) {
    return {
      canRegister: false,
      error: REGISTRATION_ERROR.REGISTRATION_CLOSED,
    };
  }

  return { canRegister: true };
}

/**
 * 대기열 여부 확인 (정원 초과 시)
 */
export function willBeWaitlisted(event: Event): boolean {
  if (!EVENT_POLICY.USE_WAITLIST) return false;
  if (!event.capacity && !event.maxCapacity) return false; // 정원 제한 없음

  const capacity = event.capacity ?? event.maxCapacity ?? 0;
  const currentCount = event.currentCount ?? event.attendees ?? 0;

  return currentCount >= capacity;
}

/**
 * 신청 취소 가능 여부 확인
 */
export function checkCanCancel(
  event: Event,
  registration: EventRegistration | undefined
): CancelCheckResult {
  // 신청 안 함
  if (!registration) {
    return {
      canCancel: false,
      error: CANCEL_ERROR.NOT_REGISTERED,
    };
  }

  // 이미 취소됨
  if (registration.status === REGISTRATION_STATUS.CANCELLED) {
    return {
      canCancel: false,
      error: CANCEL_ERROR.ALREADY_CANCELLED,
    };
  }

  const now = new Date();
  const eventStart = new Date(event.startDate ?? event.date);

  // 이미 시작됨
  if (now >= eventStart) {
    return {
      canCancel: false,
      error: CANCEL_ERROR.EVENT_STARTED,
    };
  }

  // 취소 기한 (행사 시작 48시간 전)
  const cancelDeadline = new Date(
    eventStart.getTime() - EVENT_POLICY.CANCEL_DEADLINE_MS
  );

  if (now >= cancelDeadline) {
    return {
      canCancel: false,
      error: CANCEL_ERROR.DEADLINE_PASSED,
      deadline: cancelDeadline,
    };
  }

  return {
    canCancel: true,
    deadline: cancelDeadline,
  };
}

/**
 * 취소 가능 기한 계산
 */
export function getCancelDeadline(eventStartDate: Date | string): Date {
  const start = new Date(eventStartDate);
  return new Date(start.getTime() - EVENT_POLICY.CANCEL_DEADLINE_MS);
}

/**
 * 남은 자리 계산
 */
export function getAvailability(event: Event): AvailabilityInfo {
  // 정원 제한 없음
  const capacity = event.capacity ?? event.maxCapacity;
  if (!capacity) {
    return {
      remaining: undefined,
      isFull: false,
      waitlistCount: 0,
    };
  }

  const currentCount = event.currentCount ?? event.attendees ?? 0;
  const remaining = Math.max(0, capacity - currentCount);
  const waitlistCount = Math.max(0, currentCount - capacity);

  return {
    remaining,
    isFull: remaining === 0,
    waitlistCount,
  };
}

/**
 * 신청 상태 뱃지 정보
 */
export function getRegistrationBadge(status: string): BadgeInfo {
  switch (status) {
    case REGISTRATION_STATUS.CONFIRMED:
      return { label: '신청 완료', variant: 'default' };
    case REGISTRATION_STATUS.WAITING:
      return { label: '대기중', variant: 'secondary' };
    case REGISTRATION_STATUS.CANCELLED:
      return { label: '취소됨', variant: 'outline' };
    default:
      return { label: status, variant: 'outline' };
  }
}

/**
 * 행사 상태 뱃지 정보
 */
export function getEventStatusBadge(event: Event): BadgeInfo {
  const { isFull } = getAvailability(event);

  if (event.status === EVENT_STATUS.CLOSED) {
    return { label: '마감', variant: 'destructive' };
  }
  if (event.status === EVENT_STATUS.COMPLETED) {
    return { label: '종료', variant: 'outline' };
  }
  if (event.status === EVENT_STATUS.ONGOING) {
    return { label: '진행중', variant: 'default' };
  }
  if (isFull) {
    return { label: '정원 마감', variant: 'secondary' };
  }
  return { label: '신청 가능', variant: 'default' };
}
