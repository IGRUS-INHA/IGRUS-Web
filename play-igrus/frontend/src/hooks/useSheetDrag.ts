import { useEffect, type RefObject } from "react";

/**
 * 모바일 바텀시트를 손가락으로 아래로 끌어내려 닫는 제스처.
 *
 * 안쪽 스크롤 영역([data-sheet-scroll])과 충돌하지 않도록:
 * - 그 영역이 "스크롤 가능"(내용이 넘침, scrollHeight>clientHeight)하면, 거기서 시작한
 *   터치는 통째로 네이티브 스크롤에 맡긴다 → 시트는 고정, 목록만 스크롤.
 *   (현재 scrollTop 위치가 아니라 스크롤 가능 여부로 판단한다.)
 * - 스크롤 불가(요소가 적음)하거나 손잡이·헤더 등에서 시작하면 시트 드래그로 처리한다.
 *
 * dy 는 pointer-down 이후 매 move 의 상대 이동을 누적한다. 아래로 가면 늘고 위로 가면
 * 0 까지 줄어들므로, 위로 갔다가 아래로 반전해도 그 지점부터 자연스럽게 내려간다.
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

    let lastY = 0; // 직전 move 의 y — 상대 이동 계산용
    let dy = 0; // 시트가 내려간 누적 거리(>=0)
    let scrollable = false; // 이번 터치가 스크롤 가능한 영역에서 시작했는지
    let dragging = false; // 이번 터치가 시트 드래그로 확정됐는지

    const onStart = (e: TouchEvent) => {
      // 스크롤 영역은 시트가 늦게 채워질 수 있어(로딩) 터치 시작마다 다시 찾는다
      const scroller = sheet.querySelector<HTMLElement>("[data-sheet-scroll]");
      scrollable =
        !!scroller &&
        scroller.contains(e.target as Node) &&
        scroller.scrollHeight - scroller.clientHeight > 1;
      lastY = e.touches[0].clientY;
      dy = 0;
      dragging = false;
    };

    const onMove = (e: TouchEvent) => {
      if (scrollable) return; // 스크롤 가능 목록 → 네이티브 스크롤에 맡김(시트 고정)
      const t = e.touches[0];
      // 직전 위치 대비 상대 이동을 누적: 아래로 가면 늘고, 위로 가면 0까지 준다.
      dy = Math.max(0, dy + (t.clientY - lastY));
      lastY = t.clientY;
      if (!dragging) {
        if (dy < 6) return; // 미세 이동(탭 등) 무시
        dragging = true;
      }
      e.preventDefault(); // 확정 후엔 배경/페이지 스크롤 차단(pull-to-refresh 방지)
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
