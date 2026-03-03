import { Toaster as SonnerToaster, type ToasterProps } from "sonner";

const TOAST_CLASS_NAMES = {
  closeButton: "!toast-close-button",
} as const;

export function Toaster(props: ToasterProps) {
  return (
    <SonnerToaster
      richColors
      position="top-right"
      closeButton
      toastOptions={{
        classNames: TOAST_CLASS_NAMES,
      }}
      {...props}
    />
  );
}
