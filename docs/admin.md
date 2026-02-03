# Admin (Decap CMS)

The content editor is at **`/admin`** (e.g. [www.hispanosaliados.org/admin](https://www.hispanosaliados.org/admin)). Staff sign in with Netlify Identity to edit Volunteer Spotlights and other CMS-backed content.

## Invite links and setting a password

Netlify Identity invite emails send users to the **site URL** (often the home page) with a token in the URL hash. The Identity widget that lets them set a password only runs on **`/admin`**.

The site handles this by **redirecting** any page load that has an Identity token in the hash (`invite_token`, `confirmation_token`, `recovery_token`, or `email_change_token`) to **`/admin`** with the same hash. So when a new user clicks “Accept the invite” in the email, they land on the home page, are immediately redirected to `/admin`, and the Decap/Identity widget there can complete the signup (set password).

If invite links still take users to the home page and they never see a password form, check that the link in the email goes to your site (e.g. `https://www.hispanosaliados.org/...`) and not only to `app.netlify.com`. As an alternative, you can customize the **Invitation** email template in Netlify so the link points straight to `/admin`: **Site configuration → Identity → Emails → Invitation**, and use a custom link like `{{ .SiteURL }}/admin/#invite_token={{ .Token }}` instead of `{{ .ConfirmationURL }}`.

## Indexing and SEO

The admin UI is **intentionally not indexed** by search engines:

- The site sends `X-Robots-Tag: noindex, nofollow` for `/admin`.
- `src/robots.txt` includes `Disallow: /admin`.

So it’s expected that Lighthouse’s SEO audit reports “Page is blocked from indexing” for `/admin`—that’s by design.

## Performance

Lighthouse performance scores for `/admin` are largely driven by the **Decap CMS** JavaScript bundle (loaded from unpkg). The bundle is large and causes most of the main-thread time and total blocking time. That’s a limitation of the CMS; improving scores significantly would require self-hosting or switching to a lighter editor, not small tweaks to our admin page.
