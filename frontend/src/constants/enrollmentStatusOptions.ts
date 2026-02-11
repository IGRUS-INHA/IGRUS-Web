export const ENROLLMENT_STATUS_TITLE = '재학/휴학 여부';

export const enrollmentStatusOptions = [
  '재학',
  '휴학 (일반)',
  '휴학 (군)',
] as const;

export const enrollmentStatusToEnum: Record<string, string> = {
  '재학': 'ENROLLED',
  '휴학 (일반)': 'GENERAL_LEAVE',
  '휴학 (군)': 'MILITARY_LEAVE',
};
