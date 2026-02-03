/**
 * Decap CMS OAuth: exchange code for token and return HTML that postMessages the token to the opener.
 * GitHub OAuth App callback URL must be: https://YOUR_SITE/.netlify/functions/cms-callback
 * Env: CMS_GITHUB_CLIENT_ID, CMS_GITHUB_CLIENT_SECRET.
 */
exports.handler = async (event) => {
  const code = event.queryStringParameters?.code;
  const clientId = process.env.CMS_GITHUB_CLIENT_ID;
  const clientSecret = process.env.CMS_GITHUB_CLIENT_SECRET;
  const siteUrl = process.env.URL || (event.headers['x-forwarded-proto'] + '://' + event.headers['x-forwarded-host']);
  const callbackUrl = `${siteUrl.replace(/\/$/, '')}/.netlify/functions/cms-callback`;

  if (!code || !clientId || !clientSecret) {
    return htmlResponse('error', JSON.stringify({ error: 'missing code or env' }));
  }

  const body = new URLSearchParams({
    client_id: clientId,
    client_secret: clientSecret,
    code,
    redirect_uri: callbackUrl,
  });

  let accessToken;
  try {
    const res = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });
    const data = await res.json();
    if (data.error) {
      return htmlResponse('error', JSON.stringify(data));
    }
    accessToken = data.access_token;
  } catch (err) {
    return htmlResponse('error', JSON.stringify({ error: err.message }));
  }

  const content = JSON.stringify({ token: accessToken, provider: 'github' });
  return htmlResponse('success', content);
};

function htmlResponse(message, content) {
  const escaped = String(content).replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const msgEscaped = `authorization:github:${message}:${escaped}`.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const script = `(function(){var done=false;function send(o){if(!done&&window.opener){done=true;window.opener.postMessage("${msgEscaped}",o||"*");window.close();}}window.opener.postMessage("authorizing:github","*");window.addEventListener("message",function once(e){window.removeEventListener("message",once);send(e.origin);});setTimeout(function(){send("*");},2000);})();`;
  return {
    statusCode: 200,
    headers: { 'Content-Type': 'text/html; charset=utf-8' },
    body: `<html><body><script>${script}<\/script><p>Completing sign-in…</p><\/body><\/html>`,
  };
}
