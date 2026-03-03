import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type ColorVariant = "blue" | "green" | "orange" | "red" | "purple" | "gray";

const colorClasses: Record<ColorVariant, { icon: string; border: string; glow: string }> = {
  blue: {
    icon: "stat-icon-blue text-white shadow-lg shadow-blue-500/25",
    border: "border-l-blue-500",
    glow: "group-hover:shadow-blue-500/10",
  },
  green: {
    icon: "stat-icon-green text-white shadow-lg shadow-green-500/25",
    border: "border-l-green-500",
    glow: "group-hover:shadow-green-500/10",
  },
  orange: {
    icon: "stat-icon-orange text-white shadow-lg shadow-orange-500/25",
    border: "border-l-orange-500",
    glow: "group-hover:shadow-orange-500/10",
  },
  red: {
    icon: "stat-icon-red text-white shadow-lg shadow-red-500/25",
    border: "border-l-red-500",
    glow: "group-hover:shadow-red-500/10",
  },
  purple: {
    icon: "stat-icon-purple text-white shadow-lg shadow-purple-500/25",
    border: "border-l-purple-500",
    glow: "group-hover:shadow-purple-500/10",
  },
  gray: {
    icon: "bg-gradient-to-br from-slate-400 to-slate-500 text-white shadow-lg shadow-slate-500/25",
    border: "border-l-slate-400",
    glow: "group-hover:shadow-slate-500/10",
  },
};

interface StatCardProps {
  label: string;
  value: number | string;
  subtitle?: string;
  icon?: React.ComponentType<{ className?: string }>;
  color?: ColorVariant;
  trend?: { value: number; label: string };
}

function resolveTrendMeta(trendValue: number) {
  if (trendValue >= 0) {
    return {
      colorClass: "text-green-600",
      sign: "+",
    };
  }

  return {
    colorClass: "text-red-600",
    sign: "",
  };
}

export function StatCard(
  { label, value, subtitle, icon: Icon, color = "blue", trend }: Readonly<StatCardProps>
) {
  const colors = colorClasses[color];
  const trendMeta = trend ? resolveTrendMeta(trend.value) : null;

  return (
    <Card
      className={cn(
        "group relative overflow-hidden border-l-4 transition-all duration-300 card-hover",
        colors.border,
        colors.glow
      )}
    >
      <div className="p-6">
        <div className="flex items-start justify-between">
          <div className="space-y-2">
            <p className="text-sm font-medium text-muted-foreground">{label}</p>
            <p className="text-3xl font-bold tracking-tight">{value}</p>
            {subtitle && <p className="text-xs text-muted-foreground">{subtitle}</p>}
            {trend && (
              <div className="flex items-center gap-1 pt-1">
                <span
                  className={cn(
                    "text-xs font-medium",
                    trendMeta?.colorClass
                  )}
                >
                  {trendMeta?.sign}
                  {trend.value}%
                </span>
                <span className="text-xs text-muted-foreground">{trend.label}</span>
              </div>
            )}
          </div>
          {Icon && (
            <div className={cn("flex h-12 w-12 items-center justify-center rounded-xl", colors.icon)}>
              <Icon className="h-6 w-6" />
            </div>
          )}
        </div>
      </div>
      <div className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-transparent via-current/5 to-transparent opacity-0 transition-opacity group-hover:opacity-100" />
    </Card>
  );
}
