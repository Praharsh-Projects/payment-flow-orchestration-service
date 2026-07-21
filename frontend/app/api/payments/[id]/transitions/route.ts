import { proxyBackend } from "@/lib/backend";

export async function POST(
  request: Request,
  context: { params: Promise<{ id: string }> }
): Promise<Response> {
  const { id } = await context.params;
  return proxyBackend(`/api/v1/payments/${encodeURIComponent(id)}/transitions`, {
    method: "POST",
    body: await request.text()
  });
}
