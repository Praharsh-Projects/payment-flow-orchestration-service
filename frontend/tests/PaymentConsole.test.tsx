import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PaymentConsole } from "@/app/components/PaymentConsole";
import type { PaymentSummary, PaymentView } from "@/lib/payment-types";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onerror: (() => void) | null = null;
  listeners = new Map<string, () => void>();
  closed = false;

  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(name: string, listener: () => void) {
    this.listeners.set(name, listener);
  }

  close() {
    this.closed = true;
  }
}

const summary: PaymentSummary = {
  id: "8ea265fe-b939-4a2d-9ca9-e579c8566c74",
  merchantReference: "order-2026-001",
  amount: 1250.5,
  currency: "SEK",
  status: "RECEIVED",
  statusReason: null,
  version: 0,
  createdAt: "2026-07-21T10:00:00Z",
  updatedAt: "2026-07-21T10:00:00Z"
};

const detail: PaymentView = {
  ...summary,
  debtorAccountToken: "acct_debtor_01",
  creditorAccountToken: "acct_creditor_01",
  allowedTransitions: ["AUTHORIZED", "REJECTED", "CANCELLED"],
  timeline: [
    {
      fromStatus: null,
      toStatus: "RECEIVED",
      reason: "Payment request accepted for processing.",
      sequence: 0,
      occurredAt: "2026-07-21T10:00:00Z"
    }
  ]
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" }
  });
}

describe("PaymentConsole", () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
    vi.stubGlobal("EventSource", FakeEventSource);
    vi.stubGlobal("crypto", { randomUUID: () => "test-request-id" });
  });

  it("loads the queue and opens a payment timeline", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes(summary.id)) return jsonResponse(detail);
      return jsonResponse([summary]);
    });

    render(<PaymentConsole />);
    const queueItem = await screen.findByRole("button", { name: /order-2026-001/i });
    await userEvent.click(queueItem);

    expect(await screen.findByText("Audit timeline")).toBeInTheDocument();
    expect(screen.getByText("acct_debtor_01")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Mark Authorized" })).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(`/api/payments/${summary.id}`, { cache: "no-store" });
  });

  it("creates a payment with an idempotency key", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      if (init?.method === "POST") return jsonResponse(detail, 201);
      return jsonResponse(init ? [] : [summary]);
    });

    render(<PaymentConsole />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    fireEvent.submit(screen.getByRole("button", { name: "Create payment instruction" }).closest("form")!);

    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([, init]) => init?.method === "POST");
      expect(call).toBeDefined();
      expect(new Headers(call?.[1]?.headers).get("Idempotency-Key")).toBe("console-test-request-id");
    });
    expect(await screen.findByText("Audit timeline")).toBeInTheDocument();
  });

  it("sends the visible version with an authorized transition", async () => {
    const authorized: PaymentView = {
      ...detail,
      status: "AUTHORIZED",
      version: 1,
      allowedTransitions: ["SETTLED", "CANCELLED"]
    };
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const url = String(input);
      if (url.endsWith("/transitions") && init?.method === "POST") return jsonResponse(authorized);
      if (url.includes(summary.id)) return jsonResponse(detail);
      return jsonResponse([summary]);
    });

    render(<PaymentConsole />);
    await userEvent.click(await screen.findByRole("button", { name: /order-2026-001/i }));
    await userEvent.click(await screen.findByRole("button", { name: "Mark Authorized" }));

    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([input]) => String(input).endsWith("/transitions"));
      expect(call).toBeDefined();
      expect(JSON.parse(String(call?.[1]?.body))).toEqual({
        targetStatus: "AUTHORIZED",
        expectedVersion: 0,
        reason: null
      });
    });
    expect(await screen.findByRole("button", { name: "Mark Settled" })).toBeInTheDocument();
  });

  it("refreshes the queue when a payment event arrives", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(async () => jsonResponse([summary]));

    render(<PaymentConsole />);
    await waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));
    const initialCalls = fetchMock.mock.calls.length;
    FakeEventSource.instances[0].listeners.get("payment-status")?.();

    await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(initialCalls));
    expect(FakeEventSource.instances[0].url).toBe("/api/payment-events");
  });
});
