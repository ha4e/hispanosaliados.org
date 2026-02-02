# Performance

This doc describes how we optimize the HA4E site and how to **check performance without any extension** using built-in and web tools.

## How to check performance (no extension)

### 1. Chrome DevTools → Lighthouse

**Lighthouse** is built into Chrome. No install needed.

1. Open the site (e.g. `make serve` then http://localhost:8000, or your Netlify URL).
2. Open **DevTools**: `F12` or right‑click → **Inspect**.
3. Go to the **Lighthouse** tab.
4. Choose **Performance** (and optionally **Best practices**, **SEO**).
5. Click **Analyze page load**.

You get a Performance score (0–100), Core Web Vitals, and a list of opportunities (e.g. “Serve static assets with an efficient cache policy”, “Minify CSS”).

### 2. PageSpeed Insights (online)

[PageSpeed Insights](https://pagespeed.web.dev/) uses Lighthouse and works from a URL. Good for testing a **live** site (e.g. Netlify deploy).

1. Go to https://pagespeed.web.dev/.
2. Enter your site URL (e.g. `https://yoursite.netlify.app` or your production URL).
3. Click **Analyze**.

You get mobile and desktop scores plus the same kind of suggestions. **Note:** It can’t test `http://localhost:8000`; use a public URL or deploy first.

### 3. Local testing with Lighthouse

For local URLs (e.g. http://localhost:8000), use **Chrome DevTools → Lighthouse** as above. That’s the standard way to check performance without an extension.

### 4. Why security headers don’t show on localhost

The local server (`make serve` → `python3 -m http.server` in `public/`) does **not** read Netlify’s `_headers` file. So when you run Lighthouse on `http://localhost:8000`, you will see:

- “No CSP found in enforcement mode”
- “No HSTS header found”
- “No COOP header found”
- “No frame control policy found”

These headers are defined in `src/_headers` and are applied **only by Netlify** when the site is deployed. To verify they are present, run Lighthouse (or [PageSpeed Insights](https://pagespeed.web.dev/)) against a **Netlify deploy URL** (e.g. `https://yoursite.netlify.app`). There is no need to change the local server unless you want to simulate Netlify headers locally (e.g. with `netlify dev`).

---

## What we optimize for (Lighthouse / PageSpeed)

| What they check | What we do |
|-----------------|------------|
| **Efficient cache policy** | `src/_headers` sets long-lived `Cache-Control` for `/assets/*` so CSS, JS, and images are cached. HTML uses short cache. |
| **Minify CSS / Minify JavaScript** | Build runs `npx csso-cli` and `npx terser` when available. |
| **Reduce requests / unused resources** | Single CSS and single JS per page; HTMX used for forms (contact, newsletter). |
| **Render-blocking resources** | Stylesheet in `<head>`; only script is `main.js` at end of `<body>`. |
| **Avoid empty src/href** | No empty links or script sources. |

For **prioritized next steps** (e.g. WebP in templates, verifying headers on Netlify), see [Performance suggestions](#performance-suggestions-priority-order) below.

## Build optimizations

- **Cache headers**  
  `src/_headers` is copied to `public/_headers`. Netlify serves it so assets under `/assets/*` get `Cache-Control: public, max-age=31536000, immutable`.

- **Security headers**  
  For `/*`, Netlify also sends: `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Strict-Transport-Security`, `Cross-Origin-Opener-Policy`, `Permissions-Policy`. These apply on deploy; local dev may not send them.

- **Image optimization (WebP, AVIF, responsive)**  
  After copying assets, the build runs `npx sharp-cli` to:
  - Generate **responsive widths** (480, 800, 1200 px) for content photos in `src/assets/images/photos/`, in PNG, WebP, and AVIF. Outputs are named e.g. `name-480w.webp`. Templates that use these reference them via `srcset` and `sizes`.
  - Generate **full-size WebP** (quality 85) and **AVIF** (quality 70) only where needed: logo, spotlights, and the few photos that use full-size (no srcset) in templates. Photos that use only responsive srcset skip full-size WebP/AVIF to avoid redundant work.
  - Skip favicons and small favicon sizes.  
  If `sharp-cli` isn’t available, these steps are skipped and the site still works with originals.

- **Netlify build cache (faster rebuilds)**  
  The build skips regenerating an image when the output already exists and is **newer than the source** (see `need?` / `need-generate?` in `bb/build.clj`). On Netlify, `public/` is empty at the start of each build unless it is cached. This project uses a **local plugin** (`plugins/cache-images`) that restores `public/assets/images` in `onPreBuild` and saves it in **`onPostBuild`** (before the deploy stage). Saving in `onPostBuild` ensures the directory is still full when we cache it; the npm package `netlify-plugin-cache` uses `onSuccess` (after deploy), when `public/` may already be moved or cleared, so it can end up saving only 2 files and cause alternating full/empty cache on each deploy. `copy-assets` merges `src/assets` into `public/assets` without deleting existing files, so cached WebP/AVIF/responsive outputs in `public/assets/images` are preserved and the build skips them unless the source image changed.
  Without this cache, every build regenerates all images (~5+ minutes). With the plugin, only changed or new images are processed.
  **Note:** If you use **Clear cache and deploy**, the build runs with **"Building without cache"** and the plugin cache is cleared (and often not re-saved), so all images are regenerated and the next deploy may still only restore 2 files. To repopulate the image cache: run a **normal** deploy (push to branch or **Trigger deploy** without "Clear cache") so the plugin can save the full `public/assets/images`; the following deploy will then restore that cache and skip unchanged images.

  **If the plugin says it only restored 2 files:** The cache for `public/assets/images` only had 2 files when it was restored (the plugin logs "Successfully restored: public/assets/images ... X files in total"). To see exactly what’s cached and restored, add [netlify-plugin-debug-cache](https://github.com/netlify-labs/netlify-plugin-debug-cache) after this plugin in `netlify.toml`; it writes `cache-output.json` into the publish directory.

- **Minification**  
  After WebP generation, the build runs:
  - `npx csso-cli` on `public/assets/css/style.css`
  - `npx terser` on `public/assets/js/main.js`  
  If npx or the tools aren’t available, the build continues with unminified files.

- **HTMX for forms**  
  Contact and newsletter forms use HTMX (`hx-post`, `hx-swap="none"`) so submissions go via AJAX; `main.js` only shows success/error messages. HTMX loads from unpkg in `<head>` with `defer`.

---

## Image optimization (concrete steps)

### 1. Build generates WebP

- **What:** `bb build` (or `make build`) runs `generate-webp`, which uses `npx sharp-cli` to create a `.webp` next to each PNG/JPG under `public/assets/images` (e.g. `home-hero.png` → `home-hero.webp`).
- **Requirement:** Node/npx; first run will pull `sharp-cli`. If it fails, build continues and only PNG/JPG are deployed.
- **Skip list:** Favicons and `*-16x16.png` / `*-32x32.png` are not converted.

### 2. Serve WebP in HTML with `<picture>`

Use `<picture>` so supporting browsers get WebP and others get the original:

```html
<picture>
  <source srcset="/assets/images/photos/home-hero.webp" type="image/webp">
  <img src="/assets/images/photos/home-hero.png" alt="..." loading="eager" fetchpriority="high">
</picture>
```

- **Where:** Replace plain `<img src=".../photo.png">` with the block above for hero and content images. Keep the same `alt`, `loading`, `fetchpriority`, and `width`/`height` on the `<img>`.
- **Priority:** Start with the LCP image (e.g. home hero) and above-the-fold images, then the rest.

### 3. Optional: AVIF

For more savings, generate AVIF in addition to WebP (e.g. with `sharp-cli --format avif` or a separate script). Then add a second `<source type="image/avif" srcset="...">` before the WebP source; browsers that support AVIF will pick it.

### 4. Optional: Responsive images (`srcset` / `sizes`)

If you add multiple widths (e.g. 480w, 800w, 1200w) in the build, use `srcset` and `sizes` so small screens don’t download huge files:

```html
<picture>
  <source srcset="/assets/images/photos/hero-480.webp 480w, /assets/images/photos/hero-800.webp 800w" type="image/webp" sizes="(max-width: 600px) 100vw, 800px">
  <img src="/assets/images/photos/hero.png" alt="..." width="800" height="600">
</picture>
```

Implementing multiple widths would require extra build steps (e.g. sharp resizing) and template or config listing desired widths.

## Performance suggestions (priority order)

Use these to improve Lighthouse scores and Core Web Vitals. Tackle in order for the biggest impact.

1. **Serve WebP in templates** — **Done.** All content images and the logo use `<picture>` with WebP (and AVIF) sources; PNG/JPG remain as fallback.

2. **Verify production headers**  
   Run Lighthouse or PageSpeed Insights against your **Netlify deploy URL**, not localhost. That confirms cache and security headers (CSP, HSTS, etc.) are applied and Best Practices items pass.

3. **Reduce render-blocking CSS** — **Done.**  
   The main stylesheet is loaded with `media="print" onload="this.media='all'"` so it doesn't block first paint; `<noscript>` provides a fallback. If you see a brief flash of unstyled content (FOUC), consider inlining critical above-the-fold CSS and keeping the async full stylesheet.

4. **Responsive images** — **Done.** The build generates 480w, 800w, 1200w variants for content photos; templates use `srcset`/`sizes` so small viewports get smaller files (see “Responsive images” above).

5. **AVIF (optional)** — **Done.** The build generates `.avif` files and templates include `<source type="image/avif">` before WebP so supporting browsers get smaller images.

6. **Unsized images** — **Done.** The Netlify badge `<img>` has explicit `width="114"` and `height="51"` so layout is stable (Lighthouse: unsized-images).

7. **Header logo size** — **Done.** The build generates 256w logo variants; the header `<picture>` uses `srcset` with 256w/1024w and `sizes="80px"` so the browser fetches the smaller image (Lighthouse: image-delivery).

## Lighthouse-driven optimizations (reference)

Applied from Lighthouse audits (e.g. for `/privacy`):

| Audit / issue | Action taken |
|---------------|--------------|
| **unsized-images** | Netlify badge: added `width="114"` `height="51"`. |
| **image-delivery-insight** | Header logo: generate 256w AVIF/WebP; use `srcset`/`sizes="80px"` so header loads small logo. |
| **render-blocking-insight** | Main CSS: `media="print" onload="this.media='all'"` + `<noscript>` fallback. |
| **inspector-issues (CSP)** | Add specific `sha256` hashes to `style-src` for any new inline styles (e.g. HTMX). Re-run Lighthouse on deploy and add hashes for any remaining CSP violations. |

**Not fully in our control:** mainthread-work-breakdown and bootup-time often include Chrome extensions and third-party script (e.g. GiveButter). Unused/minified JS savings in Lighthouse may be from extensions; focus on first-party JS (e.g. HTMX, main.js) and consider self-hosting HTMX to shorten the critical request chain.

---

## Quick local benchmark

```bash
make build
make serve
# Open http://localhost:8000 → DevTools → Lighthouse → Analyze page load
```

For cache and minification behavior closest to production, run Lighthouse (or PageSpeed Insights) against a Netlify deploy URL.
