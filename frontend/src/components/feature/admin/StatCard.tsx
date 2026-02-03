import { Card } from '@/components/ui/card';
import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: ReactNode;
  colorClass?: string;
}

export default function StatCard({ label, value, icon, colorClass = 'text-primary' }: StatCardProps) {
  return (
    <Card className="p-s6 rounded-[2.5rem] border bg-card border-border shadow-sm">
      <div className={`${colorClass} mb-s4`}>{icon}</div>
      <p className="text-muted-foreground text-c1 font-bold uppercase tracking-widest mb-1">
        {label}
      </p>
      <h3 className="text-h2">{value}</h3>
    </Card>
  );
}
