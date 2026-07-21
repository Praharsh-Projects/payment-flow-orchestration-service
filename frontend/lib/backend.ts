const backendUrl = process.env.PAYMENTS_API_URL ?? "http://localhost:8080";
const apiKey = process.env.PAYMENTS_API_KEY ?? "local-review-key";

export async function proxyBackend(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("X-API-Key", apiKey);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${backendUrl}${path}`, {
    ...init,
    headers,
    cache: "no-store"
  });

  const responseHeaders = new Headers();
  const contentType = response.headers.get("content-type");
  if (contentType) responseHeaders.set("content-type", contentType);
  const replayed = response.headers.get("idempotency-replayed");
  if (replayed) responseHeaders.set("idempotency-replayed", replayed);

  return new Response(response.body, {
    status: response.status,
    headers: responseHeaders
  });
}
