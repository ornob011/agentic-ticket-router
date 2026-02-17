import type { LucideIcon } from "lucide-react";

type DetailSectionProps = Readonly<{
  icon?: LucideIcon;
  title: string;
  children: React.ReactNode;
}>;

export function DetailSection({ icon: Icon, title, children }: DetailSectionProps) {
  return (
    <div className="space-y-2">
      {Icon ? (
        <div className="flex items-center gap-2">
          {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
          <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{title}</h4>
        </div>
      ) : (
        <h4 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{title}</h4>
      )}
      {children}
    </div>
  );
}
