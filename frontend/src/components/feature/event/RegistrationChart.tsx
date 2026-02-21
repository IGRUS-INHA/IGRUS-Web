import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts';
import type { PieLabelRenderProps } from 'recharts';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import type { ChartDataItem } from '@/utils/chart';

interface RegistrationChartProps {
  title: string;
  totalCount: number;
  data: ChartDataItem[];
  type: 'pie' | 'bar';
}

const RADIAN = Math.PI / 180;

function PieLabel(props: PieLabelRenderProps) {
  const { cx, cy, midAngle, innerRadius, outerRadius, percent } = props;
  if (typeof cx !== 'number' || typeof cy !== 'number' || typeof midAngle !== 'number'
    || typeof innerRadius !== 'number' || typeof outerRadius !== 'number' || typeof percent !== 'number') return null;
  if (percent < 0.05) return null;
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={12} fontWeight={600}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
}

function ChartLegend({ data, totalCount }: { data: ChartDataItem[]; totalCount: number }) {
  return (
    <div className="flex flex-col gap-s2">
      {data.map((entry) => (
        <div key={entry.name} className="flex items-center gap-s2">
          <div
            className="w-3 h-3 rounded-full flex-shrink-0"
            style={{ backgroundColor: entry.color }}
          />
          <span className="typo-b2 text-foreground truncate">{entry.name}</span>
          <span className="typo-c1 text-muted-foreground ml-auto">
            {entry.value}명 ({Math.round((entry.value / totalCount) * 100)}%)
          </span>
        </div>
      ))}
    </div>
  );
}

function PieChartContent({ data, totalCount }: { data: ChartDataItem[]; totalCount: number }) {
  return (
    <div className="flex flex-col sm:flex-row items-center gap-s5">
      <div className="w-44 h-44 flex-shrink-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              outerRadius={72}
              dataKey="value"
              label={PieLabel}
              labelLine={false}
            >
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              formatter={(value) => [`${value}명`, '']}
              contentStyle={{
                borderRadius: '8px',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-card)',
                color: 'var(--color-card-foreground)',
              }}
            />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <ChartLegend data={data} totalCount={totalCount} />
    </div>
  );
}

function BarChartContent({ data, totalCount }: { data: ChartDataItem[]; totalCount: number }) {
  const isHorizontal = data.length > 4;
  const chartHeight = isHorizontal ? Math.max(160, data.length * 36) : 180;

  if (isHorizontal) {
    return (
      <div style={{ height: chartHeight }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ left: 0, right: 24, top: 4, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" horizontal={false} />
            <XAxis type="number" tick={{ fontSize: 11, fill: 'var(--color-muted-foreground)' }} />
            <YAxis
              type="category"
              dataKey="name"
              width={90}
              tick={{ fontSize: 11, fill: 'var(--color-foreground)' }}
            />
            <Tooltip
              formatter={(value) => [`${value}명 (${Math.round((Number(value) / totalCount) * 100)}%)`, '']}
              contentStyle={{
                borderRadius: '8px',
                border: '1px solid var(--color-border)',
                backgroundColor: 'var(--color-card)',
                color: 'var(--color-card-foreground)',
              }}
            />
            <Bar dataKey="value" radius={[0, 4, 4, 0]}>
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    );
  }

  return (
    <div style={{ height: chartHeight }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ left: -16, right: 8, top: 4, bottom: 4 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
          <XAxis dataKey="name" tick={{ fontSize: 11, fill: 'var(--color-foreground)' }} />
          <YAxis tick={{ fontSize: 11, fill: 'var(--color-muted-foreground)' }} allowDecimals={false} />
          <Tooltip
            formatter={(value) => [`${value}명 (${Math.round((Number(value) / totalCount) * 100)}%)`, '']}
            contentStyle={{
              borderRadius: '8px',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-card)',
              color: 'var(--color-card-foreground)',
            }}
          />
          <Bar dataKey="value" radius={[4, 4, 0, 0]}>
            {data.map((entry) => (
              <Cell key={entry.name} fill={entry.color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export default function RegistrationChart({ title, totalCount, data, type }: RegistrationChartProps) {
  if (data.length === 0) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="typo-b1 font-bold">{title}</CardTitle>
        <CardDescription>응답 {totalCount}개</CardDescription>
      </CardHeader>
      <CardContent>
        {type === 'pie'
          ? <PieChartContent data={data} totalCount={totalCount} />
          : <BarChartContent data={data} totalCount={totalCount} />
        }
      </CardContent>
    </Card>
  );
}
