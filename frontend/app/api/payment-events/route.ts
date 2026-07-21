import { proxyBackend } from "@/lib/backend";

export const dynamic = "force-dynamic";

export async function GET(): Promise<Response> {
  const response = await proxyBackend("/api/v1/events/stream", {
    headers: { Accept: "text/event-stream" }
  });
  const headers = new Headers(response.headers);
  headers.set("Cache-Control", "no-cache, no-transform");
  headers.set("Connection", "keep-alive");
  return new Response(response.body, { status: response.status, headers });
}
