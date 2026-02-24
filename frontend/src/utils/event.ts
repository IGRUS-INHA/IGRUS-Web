import type { Event, EventRegistration, User } from '@/types/entities';
import {
  EVENT_STATUS,
  REGISTRATION_STATUS,
  EVENT_POLICY,
  REGISTRATION_ERROR,
  CANCEL_ERROR,
  EVENT_LOCATIONS,
  REGISTRATION_PERIOD_PRESETS,
} from '@/constants/event';
import type { RegistrationPeriodPresetValue } from '@/constants/event';
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

/**
 * API location 문자열을 프리셋 + 상세로 분리
 * 긴 프리셋부터 매칭하여 "5호관(5동) 208호" → { preset: '5호관(5동)', detail: '208호' }
 */
export function parseLocation(location: string): { preset: string; detail: string } {
  if (!location) return { preset: '', detail: '' };

  // 긴 문자열부터 매칭 (예: "인하드림센터 2·3관"이 "인하드림센터"보다 먼저 매칭되도록)
  const sorted = [...EVENT_LOCATIONS].sort((a, b) => b.length - a.length);
  for (const loc of sorted) {
    if (location === loc) {
      return { preset: loc, detail: '' };
    }
    if (location.startsWith(`${loc} `)) {
      return { preset: loc, detail: location.slice(loc.length + 1).trim() };
    }
  }
  return { preset: '', detail: location };
}

/**
 * 프리셋 + 상세를 API location 문자열로 결합
 */
export function combineLocation(preset: string, detail: string): string {
  const trimmedDetail = detail.trim();
  if (!preset) return trimmedDetail;
  if (!trimmedDetail) return preset;
  return `${preset} ${trimmedDetail}`;
}

/**
 * 기존 신청 기간 날짜로부터 프리셋을 역산
 */
export function detectRegistrationPreset(
  eventDate: string,
  regStartDate: string,
  regStartTime: string,
  regEndDate: string,
  regEndTime: string,
  eventTime?: string,
): RegistrationPeriodPresetValue {
  if (!eventDate || !regStartDate || !regEndDate) return 'custom';

  const [ey, em, ed] = eventDate.split('-').map(Number);

  for (const preset of REGISTRATION_PERIOD_PRESETS) {
    if (preset.value === 'custom') continue;

    const expectedStart = new Date(ey ?? 0, (em ?? 1) - 1, (ed ?? 1) - preset.startDaysBefore);
    const expectedEnd = new Date(ey ?? 0, (em ?? 1) - 1, (ed ?? 1) - preset.endDaysBefore);

    const fmt = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    // endTimeOffsetHours가 있는 프리셋은 행사 시간 기준 동적 마감 시간
    let expectedEndTime: string = preset.endTime;
    if ('endTimeOffsetHours' in preset && eventTime) {
      const timeParts = eventTime.split(':').map(Number);
      const totalMinutes = Math.max(0, Math.min(23 * 60 + 59, (timeParts[0] ?? 0) * 60 + (timeParts[1] ?? 0) + preset.endTimeOffsetHours * 60));
      expectedEndTime = `${String(Math.floor(totalMinutes / 60)).padStart(2, '0')}:${String(totalMinutes % 60).padStart(2, '0')}`;
    }

    if (
      fmt(expectedStart) === regStartDate &&
      fmt(expectedEnd) === regEndDate &&
      preset.startTime === regStartTime &&
      expectedEndTime === regEndTime
    ) {
      return preset.value;
    }
  }

  return 'custom';
}

/**
 * YYYY-MM-DD 날짜 포맷 헬퍼 (timezone 이슈 방지)
 */
export function formatDateLocal(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * HH:MM 시간 포맷 헬퍼
 */
export function formatTimeLocal(d: Date): string {
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/**
 * YYYY-MM-DD + HH:MM 문자열을 Date로 파싱
 */
export function parseDateTimeString(dateStr: string, timeStr: string): Date | null {
  if (!dateStr) return null;
  const parts = dateStr.split('-').map(Number);
  const y = parts[0] ?? 0;
  const m = parts[1] ?? 1;
  const d = parts[2] ?? 1;
  if (timeStr) {
    const timeParts = timeStr.split(':').map(Number);
    const h = timeParts[0] ?? 0;
    const min = timeParts[1] ?? 0;
    return new Date(y, m - 1, d, h, min);
  }
  return new Date(y, m - 1, d);
}

/**
 * DatePicker onChange 값에서 단일 Date 추출
 */
export function extractDateFromPicker(value: Date | Date[] | null): Date | null {
  if (!value) return null;
  return Array.isArray(value) ? (value[0] ?? null) : value;
}
