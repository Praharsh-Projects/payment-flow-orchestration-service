export type PaymentStatus =
  | "RECEIVED"
  | "AUTHORIZED"
  | "SETTLED"
  | "REJECTED"
  | "CANCELLED";

export interface PaymentSummary {
  id: string;
  merchantReference: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  statusReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentTransition {
  fromStatus: PaymentStatus | null;
  toStatus: PaymentStatus;
  reason: string | null;
  sequence: number;
  occurredAt: string;
}

export interface PaymentView extends PaymentSummary {
  debtorAccountToken: string;
  creditorAccountToken: string;
  allowedTransitions: PaymentStatus[];
  timeline: PaymentTransition[];
}

export interface ApiError {
  code?: string;
  message?: string;
  details?: Record<string, string>;
}
