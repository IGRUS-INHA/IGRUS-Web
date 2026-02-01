import { Search } from 'lucide-react';

export default function SearchBar({ className = '' }) {
  return (
    <div className={`relative group ${className}`}>
      <Search
        className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground group-focus-within:text-primary transition"
        size={18}
      />
      <input
        type="text"
        placeholder="Search..."
        className="border border-border rounded-full pl-10 pr-4 py-2 text-sm w-40 lg:w-64
                   bg-background text-foreground focus:outline-none focus:border-primary/50
                   transition-all focus:lg:w-80"
      />
    </div>
  );
}
