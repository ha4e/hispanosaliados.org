# Admin (Decap CMS)

The content editor is at **`/admin`** (e.g. [www.hispanosaliados.org/admin](https://www.hispanosaliados.org/admin)). Staff sign in with **GitHub** to edit Volunteer Spotlights and other CMS-backed content. The CMS uses the GitHub backend (no deprecated Netlify Identity or Git Gateway).

## Authentication (GitHub)

Editors log in with their **GitHub account**. Only users who have **push access** to the content repository (`ha4e/hispanosaliados.org`) can use the CMS. To add an editor, add them as a collaborator on the repo (or to the GitHub org with access to the repo).

### Netlify OAuth setup (one-time)

For “Login with GitHub” to work on the live site, Netlify must have a GitHub OAuth provider configured:

1. **Create a GitHub OAuth App**  
   GitHub → Settings → Developer settings → OAuth Apps → **New OAuth App**  
   - **Authorization callback URL:** `https://api.netlify.com/auth/done`  
   - Note the **Client ID** and create a **Client Secret**.

2. **Add the provider in Netlify**  
   Netlify → your site → **Project configuration** → **Access & security** → **OAuth**  
   Under **Authentication Providers**, **Install Provider** → **GitHub**, then enter the Client ID and Client Secret and save.

After that, `/admin` will show “Login with GitHub”; after login, Decap CMS talks to the GitHub API directly to read and commit content.

### Migrating from Netlify Identity

This project previously used Netlify Identity + Git Gateway (both deprecated). It now uses:

- **Backend:** `github` with repo `ha4e/hispanosaliados.org`
- **Auth:** Netlify’s OAuth provider (GitHub), which is **not** deprecated

You can turn off **Identity** and **Git Gateway** in Netlify (Site configuration → Identity / Git Gateway) after the GitHub backend and OAuth are working. Existing Identity users need to be given access via GitHub (repo collaborator or org membership) instead of invites.

## Indexing and SEO

The admin UI is **intentionally not indexed** by search engines:

- The site sends `X-Robots-Tag: noindex, nofollow` for `/admin`.
- `src/robots.txt` includes `Disallow: /admin`.

So it’s expected that Lighthouse’s SEO audit reports “Page is blocked from indexing” for `/admin`—that’s by design.

## Performance

Lighthouse performance scores for `/admin` are largely driven by the **Decap CMS** JavaScript bundle (loaded from unpkg). The bundle is large and causes most of the main-thread time and total blocking time. That’s a limitation of the CMS; improving scores significantly would require self-hosting or switching to a lighter editor, not small tweaks to our admin page.
