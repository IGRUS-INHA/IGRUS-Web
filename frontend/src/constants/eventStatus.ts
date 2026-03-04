// 공개 상태 (visibility)
export const VISIBILITY_BADGE: Record<string, string> = {
  PUBLISHED: "bg-success/10 text-success",
  UNPUBLISHED: "bg-muted text-muted-foreground",
};
export const VISIBILITY_LABEL: Record<string, string> = {
  PUBLISHED: "공개",
  UNPUBLISHED: "비공개",
};

// 행사 진행 상태 (eventStatus)
export const EVENT_STATUS_BADGE: Record<string, string> = {
  UPCOMING: "bg-primary/10 text-primary",
  ONGOING: "bg-success/10 text-success",
  COMPLETED: "bg-muted text-muted-foreground",
  CANCELED: "bg-destructive/10 text-destructive",
};
export const EVENT_STATUS_LABEL: Record<string, string> = {
  UPCOMING: "진행 예정",
  ONGOING: "진행중",
  COMPLETED: "완료",
  CANCELED: "취소됨",
};

// 모집 상태 (registrationStatus)
export const REG_STATUS_BADGE: Record<string, string> = {
  NOT_STARTED: "bg-primary/10 text-primary",
  OPEN: "bg-success/10 text-success",
  CLOSED: "bg-warning/10 text-warning",
};
export const REG_STATUS_LABEL: Record<string, string> = {
  NOT_STARTED: "모집 예정",
  OPEN: "모집중",
  CLOSED: "마감",
};
