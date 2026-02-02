# Forms (Netlify Forms)

The site uses Netlify Forms for the **contact** form (Contact page) and **newsletter** form (footer). Both are submitted via HTMX (AJAX). Ensure **Form detection** is enabled under **Forms** in the Netlify UI; redeploy after adding or changing forms.

## Submissions not showing in Verified

If form submissions show “success” on the site but don’t appear under **Verified submissions** in Netlify, **check the Spam tab first**. Netlify uses Akismet, and minimal submissions (e.g. newsletter with only an email) are often flagged as spam. In the Netlify UI: **Forms → [your form] → Spam submissions**. They’re usually there.

If they’re not in Spam, ensure the form is in the built HTML and **Form detection** is enabled, then redeploy.
