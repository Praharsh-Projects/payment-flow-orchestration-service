import { proxyBackend } from "@/lib/backend";

export async function GET(request: Request): Promise<Response> {
  const url = new URL(request.url);
  const status = url.searchParams.get("status");
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return proxyBackend(`/api/v1/payments${query}`);
}

export async function POST(request: Request): Promise<Response> {
  const idempotencyKey = request.headers.get("Idempotency-Key");
  return proxyBackend("/api/v1/payments", {
    method: "POST",
    body: await request.text(),
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined
  });
}
