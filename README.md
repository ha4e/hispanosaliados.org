# HA4E Website

JAMStack website for Hispanos Aliados for Education (HA4E), a 501(c)(3) non-profit organization focused on empowering Hispanic youth through education and skilled trades.

## Tech Stack

- **Babashka**: Build scripts and tooling
- **HTMX + VanillaJS**: Frontend interactivity
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
├── public/                 # Built static site (generated)
└── admin/                  # Admin config (if needed)
```

## Development

### Build

```bash
bb build
```

### Serve Locally

```bash
# After build, serve public/ directory
python3 -m http.server 8000 --directory public
```

## Deployment

Deploy to Netlify by connecting the repository. The build command is `bb build` and the publish directory is `public/`.
