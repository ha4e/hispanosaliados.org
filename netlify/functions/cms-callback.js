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
  const authMsg = `authorization:github:${message}:${content}`;
  const authMsgJson = JSON.stringify(authMsg);
  // When opener is null, write to localStorage immediately so closing the window doesn't lose the token.
  const script = `(function(){var op=window.opener;var rawMsg=window.CMS_OAUTH_MSG;var done=false;function send(origin){if(done)return;done=true;if(op&&rawMsg)try{op.postMessage(rawMsg,origin||"*");setTimeout(function(){window.close();},300);}catch(e){window.close();}else if(rawMsg){try{localStorage.setItem("cms-oauth-pending",rawMsg);}catch(e){}document.getElementById("msg").textContent="Close this window; the admin tab will complete sign-in.";}}if(!op&&rawMsg){send();}else{if(op)try{op.postMessage("authorizing:github","*");}catch(e){}setTimeout(function(){send("*");},500);}})();`;
  return {
    statusCode: 200,
    headers: {
      'Content-Type': 'text/html; charset=utf-8',
      'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';",
    },
    body: `<html><body><p id="msg">Completing sign-in…</p><script>window.CMS_OAUTH_MSG=${authMsgJson};<\/script><script>${script}<\/script><\/body><\/html>`,
  };
}
