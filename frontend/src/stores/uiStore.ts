import type { ReactNode } from 'react';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UIStore, UIPersistState } from '@/types/store';
import type { Toast, ToastInput, Theme } from '@/types/common';
import { THEME } from '@/types/common';

export const useUIStore = create<UIStore>()(
  persist(
    (set) => ({
      // 사이드바
      sidebarOpen: false,
      toggleSidebar: () => {
        set((state) => ({ sidebarOpen: !state.sidebarOpen }));
      },
      setSidebarOpen: (open: boolean) => {
        set({ sidebarOpen: open });
      },

      // 모달
      modalOpen: false,
      modalContent: null,
      openModal: (content: ReactNode) => {
        set({ modalOpen: true, modalContent: content });
      },
      closeModal: () => {
        set({ modalOpen: false, modalContent: null });
      },

      // 토스트 (알림 메시지)
      toasts: [],
      addToast: (toast: ToastInput) => {
        set((state) => ({
          toasts: [...state.toasts, { id: Date.now(), ...toast } as Toast],
        }));
      },
      removeToast: (id: number) => {
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        }));
      },

      // 테마
      theme: THEME.LIGHT,
      toggleTheme: () => {
        set((state) => {
          const newTheme =
            state.theme === THEME.LIGHT ? THEME.DARK : THEME.LIGHT;
          if (newTheme === THEME.DARK) {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
          return { theme: newTheme };
        });
      },
      setTheme: (theme: Theme) => {
        if (theme === THEME.DARK) {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
        set({ theme });
      },
    }),
    {
      name: 'ui-storage',
      partialize: (state): UIPersistState => ({ theme: state.theme }),
      onRehydrateStorage: () => (state) => {
        if (state?.theme === THEME.DARK) {
          document.documentElement.classList.add('dark');
        }
      },
    }
  )
);
