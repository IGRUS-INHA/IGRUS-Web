import { useEffect } from "react";

/**
 * 배경 스크롤 잠금 (모바일 크롬/사파리 안전).
 * body overflow:hidden 만으로는 실제 스크롤러가 <html> 인 모바일에서 잠기지 않고,
 * 주소창 표시/숨김으로 뷰포트가 바뀌면 스크롤 위치가 깨진다.
 * → body 를 position:fixed 로 고정하고 닫을 때 원래 위치로 복원한다.
 */
export function useScrollLock(active: boolean) {
  useEffect(() => {
    if (!active) return;
    const scrollY = window.scrollY;
    const { style } = document.body;
    style.position = "fixed";
    style.top = `-${scrollY}px`;
    style.left = "0";
    style.right = "0";
    style.width = "100%";
    return () => {
      style.position = "";
      style.top = "";
      style.left = "";
      style.right = "";
      style.width = "";
      window.scrollTo(0, scrollY);
    };
  }, [active]);
}
