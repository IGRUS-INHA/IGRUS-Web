import { useState, useEffect, useCallback } from "react";

interface UseCountdownOptions {
  /** 카운트다운 시작 시간 (초) */
  initialSeconds: number;
  /** 마운트 시 자동 시작 여부 (기본: false) */
  autoStart?: boolean;
  /** sessionStorage 키 — 지정 시 새로고침해도 타이머 유지 */
  persistKey?: string;
}

interface UseCountdownReturn {
  /** 남은 시간 (초) */
  remaining: number;
  /** 만료 여부 */
  isExpired: boolean;
  /** 실행 중 여부 */
  isRunning: boolean;
  /** "M:SS" 형식의 포맷된 시간 문자열 */
  formatted: string;
  /** 초기 시간으로 리셋 후 재시작 */
  restart: () => void;
  /** 타이머 시작 */
  start: () => void;
  /** 타이머 정지 */
  stop: () => void;
}

function getPersistedRemaining(key: string): number {
  const stored = sessionStorage.getItem(key);
  if (!stored) return -1;

  const endTime = Number(stored);
  const remaining = Math.floor((endTime - Date.now()) / 1000);

  return Math.max(remaining, 0);
}

function persistEndTime(key: string, seconds: number) {
  sessionStorage.setItem(key, String(Date.now() + seconds * 1000));
}

export function useCountdown({
  initialSeconds,
  autoStart = false,
  persistKey,
}: UseCountdownOptions): UseCountdownReturn {
  const [remaining, setRemaining] = useState(() => {
    if (persistKey) {
      const persisted = getPersistedRemaining(persistKey);
      if (persisted >= 0) return persisted;
    }
    if (autoStart) {
      if (persistKey) persistEndTime(persistKey, initialSeconds);
      return initialSeconds;
    }
    return 0;
  });

  const [isRunning, setIsRunning] = useState(() => {
    if (persistKey) {
      const persisted = getPersistedRemaining(persistKey);
      return persisted > 0;
    }
    return autoStart;
  });

  useEffect(() => {
    if (!isRunning || remaining <= 0) {
      if (remaining <= 0 && isRunning) setIsRunning(false);
      return;
    }

    const timer = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          setIsRunning(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isRunning, remaining, persistKey]);

  const formatted = `${Math.floor(remaining / 60)}:${(remaining % 60).toString().padStart(2, "0")}`;

  const restart = useCallback(() => {
    if (persistKey) persistEndTime(persistKey, initialSeconds);
    setRemaining(initialSeconds);
    setIsRunning(true);
  }, [initialSeconds, persistKey]);

  const start = useCallback(() => {
    if (remaining > 0) setIsRunning(true);
  }, [remaining]);

  const stop = useCallback(() => {
    setIsRunning(false);
  }, []);

  return {
    remaining,
    isExpired: remaining <= 0,
    isRunning,
    formatted,
    restart,
    start,
    stop,
  };
}
