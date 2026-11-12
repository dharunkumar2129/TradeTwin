import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number | string, currency = 'USD'): string {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(num);
}

export function extractErrorMessage(err: unknown, fallback = 'An unexpected error occurred.'): string {
  if (err instanceof Error && err.message) {
    return err.message;
  }
  if (typeof err === 'object' && err !== null && 'message' in err) {
    const msg = (err as { message?: unknown }).message;
    if (typeof msg === 'string' && msg.trim().length > 0) {
      return msg;
    }
  }
  if (typeof err === 'string' && err.trim().length > 0) {
    return err;
  }
  return fallback;
}
