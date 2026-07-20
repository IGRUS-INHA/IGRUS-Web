import { useEffect, type RefObject } from "react";

/**
 * 모바일 바텀시트를 손가락으로 아래로 끌어내려 닫는 제스처.
 *
 * 안쪽 스크롤 영역([data-sheet-scroll])과 충돌하지 않도록:
 * - 스크롤 가능한 목록에서 스크롤이 시작되면(scrollTop>0) 그 제스처는 네이티브 전용 →
 *   시트는 안 내려가고 목록만 스크롤된다.
 * - 목록이 맨 위이거나 요소가 적어 스크롤 불가하면, 아래로 당김을 시트 드래그로 처리한다.
 *   기준점(anchor)을 최상단 위치로 따라가므로 위로 갔다가 아래로 반전해도 그 지점부터 내려간다.
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

    let startY = 0; // 드래그 확정 후 translateY 기준점(아래로 당기기 시작 지점)
    let anchorX = 0; // 아래로 당기기 기준점 X (반환점마다 갱신)
    let anchorY = 0; // 아래로 당기기 기준점 Y
    let dy = 0;
    let scroller: HTMLElement | null = null; // 터치가 시작된 시점의 스크롤 앱 영역
    let startedInScroller = false; // 터치가 스크롤 앱 영역에서 시작됐는지
    let canScroll = false; // 그 영역이 실제로 스크롤 가능한지(내용이 넘치는지)
    let nativeLock = false; // 이번 터치가 네이티브 스크롤 전용으로 확정됐는지
    let dragging = false; // 이번 터치가 시트 드래그로 확정됐는지

    const onStart = (e: TouchEvent) => {
      // 스크롤 영역은 시트가 늦게 채워질 수 있어(로딩) 터치 시작마다 다시 찾는다
      scroller = sheet.querySelector<HTMLElement>("[data-sheet-scroll]");
      startedInScroller = !!scroller && scroller.contains(e.target as Node);
      canScroll = !!scroller && scroller.scrollHeight - scroller.clientHeight > 1;
      anchorX = e.touches[0].clientX;
      anchorY = e.touches[0].clientY;
      dy = 0;
      nativeLock = false;
      dragging = false;
    };

    const onMove = (e: TouchEvent) => {
      const t = e.touches[0];
      if (!dragging) {
        // 스크롤 가능한 영역에서 실제 스크롤이 시작되면(scrollTop>0) 이번 제스처는
        // 끝까지 네이티브 스크롤 전용 → 시트는 안 내려가고 목록만 스크롤된다.
        if (startedInScroller && canScroll) {
          if (scroller && scroller.scrollTop > 0) nativeLock = true;
          if (nativeLock) {
            anchorX = t.clientX; // 맨 위로 되돌아온 뒤 당김에 대비해 기준점만 따라감
            anchorY = t.clientY;
            return;
          }
          // 여기부터는 스크롤 가능하지만 아직 맨 위(scrollTop 0)이고 스크롤이 시작되지 않은 상태
        }
        // 손가락이 기준점보다 위로 올라가면 기준점을 갱신한다.
        // → 위로 스크롤/이동했다가 방향을 바꿔 아래로 당기면 그 반환점부터 시트가 내려간다.
        if (t.clientY < anchorY) {
          anchorX = t.clientX;
          anchorY = t.clientY;
          return;
        }
        const pull = t.clientY - anchorY; // 기준점 대비 아래로 당긴 양
        const dxRaw = t.clientX - anchorX;
        // 아래로(6px 데드존) + 세로 우세(수직 기준 약 60°까지 허용)일 때 시트 드래그 확정.
        if (pull > 6 && pull * 2 > Math.abs(dxRaw)) {
          dragging = true;
          startY = anchorY; // 반환점을 translateY 기준으로 → dy 0부터 부드럽게
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
