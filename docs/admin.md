# Admin (Decap CMS)

The content editor is at **`/admin`** (e.g. [www.hispanosaliados.org/admin](https://www.hispanosaliados.org/admin)). Staff sign in with **GitHub** to edit Volunteer Spotlights and other CMS-backed content. The CMS uses the GitHub backend (no deprecated Netlify Identity or Git Gateway).

## Authentication (GitHub)

Editors log in with their **GitHub account**. Only users who have **push access** to the content repository (`ha4e/hispanosaliados.org`) can use the CMS. To add an editor, add them as a collaborator on the repo (or to the GitHub org with access to the repo).

### OAuth proxy setup (one-time)

“Login with GitHub” uses an **OAuth proxy** (Netlify Functions) because Netlify’s built-in OAuth (`api.netlify.com/auth/done`) does not send the postMessage format Decap CMS expects, so the popup would show “Authorized” but the CMS would never receive the token.

1. **Create a GitHub OAuth App** (for the CMS only; keep it separate from any other OAuth app):  
   GitHub → Settings → Developer settings → OAuth Apps → **New OAuth App**  
   - **Application name:** e.g. `HA4E Content Editor`  
   - **Homepage URL:** `https://www.hispanosaliados.org`  
   - **Authorization callback URL:** `https://www.hispanosaliados.org/.netlify/functions/cms-callback`  
     (If you use a different primary domain or `*.netlify.app`, use that host instead, e.g. `https://YOUR-SITE.netlify.app/.netlify/functions/cms-callback`.)  
   - Note the **Client ID** and create a **Client Secret**.

2. **Set env vars in Netlify**  
   Netlify → your site → **Project configuration** → **Environment variables**  
   Add:  
   - `CMS_GITHUB_CLIENT_ID` = the OAuth App Client ID  
   - `CMS_GITHUB_CLIENT_SECRET` = the OAuth App Client Secret  

3. **Redeploy** so the functions see the new env vars. (Functions only see variables set in Netlify; a local `.env` does not apply to deployed callbacks.)

**If login fails:** The OAuth popup shows the error. `"missing env"` (and `clientIdSet` / `clientSecretSet` in the message) → set both env vars in Netlify, scope them to **Runtime/Functions**, set a value for your deploy context (Production / Deploy Previews), then **Trigger deploy**. `"missing code"` → set the GitHub OAuth App callback URL exactly to `https://YOUR_SITE/.netlify/functions/cms-callback` (no trailing slash). If the secret still isn’t seen, re-create the variable and use **Clear cache and deploy site**.

After that, `/admin` → “Login with GitHub” opens the proxy at `/auth`, which redirects to GitHub; after authorization, GitHub redirects to the callback function, which posts the token to the opener and closes the popup so Decap can complete login.

### Migrating from Netlify Identity

This project previously used Netlify Identity + Git Gateway (both deprecated). It now uses:

- **Backend:** `github` with repo `ha4e/hispanosaliados.org`
- **Auth:** Our OAuth proxy (`netlify/functions/cms-auth/` and `netlify/functions/cms-callback/`) with a dedicated GitHub OAuth App. The auth endpoint uses the modern Netlify Functions API (Request/Response) via `cms-auth/index.mjs`, which calls the ClojureScript-built handler. Source is in `netlify/functions/src/`; compiled JS is checked in. Rebuild with `make netlify-fns` after editing the `.cljs` files.

You can turn off **Identity** and **Git Gateway** in Netlify (Site configuration → Identity / Git Gateway). The “Authentication Providers” OAuth in Netlify (api.netlify.com/auth/done) is not used for the CMS; the CMS uses the proxy above.

## Indexing and SEO

The admin UI is **intentionally not indexed** by search engines:

- The site sends `X-Robots-Tag: noindex, nofollow` for `/admin`.
- `src/robots.txt` includes `Disallow: /admin`.

So it’s expected that Lighthouse’s SEO audit reports “Page is blocked from indexing” for `/admin`—that’s by design.

## Performance and Lighthouse

Lighthouse performance scores for `/admin` are largely driven by the **Decap CMS** JavaScript bundle (self-hosted at `/admin/decap-cms.js`; the build downloads it from unpkg when missing, pinned to 3.10.0). That removes the unpkg redirect and third-party fetch. The bundle is still large (~1.5 MB) and causes most of the main-thread time and total blocking time (e.g. TBT ~900 ms, LCP ~1.8 s with most time in “element render delay”). That’s a limitation of the CMS; further gains would require a lighter editor.

**Content Security Policy (CSP):** Decap CMS uses `eval()`-style code, so we allow string evaluation via `script-src ... 'unsafe-eval'` for `/admin` (in `src/_headers`, including the block at the end so it wins over the catch-all) and for the OAuth callback (in the cms-callback function’s response headers, built from `netlify/functions/src/cms_callback.cljs`). If a tool still reports “script-src blocked” or “CSP prevents evaluation”, verify the **deployed** response headers (DevTools → Network → select the request → Response Headers); `_headers` only apply on Netlify, not when serving locally.

**Lighthouse clues that affected sign-in:**

- **Cross-Origin-Opener-Policy (COOP):** The site’s catch-all headers set `Cross-Origin-Opener-Policy: same-origin`, which can make `window.opener` null in the OAuth callback. We override COOP for `/admin` with `unsafe-none` in a **second** `/admin` block at the **end** of `_headers` so it wins (Netlify merges headers; last match wins).
- **IndexedDB run warning:** Lighthouse may warn that stored data (e.g. IndexedDB) affects loading; re-running in incognito gives a cleaner performance baseline.
- **SEO “blocked from indexing”:** Expected for `/admin`; see Indexing and SEO above.

## Accessibility

Lighthouse accessibility audits for `/admin` report issues that come from **Decap CMS's own UI**, not from our templates. We do not control Decap's markup or styles. Typical findings:

- **Buttons/controls without accessible names:** e.g. the avatar dropdown and view-control buttons (list/card) have no `aria-label` or visible text. Screen readers may announce them as "button" with no context.
- **Color contrast:** Some Decap UI text (e.g. "Media" nav, sidebar links) does not meet 4.5:1 contrast; Decap uses its own palette.

These are upstream limitations. Improving them would require changes in [Decap CMS](https://github.com/decaporg/decap-cms) (or switching to another editor). We document them here so maintainers know they are expected and not caused by our admin page or headers.
