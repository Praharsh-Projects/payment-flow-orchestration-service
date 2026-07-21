import { describe, expect, it } from "vitest";
import { formatMoney, statusClasses, statusLabels } from "@/lib/payment-format";

describe("payment formatting", () => {
  it("formats Swedish krona values", () => {
    const value = formatMoney(1250.5, "SEK");
    expect(value).toContain("1");
    expect(value).toContain("250,50");
    expect(value.toLowerCase()).toContain("kr");
  });

  it("falls back for unsupported currency codes", () => {
    expect(formatMoney(12.5, "INVALID")).toBe("12.50 INVALID");
  });

  it("uses distinct terminal-state styling", () => {
    expect(statusClasses("SETTLED")).toContain("emerald");
    expect(statusClasses("REJECTED")).toContain("rose");
    expect(statusClasses("AUTHORIZED")).toContain("sky");
  });

  it("exposes clear labels for every state", () => {
    expect(statusLabels).toEqual({
      RECEIVED: "Received",
      AUTHORIZED: "Authorized",
      SETTLED: "Settled",
      REJECTED: "Rejected",
      CANCELLED: "Cancelled"
    });
  });
});
