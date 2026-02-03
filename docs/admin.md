# Admin (Decap CMS)

The content editor is at **`/admin`** (e.g. [www.hispanosaliados.org/admin](https://www.hispanosaliados.org/admin)). Staff sign in with Netlify Identity to edit Volunteer Spotlights and other CMS-backed content.

## Invite links and setting a password

Netlify Identity invite emails send users to the **site URL** (often the home page) with a token in the URL (hash or query). The widget that lets them **set a password** only runs on a dedicated page so Decap CMS on `/admin` doesn’t overwrite the URL and lose the token.

The site **redirects** any page load that has an Identity token (`invite_token`, `confirmation_token`, `recovery_token`, or `email_change_token`) in the **hash or query string** to **`/admin/accept-invite.html`**, with the token in the hash so the Identity widget can read it and show the set-password flow.

- If the user **never sees a set-password form** and later gets **“invalid_grant: Email not confirmed”** when signing in, the invite link didn’t complete (token was lost or not processed). Use a **fresh invite link** from the email and ensure you land on `…/admin/accept-invite.html` with the token in the URL. If your email client or Netlify sends the token as a query parameter, the site now normalizes it to the hash on accept-invite so the widget can process it.
- For the most reliable flow, customize the **Invitation** email in Netlify so the link goes straight to the accept-invite page: **Site configuration → Identity → Emails → Invitation**, and set the link to `{{ .SiteURL }}/admin/accept-invite.html#invite_token={{ .Token }}` (instead of `{{ .ConfirmationURL }}`).

## Indexing and SEO

The admin UI is **intentionally not indexed** by search engines:

- The site sends `X-Robots-Tag: noindex, nofollow` for `/admin`.
- `src/robots.txt` includes `Disallow: /admin`.

So it’s expected that Lighthouse’s SEO audit reports “Page is blocked from indexing” for `/admin`—that’s by design.

## Performance

Lighthouse performance scores for `/admin` are largely driven by the **Decap CMS** JavaScript bundle (loaded from unpkg). The bundle is large and causes most of the main-thread time and total blocking time. That’s a limitation of the CMS; improving scores significantly would require self-hosting or switching to a lighter editor, not small tweaks to our admin page.
