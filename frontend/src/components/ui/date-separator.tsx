import { cn } from "@/lib/utils";

interface DateSeparatorProps {
  date: string;
  className?: string;
}

export function DateSeparator({ date, className }: Readonly<DateSeparatorProps>) {
  return (
    <div className={cn("date-separator", className)}>
      <span className="px-3">{date}</span>
    </div>
  );
}
