import { Search } from 'lucide-react';

interface SearchBarProps {
  className?: string;
}

export default function SearchBar({ className = '' }: SearchBarProps) {
  return (
    <div className={`relative group ${className}`}>
      <Search
        className="absolute left-s3 top-1/2 -translate-y-1/2 text-muted-foreground group-focus-within:text-primary transition"
        size={18}
      />
      <input
        type="text"
        placeholder="Search..."
        className="border border-border rounded-full pl-s7 pr-s4 py-s2 text-sm
                   w-56 md:w-72 lg:w-96 xl:w-[500px]
                   bg-background text-foreground focus:outline-none focus:border-primary/50
                   transition-all focus:xl:w-[600px]"
      />
    </div>
  );
}
