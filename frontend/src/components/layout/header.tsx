import { useNavigate } from "react-router-dom";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet";
import { Sidebar } from "./sidebar";
import { getMe, logout } from "@/app/auth";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Menu, LogOut, User, Settings, HelpCircle, ChevronDown } from "lucide-react";
import { useState } from "react";
import { formatLabel } from "@/lib/utils";

export function Header() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [mobileOpen, setMobileOpen] = useState(false);
  const { data: me } = useQuery({ queryKey: ["me"], queryFn: getMe });
  const roleLabel = me?.roleLabel || formatLabel(me?.role);

  const handleLogout = async () => {
    await logout();
    queryClient.clear();
    navigate("/app/login", { replace: true });
  };

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case "ADMIN":
        return "destructive";
      case "SUPERVISOR":
        return "warning";
      case "AGENT":
        return "default";
      default:
        return "secondary";
    }
  };

  return (
    <header className="sticky top-0 z-40 h-16 flex items-center justify-between gap-4 border-b bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/80 px-4 lg:px-6">
      <div className="flex items-center gap-4">
        <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="lg:hidden">
              <Menu className="h-5 w-5" />
              <span className="sr-only">Toggle menu</span>
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="p-0 w-[280px]">
            <Sidebar role={me?.role} collapsed={false} />
          </SheetContent>
        </Sheet>

        <button 
          onClick={() => navigate("/app/dashboard")}
          className="flex items-center gap-2.5 hover:opacity-80 transition-opacity"
        >
          <div className="h-8 w-8 rounded-lg bg-primary flex items-center justify-center">
            <span className="text-white font-bold text-sm">SH</span>
          </div>
          <div className="hidden sm:block text-left">
            <span className="font-semibold text-foreground block leading-tight">SupportHub</span>
            <span className="text-xs text-muted-foreground">Ticket Router</span>
          </div>
        </button>
      </div>

      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" className="hidden sm:flex">
          <HelpCircle className="h-5 w-5 text-muted-foreground" />
        </Button>

        <Badge variant={getRoleBadgeVariant(me?.role)} className="hidden md:flex">
          {roleLabel}
        </Badge>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="flex items-center gap-2 h-9 px-2">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="bg-primary/10 text-primary text-xs font-medium">
                  {getInitials(me?.fullName || me?.username)}
                </AvatarFallback>
              </Avatar>
              <div className="hidden md:block text-left">
                <p className="text-sm font-medium leading-none">{me?.fullName || me?.username}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{me?.username}</p>
              </div>
              <ChevronDown className="h-4 w-4 text-muted-foreground hidden md:block" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="w-56" align="end" forceMount>
            <DropdownMenuLabel className="font-normal">
              <div className="flex flex-col space-y-1">
                <p className="text-sm font-medium">{me?.fullName || me?.username}</p>
                <p className="text-xs text-muted-foreground">{me?.username}</p>
              </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate("/app/profile")}>
              <User className="mr-2 h-4 w-4" />
              Profile
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Settings className="mr-2 h-4 w-4" />
              Settings
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="text-destructive focus:text-destructive">
              <LogOut className="mr-2 h-4 w-4" />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
