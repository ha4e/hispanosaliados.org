/**
 * Decap CMS OAuth: redirect to GitHub authorize URL.
 * Decap opens base_url/auth; we redirect to GitHub with callback = this site’s cms-callback.
 * Env: CMS_GITHUB_CLIENT_ID, CMS_GITHUB_CLIENT_SECRET (from a GitHub OAuth App whose callback is this site’s /.netlify/functions/cms-callback).
 */
exports.handler = async (event) => {
  const clientId = process.env.CMS_GITHUB_CLIENT_ID;
  const siteUrl = process.env.URL || (event.headers['x-forwarded-proto'] + '://' + event.headers['x-forwarded-host']);
  const callbackUrl = `${siteUrl.replace(/\/$/, '')}/.netlify/functions/cms-callback`;

  if (!clientId) {
    return { statusCode: 500, body: 'CMS_GITHUB_CLIENT_ID not set' };
  }

  const state = require('crypto').randomBytes(16).toString('hex');
  const scope = 'repo,user';
  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: callbackUrl,
    scope,
    state,
  });
  const url = `https://github.com/login/oauth/authorize?${params.toString()}`;

  return {
    statusCode: 302,
    headers: { Location: url },
    body: '',
  };
};
