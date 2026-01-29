import {
  EVENT_STATUS,
  REGISTRATION_STATUS,
  EVENT_POLICY,
  REGISTRATION_ERROR,
  CANCEL_ERROR,
} from '@/constants/event';
import { canRegisterEvent } from '@/constants/permissions';

/**
 * 행사 신청 가능 여부 확인
 * @param {Object} event - 행사 정보
 * @param {Object} user - 사용자 정보 (null이면 비로그인)
 * @param {Object|null} registration - 현재 신청 정보 (없으면 null)
 * @returns {{ canRegister: boolean, error?: string, errorMessage?: string }}
 */
export function checkCanRegister(event, user, registration = null) {
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
  const eventStart = new Date(event.startDate);
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
 * @param {Object} event - 행사 정보
 * @returns {boolean} 대기열 등록인지 여부
 */
export function willBeWaitlisted(event) {
  if (!EVENT_POLICY.USE_WAITLIST) return false;
  if (!event.capacity) return false; // 정원 제한 없음

  return event.currentCount >= event.capacity;
}

/**
 * 신청 취소 가능 여부 확인
 * @param {Object} event - 행사 정보
 * @param {Object} registration - 신청 정보
 * @returns {{ canCancel: boolean, error?: string, deadline?: Date }}
 */
export function checkCanCancel(event, registration) {
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
  const eventStart = new Date(event.startDate);

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
 * @param {Date|string} eventStartDate - 행사 시작 일시
 * @returns {Date} 취소 가능 마감 시간
 */
export function getCancelDeadline(eventStartDate) {
  const start = new Date(eventStartDate);
  return new Date(start.getTime() - EVENT_POLICY.CANCEL_DEADLINE_MS);
}

/**
 * 남은 자리 계산
 * @param {Object} event - 행사 정보
 * @returns {{ remaining: number|null, isFull: boolean, waitlistCount: number }}
 */
export function getAvailability(event) {
  // 정원 제한 없음
  if (!event.capacity) {
    return {
      remaining: null,
      isFull: false,
      waitlistCount: 0,
    };
  }

  const remaining = Math.max(0, event.capacity - event.currentCount);
  const waitlistCount = Math.max(0, event.currentCount - event.capacity);

  return {
    remaining,
    isFull: remaining === 0,
    waitlistCount,
  };
}

/**
 * 신청 상태 뱃지 정보
 * @param {string} status - 신청 상태
 * @returns {{ label: string, variant: string }}
 */
export function getRegistrationBadge(status) {
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
 * @param {Object} event - 행사 정보
 * @returns {{ label: string, variant: string }}
 */
export function getEventStatusBadge(event) {
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
