# Open Source and Netlify Hosting

This doc summarizes Netlify’s open source program, how it fits HA4E, and what to add if you want to open-source the repo and (optionally) apply for the Open Source plan.

## Netlify Open Source Plan – Requirements

From [Netlify’s Open Source Plan Policy](https://www.netlify.com/legal/open-source-policy/):

1. **License**  
   Use a license from the [OSI approved list](https://opensource.org/licenses) or a Creative Commons license with attribution or public domain.

2. **Code of Conduct**  
   Have a Code of Conduct in the repo root or clearly linked from docs (e.g. navigation, footer, or homepage).

3. **Attribution**  
   On the main page or all internal pages, show a link to Netlify: either their [badges](https://www.netlify.com/press/#badges) or your own text: “This site is powered by Netlify” with a link to netlify.com.

4. **Non-commercial**  
   The project must not be commercial (no commercial support/hosting services).

5. **Site content**  
   The plan is intended for sites that contain “documentation, change histories, issue trackers, blogs by the development team, and similar auxiliary information” related to open-source-licensed software. Examples: Hugo, Vue, Redux, Home Assistant.

**What you get:** Same features/limits as Pro, plus free unlimited team members.

**Apply:** [opensource-form.netlify.com](https://opensource-form.netlify.com/)

---

## Open Source submission checklist (HA4E)

| Requirement | Status | Where |
|-------------|--------|--------|
| **License** (OSI-approved or CC with attribution) | ✓ | [LICENSE](LICENSE) — MIT (logo/branding excluded) |
| **Code of Conduct** (repo root or prominent in docs) | ✓ | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) in repo root |
| **Attribution** (link to Netlify on main page or all pages) | ✓ | Footer on all pages: “Deploys by Netlify” badge + link to netlify.com |
| **Non-commercial** | ✓ | HA4E is a 501(c)(3) nonprofit |
| **Site content** (docs, issue tracker, etc. for OSS project) | ⚠️ Gray area | Live site is an org site (donate, contact, programs). Policy targets OSS project docs; Netlify reviews case-by-case. |

**Summary:** We meet the explicit requirements (license, CoC, attribution, non-commercial). The “site content” criterion is interpretive; you can still apply at [opensource-form.netlify.com](https://opensource-form.netlify.com/).

---

## Netlify Free Plan (Alternative)

Netlify’s general **Free plan** (no credit card):

- 100 GB bandwidth, 300 build minutes/month, and other limits that are usually enough for a small nonprofit site.
- No license, Code of Conduct, or “Powered by Netlify” requirement.
- You can keep using this without applying for the Open Source plan.

---

## Fit for HA4E

**Open-sourcing the code on GitHub**

- **Good fit.** Putting the site code on GitHub under an OSI-approved license (e.g. MIT) gives:
  - Transparency and trust.
  - Reuse by other nonprofits or educators.
  - Community contributions and forks.

**Netlify Open Source plan**

- **Gray area.** The policy targets sites whose *content* is about the open-source project (docs, issue tracker, dev blog). HA4E’s live site is an organizational site (donate, contact, programs, about), not “docs for this repo.”
- Netlify reviews case-by-case. Some nonprofits have been accepted; others use the general Free plan.
- **Recommendation:** Add LICENSE, CODE_OF_CONDUCT, and “Powered by Netlify” so you *can* apply. If you’re already fine on the Free plan, you can still open-source the code and skip applying.

**Summary**

| Goal | Recommendation |
|------|----------------|
| Put code on GitHub with an open source license | **Do it.** Add `LICENSE` (e.g. MIT). |
| Code of Conduct | **Add it** if you want contributors or to apply for Netlify Open Source. |
| “Powered by Netlify” in footer | **Add it** if you plan to apply for the Open Source plan. |
| Apply for Netlify Open Source | **Optional.** Try the form if you want Pro-level limits and unlimited team members; otherwise stay on the Free plan. |

---

## Checklist to Be Ready

- [ ] Add `LICENSE` in repo root (e.g. MIT).
- [ ] Add `CODE_OF_CONDUCT.md` in repo root (e.g. [Contributor Covenant](https://www.contributor-covenant.org/)).
- [ ] In site footer: add “Powered by [Netlify](https://www.netlify.com/)” (or use their badge) if applying for Open Source plan.
- [ ] Create a public GitHub repo and push the code.
- [ ] (Optional) Submit [Netlify Open Source application](https://opensource-form.netlify.com/).
