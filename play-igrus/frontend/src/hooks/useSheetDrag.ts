import { useEffect, type RefObject } from "react";

/**
 * 모바일 바텀시트를 손가락으로 아래로 끌어내려 닫는 제스처.
 *
 * 앱 목록 등 내부 스크롤과 충돌하지 않도록, 안쪽 스크롤 영역([data-sheet-scroll])이
 * 맨 위에 있고 + 아래로 당기는 수직 제스처일 때만 드래그로 확정한다.
 * 그 외(목록 스크롤·위로 당김·가로 스와이프)는 네이티브 스크롤에 맡긴다.
 *
 * @param sheetRef translateY 로 움직일 시트 요소(<dialog>)
 * @param onClose  임계치 이상 끌어내렸을 때 호출
 * @param open     시트가 열려 있는 동안만 true (열릴 때마다 리스너 재부착)
 */
export function useSheetDrag(
  sheetRef: RefObject<HTMLDialogElement | null>,
  onClose: () => void,
  open: boolean,
) {
  useEffect(() => {
    const sheet = sheetRef.current;
    if (!sheet || !open) return;
    if (!window.matchMedia("(max-width: 639px)").matches) return; // 모바일 전용

    let startX = 0;
    let startY = 0;
    let dy = 0;
    let startedInScroller = false; // 터치가 스크롤 앱 영역에서 시작됐는지
    let dragging = false; // 이번 터치가 시트 드래그로 확정됐는지

    const onStart = (e: TouchEvent) => {
      // 스크롤 영역은 시트가 늦게 채워질 수 있어(로딩) 터치 시작마다 다시 찾는다
      const scroller = sheet.querySelector<HTMLElement>("[data-sheet-scroll]");
      startedInScroller = !!scroller && scroller.contains(e.target as Node);
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      dy = 0;
      dragging = false;
    };

    const onMove = (e: TouchEvent) => {
      const t = e.touches[0];
      if (!dragging) {
        const dyRaw = t.clientY - startY;
        const dxRaw = t.clientX - startX;
        // 아래로(6px 데드존) + 세로 우세 + 스크롤 앱 영역 밖(헤더·배너·손잡이)에서 시작했을 때만 확정.
        // 앱 목록 영역에서 시작한 터치는 스크롤 여부와 무관하게 시트로 넘어가지 않는다.
        if (dyRaw > 6 && Math.abs(dyRaw) > Math.abs(dxRaw) && !startedInScroller) {
          dragging = true;
          startY = t.clientY; // 재기준점 → dy 0부터 부드럽게
        } else {
          return; // 네이티브 스크롤에 맡김
        }
      }
      dy = Math.max(0, t.clientY - startY);
      e.preventDefault(); // 확정 후엔 배경/내부 스크롤 차단
      sheet.style.transition = "none";
      sheet.style.transform = `translateY(${dy}px)`;
    };

    const onEnd = () => {
      if (!dragging) return;
      dragging = false;
      sheet.style.transition = "transform 0.25s cubic-bezier(0.32, 0.72, 0, 1)";
      if (dy > 120) {
        sheet.style.transform = "translateY(100%)";
        onClose();
      } else {
        sheet.style.transform = "translateY(0)";
      }
    };

    sheet.addEventListener("touchstart", onStart, { passive: true });
    sheet.addEventListener("touchmove", onMove, { passive: false });
    sheet.addEventListener("touchend", onEnd);
    sheet.addEventListener("touchcancel", onEnd);
    return () => {
      sheet.removeEventListener("touchstart", onStart);
      sheet.removeEventListener("touchmove", onMove);
      sheet.removeEventListener("touchend", onEnd);
      sheet.removeEventListener("touchcancel", onEnd);
      sheet.style.transform = "";
      sheet.style.transition = "";
    };
  }, [sheetRef, onClose, open]);
}
