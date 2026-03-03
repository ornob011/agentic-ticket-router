import { toast, type ExternalToast } from "sonner";

type NotificationOptions = ExternalToast;

export const notification = {
  success: (message: string, options?: NotificationOptions) => {
    toast.success(message, options);
  },

  error: (message: string, options?: NotificationOptions) => {
    toast.error(message, options);
  },

  info: (message: string, options?: NotificationOptions) => {
    toast.info(message, options);
  },

  warning: (message: string, options?: NotificationOptions) => {
    toast.warning(message, options);
  },

  promise: <T>(
    promise: Promise<T>,
    messages: {
      loading: string;
      success: string | ((data: T) => string);
      error: string | ((error: unknown) => string);
    }
  ) => {
    return toast.promise(promise, messages);
  },

  dismiss: (id?: string | number) => {
    toast.dismiss(id);
  },
};

export type NotificationService = typeof notification;
