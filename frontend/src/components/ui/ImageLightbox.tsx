import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X, ChevronLeft, ChevronRight } from "lucide-react";

interface ImageLightboxProps {
  images: string[];
  initialIndex: number;
  onClose: () => void;
}

export function ImageLightbox({
  images,
  initialIndex,
  onClose,
}: ImageLightboxProps) {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);

  useEffect(() => {
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, []);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowLeft") setCurrentIndex((i) => Math.max(0, i - 1));
      if (e.key === "ArrowRight")
        setCurrentIndex((i) => Math.min(images.length - 1, i + 1));
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [images.length, onClose]);

  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < images.length - 1;
  const showNav = images.length > 1;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80"
      onClick={onClose}
    >
      {/* 닫기 버튼 */}
      <button
        type="button"
        onClick={onClose}
        className="absolute top-s4 right-s4 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer transition"
      >
        <X size={20} />
      </button>

      {/* 이전 버튼 */}
      {showNav && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setCurrentIndex((i) => Math.max(0, i - 1));
          }}
          disabled={!hasPrev}
          className="absolute left-s4 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer transition disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <ChevronLeft size={24} />
        </button>
      )}

      {/* 이미지 */}
      <img
        src={images[currentIndex]}
        alt={`이미지 ${currentIndex + 1}`}
        className="max-w-[90vw] max-h-[90vh] object-contain rounded-r3 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      />

      {/* 다음 버튼 */}
      {showNav && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            setCurrentIndex((i) => Math.min(images.length - 1, i + 1));
          }}
          disabled={!hasNext}
          className="absolute right-s4 w-10 h-10 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center cursor-pointer transition disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <ChevronRight size={24} />
        </button>
      )}

      {/* 인덱스 표시 */}
      {showNav && (
        <div className="absolute bottom-s4 left-1/2 -translate-x-1/2 text-white/70 text-sm">
          {currentIndex + 1} / {images.length}
        </div>
      )}
    </div>,
    document.body,
  );
}
