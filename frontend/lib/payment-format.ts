import type { PaymentStatus } from "@/lib/payment-types";

export const statusLabels: Record<PaymentStatus, string> = {
  RECEIVED: "Received",
  AUTHORIZED: "Authorized",
  SETTLED: "Settled",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled"
};

export function formatMoney(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat("sv-SE", {
      style: "currency",
      currency,
      minimumFractionDigits: 2
    }).format(amount);
  } catch {
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function statusClasses(status: PaymentStatus): string {
  if (status === "SETTLED") return "bg-emerald-100 text-emerald-900";
  if (status === "REJECTED" || status === "CANCELLED") return "bg-rose-100 text-rose-900";
  if (status === "AUTHORIZED") return "bg-sky-100 text-sky-900";
  return "bg-amber-100 text-amber-950";
}

export function formatTimestamp(value: string): string {
  return new Intl.DateTimeFormat("sv-SE", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}
