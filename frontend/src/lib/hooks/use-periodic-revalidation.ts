import { useEffect } from "react";

const DEFAULT_INTERVAL_MS = 30000;

export function usePeriodicRevalidation(
  revalidator: { revalidate: () => void },
  intervalMs: number = DEFAULT_INTERVAL_MS
) {
  useEffect(() => {
    const interval = setInterval(() => {
      revalidator.revalidate();
    }, intervalMs);

    return () => clearInterval(interval);
  }, [intervalMs, revalidator]);
}
