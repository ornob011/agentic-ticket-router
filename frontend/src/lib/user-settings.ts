import type { UserRole } from "@/lib/api";

export type UserSettings = {
  defaultLanding: "dashboard" | "tickets" | "queue";
  defaultSidebarCollapsed: boolean;
};

const SETTINGS_STORAGE_KEY = "supporthub:user-settings";

export function defaultUserSettings(): UserSettings {
  return {
    defaultLanding: "dashboard",
    defaultSidebarCollapsed: false,
  };
}

export function loadUserSettings(): UserSettings {
  const raw = localStorage.getItem(SETTINGS_STORAGE_KEY);
  if (!raw) {
    return defaultUserSettings();
  }

  try {
    const parsed = JSON.parse(raw) as Partial<UserSettings>;
    return {
      defaultLanding: parsed.defaultLanding ?? "dashboard",
      defaultSidebarCollapsed: parsed.defaultSidebarCollapsed ?? false,
    };
  } catch {
    return defaultUserSettings();
  }
}

export function saveUserSettings(settings: UserSettings): void {
  localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(settings));
}

export function resolveLandingPath(
  settings: UserSettings,
  role: UserRole | null | undefined
): string {
  if (settings.defaultLanding === "tickets") {
    return "/app/tickets";
  }

  if (settings.defaultLanding === "queue") {
    if (role === "AGENT" || role === "SUPERVISOR" || role === "ADMIN") {
      return "/app/agent/queues/GENERAL_Q";
    }

    return "/app/dashboard";
  }

  return "/app/dashboard";
}
