# HA4E Website

JAMStack website for Hispanos Aliados for Education (HA4E), a 501(c)(3) non-profit organization focused on empowering Hispanic youth through education and skilled trades.

## Tech Stack

- **Babashka**: Build scripts and tooling
- **HTMX + VanillaJS**: Forms via HTMX (no full reload); nav and message handling in main.js
- **Markdown**: Content management
- **Static HTML/CSS**: Simple, fast, CDN-friendly
- **Netlify**: Hosting and CDN ([Open Source plan](https://www.netlify.com/legal/open-source-policy))

## Project Structure

```
ha4e/
├── src/
│   ├── templates/          # HTML templates
│   ├── content/             # Markdown content files
│   ├── assets/              # CSS, JS, images
│   ├── admin/               # Decap CMS (config, entry page at /admin)
│   ├── _headers             # Response headers (CSP, cache, etc.)
│   └── _redirects           # URL rewrites
├── netlify/
│   └── functions/          # OAuth proxy for CMS (cms-auth, cms-callback)
├── bb/                      # Babashka build scripts
├── docs/                    # Project documentation
├── plugins/                 # Netlify build plugins (e.g. cache-images)
├── scripts/                 # Netlify build script, etc.
└── public/                  # Built static site (generated)
```

## Development

### Build

```bash
make build
```

(or `bb bb/build.clj` if you have Babashka in PATH)

To rebuild the Netlify cache-images plugin (ClojureScript → JS): `make plugin`.

### Serve Locally

```bash
make serve
# Open http://localhost:8000
```

## Deployment

Deploy to Netlify by connecting the repository. The build runs `scripts/netlify-build.sh`, which installs Babashka (if needed) and runs `make build`. Publish directory is `public/`.

For staff who update **content**: edit Markdown/JSON in `src/content/` via GitHub, or use the **web editor** at `/admin` (Decap CMS, GitHub sign-in). See [docs/website-maintenance.md](docs/website-maintenance.md) and [docs/admin.md](docs/admin.md).

## License and trademarks

- **Code:** MIT License — see [LICENSE](LICENSE). Logo and branding assets are excluded (see LICENSE).
- **Trademarks and name:** [TRADEMARKS.md](TRADEMARKS.md) — use of the code does not grant rights to use HA4E’s name or marks; no endorsement implied.
- **Notice:** [NOTICE](NOTICE) — copyright and trademark notice, no-endorsement disclaimer.
