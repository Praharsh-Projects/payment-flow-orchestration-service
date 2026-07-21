import { beforeEach, describe, expect, it, vi } from "vitest";
import { proxyBackend } from "@/lib/backend";

describe("server-side backend proxy", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("adds the server-held API key", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { "content-type": "application/json" }
      })
    );

    const response = await proxyBackend("/api/v1/payments");
    const init = fetchMock.mock.calls[0][1];
    const headers = new Headers(init?.headers);

    expect(headers.get("X-API-Key")).toBe("local-review-key");
    expect(response.status).toBe(200);
  });

  it("preserves idempotency metadata without exposing other backend headers", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ id: "payment-1" }), {
        status: 201,
        headers: {
          "content-type": "application/json",
          "idempotency-replayed": "false",
          "x-internal-debug": "hidden"
        }
      })
    );

    const response = await proxyBackend("/api/v1/payments", {
      method: "POST",
      headers: { "Idempotency-Key": "console-key-1" },
      body: "{}"
    });

    expect(response.headers.get("idempotency-replayed")).toBe("false");
    expect(response.headers.get("x-internal-debug")).toBeNull();
  });
});
