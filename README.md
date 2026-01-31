# HA4E Website

JAMStack website for Hispanos Aliados for Education (HA4E), a 501(c)(3) non-profit organization focused on empowering Hispanic youth through education and skilled trades.

## Tech Stack

- **Babashka**: Build scripts and tooling
- **HTMX + VanillaJS**: Forms via HTMX (no full reload); nav and message handling in main.js
- **Markdown**: Content management
- **Static HTML/CSS**: Simple, fast, CDN-friendly
- **Netlify**: Hosting and CDN

## Project Structure

```
ha4e/
├── src/
│   ├── templates/          # HTML templates
│   ├── content/            # Markdown content files
│   └── assets/             # CSS, JS, images
├── bb/                     # Babashka build scripts
├── scripts/                # Netlify build script, etc.
└── public/                 # Built static site (generated)
```

## Development

### Build

```bash
make build
```

(or `bb bb/build.clj` if you have Babashka in PATH)

### Serve Locally

```bash
make serve
# Open http://localhost:8000
```

## Deployment

Deploy to Netlify by connecting the repository. The build runs `scripts/netlify-build.sh`, which installs Babashka (if needed) and runs `make build`. Publish directory is `public/`.
