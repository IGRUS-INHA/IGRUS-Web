import { Outlet } from 'react-router-dom';
import { useUIStore } from '@/stores';
import Sidebar from './Sidebar';
import Header from './Header';

export default function Layout() {
  const { sidebarOpen, setSidebarOpen } = useUIStore();

  return (
    <div className="min-h-screen bg-background text-foreground flex">
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content */}
      <div className="flex-1 min-w-0 flex flex-col">
        {/* Header */}
        <Header />

        {/* Page Content */}
        <main className="px-6 pb-6 flex-1">
          <div className="max-w-7xl mx-auto h-full">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
