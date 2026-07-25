"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import {
  formatMoney,
  formatTimestamp,
  statusClasses,
  statusLabels
} from "@/lib/payment-format";
import type {
  ApiError,
  PaymentStatus,
  PaymentSummary,
  PaymentView
} from "@/lib/payment-types";

const filters: Array<{ label: string; value: PaymentStatus | "ALL" }> = [
  { label: "All", value: "ALL" },
  { label: "Received", value: "RECEIVED" },
  { label: "Authorized", value: "AUTHORIZED" },
  { label: "Settled", value: "SETTLED" },
  { label: "Exceptions", value: "REJECTED" }
];

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as ApiError;
    const detail = payload.details ? Object.values(payload.details)[0] : undefined;
    return detail ?? payload.message ?? `Request failed with status ${response.status}.`;
  } catch {
    return `Request failed with status ${response.status}.`;
  }
}

export function PaymentConsole() {
  const [payments, setPayments] = useState<PaymentSummary[]>([]);
  const [selected, setSelected] = useState<PaymentView | null>(null);
  const [filter, setFilter] = useState<PaymentStatus | "ALL">("ALL");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPayment = useCallback(async (paymentId: string) => {
    const response = await fetch(`/api/payments/${paymentId}`, { cache: "no-store" });
    if (!response.ok) throw new Error(await errorMessage(response));
    const payment = (await response.json()) as PaymentView;
    setSelected(payment);
    return payment;
  }, []);

  const loadQueue = useCallback(async (activeFilter: PaymentStatus | "ALL") => {
    const query = activeFilter === "ALL" ? "" : `?status=${activeFilter}`;
    const response = await fetch(`/api/payments${query}`, { cache: "no-store" });
    if (!response.ok) throw new Error(await errorMessage(response));
    setPayments((await response.json()) as PaymentSummary[]);
  }, []);

  useEffect(() => {
    loadQueue(filter).catch((cause: unknown) => {
      setError(cause instanceof Error ? cause.message : "The payment queue could not be loaded.");
    });
  }, [filter, loadQueue]);

  useEffect(() => {
    const stream = new EventSource("/api/payment-events");
    const refresh = () => {
      void loadQueue(filter);
      if (selected?.id) void loadPayment(selected.id);
    };
    stream.addEventListener("payment-status", refresh);
    stream.onerror = () => stream.close();
    return () => stream.close();
  }, [filter, loadPayment, loadQueue, selected?.id]);

  async function createPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const form = new FormData(event.currentTarget);
    const payload = {
      merchantReference: String(form.get("merchantReference")),
      amount: Number(form.get("amount")),
      currency: String(form.get("currency")).toUpperCase(),
      debtorAccountToken: String(form.get("debtorAccountToken")),
      creditorAccountToken: String(form.get("creditorAccountToken"))
    };

    try {
      const response = await fetch("/api/payments", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": `console-${crypto.randomUUID()}`
        },
        body: JSON.stringify(payload)
      });
      if (!response.ok) throw new Error(await errorMessage(response));
      const payment = (await response.json()) as PaymentView;
      setSelected(payment);
      setReason("");
      event.currentTarget.reset();
      await loadQueue(filter);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "The payment could not be created.");
    } finally {
      setBusy(false);
    }
  }

  async function transition(targetStatus: PaymentStatus) {
    if (!selected) return;
    setBusy(true);
    setError(null);
    try {
      const response = await fetch(`/api/payments/${selected.id}/transitions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          targetStatus,
          expectedVersion: selected.version,
          reason: reason.trim() || null
        })
      });
      if (!response.ok) throw new Error(await errorMessage(response));
      setSelected((await response.json()) as PaymentView);
      setReason("");
      await loadQueue(filter);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "The status change could not be applied.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="min-h-screen bg-paper text-ink">
      <section className="border-b border-ink/10 bg-ink text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-6 py-10 lg:grid-cols-[1.4fr_0.6fr] lg:px-10">
          <div>
            <p className="mb-3 text-xs font-semibold uppercase tracking-[0.28em] text-mist">
              Payment operations console
            </p>
            <h1 className="max-w-3xl text-4xl font-semibold leading-tight sm:text-5xl">
              Every status change should be explainable before it is fast.
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-7 text-white/75">
              Create synthetic payment instructions, apply controlled lifecycle transitions and inspect the
              audit timeline as events reach the console.
            </p>
          </div>
          <div className="self-end rounded-2xl border border-white/15 bg-white/5 p-5">
            <p className="text-sm font-semibold text-mist">Safety boundary</p>
            <p className="mt-2 text-sm leading-6 text-white/70">
              Token references only. No card numbers, bank credentials, real funds or autonomous settlement.
            </p>
          </div>
        </div>
      </section>

      <div className="mx-auto grid max-w-7xl gap-6 px-6 py-8 lg:grid-cols-[0.78fr_1.22fr] lg:px-10">
        <section className="space-y-6">
          <form onSubmit={createPayment} className="rounded-2xl bg-white p-6 shadow-panel">
            <div className="flex items-baseline justify-between gap-4">
              <h2 className="text-xl font-semibold">New payment</h2>
              <span className="text-xs font-medium uppercase tracking-[0.2em] text-ink/45">Synthetic</span>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <label className="sm:col-span-2">
                <span className="field-label">Merchant reference</span>
                <input name="merchantReference" required defaultValue="order-2026-001" className="field" />
              </label>
              <label>
                <span className="field-label">Amount</span>
                <input name="amount" required type="number" min="0.01" step="0.01" defaultValue="1250.50" className="field" />
              </label>
              <label>
                <span className="field-label">Currency</span>
                <input name="currency" required maxLength={3} defaultValue="SEK" className="field uppercase" />
              </label>
              <label className="sm:col-span-2">
                <span className="field-label">Debtor account token</span>
                <input name="debtorAccountToken" required defaultValue="acct_debtor_01" className="field font-mono text-sm" />
              </label>
              <label className="sm:col-span-2">
                <span className="field-label">Creditor account token</span>
                <input name="creditorAccountToken" required defaultValue="acct_creditor_01" className="field font-mono text-sm" />
              </label>
            </div>
            <button type="submit" disabled={busy} className="mt-5 w-full rounded-xl bg-signal px-4 py-3 font-semibold text-white transition hover:bg-signal/90 disabled:cursor-wait disabled:opacity-55">
              {busy ? "Applying change..." : "Create payment instruction"}
            </button>
          </form>

          <div className="rounded-2xl bg-white p-6 shadow-panel">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">Queue</h2>
              <span className="rounded-full bg-mist px-3 py-1 text-xs font-semibold text-teal">
                {payments.length} visible
              </span>
            </div>
            <fieldset className="mt-4 flex flex-wrap gap-2" aria-label="Payment status filters">
              {filters.map((item) => (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => setFilter(item.value)}
                  className={`rounded-full px-3 py-1.5 text-xs font-semibold transition ${
                    filter === item.value ? "bg-ink text-white" : "bg-paper text-ink/65 hover:text-ink"
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </fieldset>
            <div className="mt-5 space-y-3">
              {payments.length === 0 ? (
                <p className="rounded-xl border border-dashed border-ink/20 p-5 text-sm text-ink/55">
                  No payments match this filter.
                </p>
              ) : (
                payments.map((payment) => (
                  <button
                    key={payment.id}
                    type="button"
                    onClick={() => void loadPayment(payment.id)}
                    className={`w-full rounded-xl border p-4 text-left transition ${
                      selected?.id === payment.id
                        ? "border-teal bg-mist/60"
                        : "border-ink/10 hover:border-ink/30"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-semibold">{payment.merchantReference}</p>
                        <p className="mt-1 text-sm text-ink/55">{formatMoney(payment.amount, payment.currency)}</p>
                      </div>
                      <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusClasses(payment.status)}`}>
                        {statusLabels[payment.status]}
                      </span>
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        </section>

        <section className="rounded-2xl bg-white p-6 shadow-panel lg:p-8">
          {error && (
            <div role="alert" className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-900">
              {error}
            </div>
          )}
          {!selected ? (
            <div className="flex min-h-[36rem] items-center justify-center text-center">
              <div className="max-w-sm">
                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-teal">Nothing selected</p>
                <h2 className="mt-3 text-3xl font-semibold">Choose a payment to inspect its state.</h2>
                <p className="mt-3 text-sm leading-6 text-ink/55">
                  The detail panel exposes current version, allowed actions and the complete transition history.
                </p>
              </div>
            </div>
          ) : (
            <div>
              <div className="flex flex-wrap items-start justify-between gap-5 border-b border-ink/10 pb-6">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-ink/45">Payment detail</p>
                  <h2 className="mt-2 text-3xl font-semibold">{selected.merchantReference}</h2>
                  <p className="mt-2 text-lg text-ink/60">{formatMoney(selected.amount, selected.currency)}</p>
                </div>
                <span className={`rounded-full px-3 py-1.5 text-sm font-semibold ${statusClasses(selected.status)}`}>
                  {statusLabels[selected.status]}
                </span>
              </div>

              <dl className="grid gap-4 border-b border-ink/10 py-6 sm:grid-cols-2">
                <div><dt className="detail-label">Version</dt><dd className="detail-value">{selected.version}</dd></div>
                <div><dt className="detail-label">Last update</dt><dd className="detail-value">{formatTimestamp(selected.updatedAt)}</dd></div>
                <div><dt className="detail-label">Debtor token</dt><dd className="detail-value font-mono text-sm">{selected.debtorAccountToken}</dd></div>
                <div><dt className="detail-label">Creditor token</dt><dd className="detail-value font-mono text-sm">{selected.creditorAccountToken}</dd></div>
              </dl>

              <div className="border-b border-ink/10 py-6">
                <h3 className="font-semibold">Controlled actions</h3>
                {selected.allowedTransitions.length === 0 ? (
                  <p className="mt-2 text-sm text-ink/55">This payment is terminal. No further transitions are allowed.</p>
                ) : (
                  <>
                    <label className="mt-4 block">
                      <span className="field-label">Reason for rejection or cancellation</span>
                      <input value={reason} onChange={(event) => setReason(event.target.value)} className="field" placeholder="Required for exception outcomes" />
                    </label>
                    <div className="mt-4 flex flex-wrap gap-2">
                      {selected.allowedTransitions.map((target) => (
                        <button
                          key={target}
                          type="button"
                          disabled={busy}
                          onClick={() => void transition(target)}
                          className="rounded-xl border border-ink/15 px-4 py-2 text-sm font-semibold transition hover:border-teal hover:text-teal disabled:opacity-50"
                        >
                          Mark {statusLabels[target]}
                        </button>
                      ))}
                    </div>
                  </>
                )}
              </div>

              <div className="pt-6">
                <h3 className="font-semibold">Audit timeline</h3>
                <ol className="mt-5 space-y-5">
                  {selected.timeline.map((entry) => (
                    <li key={`${entry.sequence}-${entry.toStatus}`} className="relative border-l border-teal/30 pl-5">
                      <span className="absolute -left-1.5 top-1 h-3 w-3 rounded-full border-2 border-white bg-teal" />
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <p className="font-semibold">{statusLabels[entry.toStatus]}</p>
                        <time className="text-xs text-ink/45">{formatTimestamp(entry.occurredAt)}</time>
                      </div>
                      <p className="mt-1 text-sm text-ink/55">
                        Sequence {entry.sequence}{entry.reason ? ` - ${entry.reason}` : ""}
                      </p>
                    </li>
                  ))}
                </ol>
              </div>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
