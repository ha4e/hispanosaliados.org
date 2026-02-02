# Forms (Netlify Forms)

## Form detection

The site uses Netlify Forms for the **contact** form (Contact page) and **newsletter** form (footer). Both are submitted via HTMX (AJAX).

A hidden page at `/netlify-forms.html` contains static copies of both forms so Netlify reliably detects and registers them at deploy time. This page is not linked from the site; it exists only for form detection.

After adding or changing forms, redeploy the site so Netlify re-scans and registers the forms. Ensure **Form detection** is enabled under **Forms** in the Netlify UI.

## Newsletter submissions not appearing

If newsletter submissions show “success” on the site but don’t appear under **Verified submissions** in Netlify:

1. **Check the Spam tab**  
   Netlify uses Akismet; minimal submissions (e.g. only an email) are often flagged as spam. In the Netlify UI, open **Forms → [your form]** and switch to **Spam submissions** to see if entries are there.

2. **Redeploy**  
   Ensure the “newsletter” form is in the built HTML (it’s in the footer and on `/netlify-forms.html`). Enable form detection if needed, then trigger a new deploy.
