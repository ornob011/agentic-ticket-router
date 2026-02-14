import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getMe } from "@/app/auth";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { formatLabel } from "@/lib/utils";
import {
  defaultUserSettings,
  loadUserSettings,
  resolveLandingPath,
  saveUserSettings,
  type UserSettings,
} from "@/lib/user-settings";
import { LayoutGrid, Save, Settings2, Undo2 } from "lucide-react";

function SettingsSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-52 w-full" />
      <Skeleton className="h-28 w-full" />
    </div>
  );
}

export default function SettingsPage() {
  const navigate = useNavigate();
  const { data: me, isLoading } = useQuery({ queryKey: ["me"], queryFn: getMe });
  const [settings, setSettings] = useState<UserSettings>(() => loadUserSettings());
  const [saved, setSaved] = useState(false);

  if (isLoading) {
    return <SettingsSkeleton />;
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    saveUserSettings(settings);
    setSaved(true);
  };

  const handleReset = () => {
    const defaults = defaultUserSettings();
    setSettings(defaults);
    saveUserSettings(defaults);
    setSaved(true);
  };

  const handleOpenDefaultLanding = () => {
    const path = resolveLandingPath(settings, me?.role);
    Promise.resolve(navigate(path)).catch(() => undefined);
  };

  const roleLabel = me?.roleLabel || formatLabel(me?.role);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Settings"
        description="Manage preferences that are actively applied in your workspace."
      />

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Settings2 className="h-4 w-4 text-primary" />
              Workspace Behavior
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="grid gap-2">
              <Label htmlFor="default-landing">Default Landing Page</Label>
              <Select
                value={settings.defaultLanding}
                onValueChange={(value: "dashboard" | "tickets" | "queue") => {
                  setSaved(false);
                  setSettings({
                    ...settings,
                    defaultLanding: value,
                  });
                }}
              >
                <SelectTrigger id="default-landing" className="max-w-sm">
                  <SelectValue placeholder="Select default landing page" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="dashboard">Dashboard</SelectItem>
                  <SelectItem value="tickets">My Tickets</SelectItem>
                  <SelectItem value="queue">Queue Inbox</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Applied automatically right after successful sign-in.
              </p>
            </div>

            <label className="flex items-center justify-between rounded-lg border p-3">
              <div className="space-y-1">
                <p className="text-sm font-medium">Default Sidebar Collapsed</p>
                <p className="text-xs text-muted-foreground">
                  Applied when loading the app layout on desktop.
                </p>
              </div>
              <input
                type="checkbox"
                className="h-4 w-4"
                checked={settings.defaultSidebarCollapsed}
                onChange={(event) => {
                  setSaved(false);
                  setSettings({
                    ...settings,
                    defaultSidebarCollapsed: event.target.checked,
                  });
                }}
              />
            </label>

            <div className="flex flex-wrap items-center gap-2">
              <Button type="submit">
                <Save className="mr-2 h-4 w-4" />
                Save Settings
              </Button>
              <Button type="button" variant="outline" onClick={handleOpenDefaultLanding}>
                <LayoutGrid className="mr-2 h-4 w-4" />
                Open Default Landing
              </Button>
              <Button type="button" variant="outline" onClick={handleReset}>
                <Undo2 className="mr-2 h-4 w-4" />
                Reset To Defaults
              </Button>
              {saved && <Badge variant="success">Saved</Badge>}
            </div>
          </CardContent>
        </Card>
      </form>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Current Account Context</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{roleLabel}</Badge>
          <Badge variant="outline">{me?.username}</Badge>
        </CardContent>
      </Card>
    </div>
  );
}
