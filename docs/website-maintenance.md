# Website content maintenance

This guide is for staff who update the HA4E website content with **minimal technical experience**. You can edit text directly in GitHub; when you save your changes, Netlify will automatically rebuild and publish the site.

**The site is available in English and Spanish.** Menus, buttons, and footer text are translated via `src/i18n/en.yaml` and `src/i18n/es.yaml`. Page content lives in `src/content/en/` (English) and `src/content/es/` (Spanish). For details, see [i18n (internationalization)](./i18n.md).

---

## How updates go live

1. **You edit** a content file in GitHub (in your browser).
2. **You save** (commit) the change.
3. **Netlify** detects the update and runs a build. In a few minutes, the live site at [www.hispanosaliados.org](https://www.hispanosaliados.org) reflects your edits.

You don’t need to run any commands or log into Netlify for routine content updates.

---

## Where to edit

All **page content** lives in the **`src/content`** folder. Each file corresponds to one page (or one part of the site):

| File | Page it affects |
|------|------------------|
| `about.md` | **About Us** |
| `contact.md` | **Contact** (main contact info and intro) |
| `contact-success.md` | **Thank-you page** after someone submits the contact form |
| `donate.md` | **Donate** (intro and “Ways to Give”) |
| `get-involved.md` | **Get Involved** (intro and “Ways to Participate”) |
| **`home.md`** | **Home** (text below the hero section) |
| `impact.md` | **Impact** (goals and stats intro) |
| `programs.md` | **Programs** (intro and the three program summaries at the top) |
| `privacy.md` | **Privacy Policy** |
| `404.md` | **404 (Page not found)** message |

**Note:** The **Volunteer Spotlight** on the Get Involved page is in **`spotlights.edn`** in each content folder (`src/content/en/spotlights.edn` and `src/content/es/spotlights.edn`). If you need to change a spotlight, ask a developer or someone familiar with the project to update it.

---

## How to edit a file in GitHub

1. Open the **HA4E website repository** on GitHub (your team will have the link; it’s the same repo used for the site).
2. Go to **`src/content/en/`** (English) or **`src/content/es/`** (Spanish), or **`src/i18n/`** for menus/footer (edit both `en.yaml` and `es.yaml`).
3. Click the **file** you want to edit (e.g. `programs.md`).
4. Click the **pencil icon** (“Edit this file”) near the top right.
5. Edit the text in the box. You can use the simple formatting below.
6. Scroll down to **Commit changes**.
7. In the first box, type a short description of what you changed (e.g. “Update Programs intro” or “Fix contact phone number”).
8. Click **Commit changes** (green button).

That’s it. Netlify will pick up the change and update the live site.

---

## Simple formatting (Markdown)

Content files use **Markdown**: a few symbols give you headings, bold, links, and lists.

| You type | Result |
|----------|--------|
| `## Heading` | A subheading (use two `#`) |
| `### Smaller heading` | A smaller subheading (three `#`) |
| `**bold text**` | **bold text** |
| `*italic text*` | *italic text* |
| `[link text](https://example.com)` | A clickable link |
| `- item` on its own line | A bullet list item |

**Important:** Don’t add a big `# Main Title` at the top of a page. Each page already has its main title (e.g. “Programs”, “Contact”) in the layout; your file should start with the intro paragraph or the first subheading.

**Example** — for `programs.md` you might have:

```markdown
Our programs focus on three main areas: High School Outreach, Trade School Awareness, and Scholarships.

## High School Outreach Programs

We visit high schools and connect directly with students...
```

---

## After you save

- **Netlify** will run a build automatically. This usually takes one to three minutes.
- When the build finishes, the live site is updated. Refresh [www.hispanosaliados.org](https://www.hispanosaliados.org) to see your changes.
- If you have access to Netlify, you can open the site in the Netlify dashboard and check the “Deploys” tab to see build status.

---

## What not to change

To avoid breaking the site, **only edit files inside `src/content`** (the `.md` files listed in the table above), unless someone has asked you to change something else.

- **Don’t edit** the `src/templates` folder (HTML layout), `src/assets` (CSS, images, JS), or the `bb` folder (build scripts) unless a developer has given you instructions.

---

## Need help?

- **Accounts and access:** See [Client onboarding](client-onboarding.md) for how to get access to GitHub, Netlify, and other HA4E accounts.
- **Other docs:** The `docs/` folder has more guides (forms, performance, etc.) for reference.
- **Technical or build issues:** Contact the person who maintains the HA4E website or the developer who set up the repo and Netlify.
