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

  if (!code) {
    return htmlResponse('error', JSON.stringify({
      error: 'missing code',
      hint: 'GitHub did not send a code. Check the OAuth App callback URL matches exactly: ' + callbackUrl,
    }));
  }
  if (!clientId || !clientSecret) {
    return htmlResponse('error', JSON.stringify({
      error: 'missing env',
      hint: 'In Netlify, set CMS_GITHUB_CLIENT_ID and CMS_GITHUB_CLIENT_SECRET and scope them to Runtime/Functions (not only Build). Redeploy after changing.',
      clientIdSet: !!clientId,
      clientSecretSet: !!clientSecret,
    }));
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

function escapeForJsString(s) {
  return String(s)
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/<\/script/gi, '<\\/script');
}

function htmlResponse(message, content) {
  const authMsg = `authorization:github:${message}:${content}`;
  const authMsgEscaped = escapeForJsString(authMsg);
  // Decap netlify-auth expects event.data to be a string (calls .indexOf). Always send string via postMessage.
  const script = `(function(){var op=window.opener;var rawMsg=window.CMS_OAUTH_MSG;var done=false;function send(origin){if(done)return;done=true;var msg=typeof rawMsg==="string"?rawMsg:JSON.stringify(rawMsg);if(!msg)return;try{localStorage.setItem("cms-oauth-pending",msg);}catch(e){}if(op)try{op.postMessage(msg,origin||"*");}catch(e){}var isError=rawMsg.indexOf("authorization:github:error:")===0;if(isError){var esc=rawMsg.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");document.getElementById("msg").innerHTML="Sign-in failed. Copy this and fix the issue, then close this window:<br><pre style=\\"white-space:pre-wrap;font-size:12px;\\">"+esc+"</pre>";return;}document.getElementById("msg").textContent="Sign-in complete. Closing…";setTimeout(function(){window.close();},1500);}if(!op&&rawMsg){send();}else{if(op)try{op.postMessage("authorizing:github","*");}catch(e){}setTimeout(function(){send("*");},500);}})();`;
  return {
    statusCode: 200,
    headers: {
      'Content-Type': 'text/html; charset=utf-8',
      'Content-Security-Policy': "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';",
    },
    body: `<html><body><p id="msg">Completing sign-in…</p><script>window.CMS_OAUTH_MSG="${authMsgEscaped}";<\/script><script>${script}<\/script><\/body><\/html>`,
  };
}
