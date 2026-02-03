/**
 * Modern Netlify Function (Request/Response) wrapper for cms-auth.
 * Redirects /auth to GitHub OAuth; uses legacy handler built from ClojureScript.
 */
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { handler } = require("./cms-auth-handler.js");

export default async (request) => {
  const url = new URL(request.url);
  const event = {
    headers: {
      "x-forwarded-proto":
        request.headers.get("x-forwarded-proto") || url.protocol.replace(":", ""),
      "x-forwarded-host":
        request.headers.get("x-forwarded-host") || url.host,
    },
  };
  const result = await Promise.resolve(handler(event));
  const status = Number(result.statusCode) || 500;
  const body = result.body != null ? String(result.body) : "";
  const headers = result.headers && typeof result.headers === "object"
    ? result.headers
    : {};
  return new Response(body, { status, headers });
};
