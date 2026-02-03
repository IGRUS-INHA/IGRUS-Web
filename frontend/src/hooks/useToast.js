import { useUIStore } from '@/stores';

/**
 * 토스트 알림 훅
 *
 * 사용 예시:
 * const toast = useToast();
 * toast.success('저장되었습니다');
 * toast.error('오류가 발생했습니다');
 * toast.warning('주의가 필요합니다');
 */
export function useToast() {
  const addToast = useUIStore((state) => state.addToast);

  return {
    /**
     * 기본 토스트
     */
    show: (message, options = {}) => {
      addToast({ message, type: 'default', ...options });
    },

    /**
     * 성공 토스트
     */
    success: (message, options = {}) => {
      addToast({ message, type: 'success', ...options });
    },

    /**
     * 에러 토스트
     */
    error: (message, options = {}) => {
      addToast({ message, type: 'error', ...options });
    },

    /**
     * 경고 토스트
     */
    warning: (message, options = {}) => {
      addToast({ message, type: 'warning', ...options });
    },

    /**
     * 제목 + 메시지 토스트
     */
    custom: (toast) => {
      addToast(toast);
    },
  };
}
