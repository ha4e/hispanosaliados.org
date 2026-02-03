(ns cms-callback
  "Decap CMS OAuth: exchange code for token, return HTML that postMessages token to opener.
   GitHub OAuth App callback URL must be: https://YOUR_SITE/.netlify/functions/cms-callback
   Env: CMS_GITHUB_CLIENT_ID, CMS_GITHUB_CLIENT_SECRET."
  (:require [clojure.string :as str]
            [goog.object :as gobj]))

(defn- escape-for-json-script [s]
  "Escape payload so it can be placed inside <script type=\"application/json\"> without closing the tag."
  (str/replace (str s) #"(?i)</script" "<\\/script"))

(def ^:private inline-script
  "(function(){setTimeout(function(){var el=document.getElementById(\"cms-oauth-msg\");var rawMsg=el?el.textContent:null;if(!rawMsg||!rawMsg.trim()){document.getElementById(\"msg\").textContent=\"No response. Close this window and try again.\";return;}var msg=rawMsg.trim();var isError=msg.indexOf(\"authorization:github:error:\")===0;if(isError){var esc=msg.replace(/&/g,\"&amp;\").replace(/</g,\"&lt;\").replace(/>/g,\"&gt;\");document.getElementById(\"msg\").innerHTML=\"Sign-in failed. Copy this and fix the issue, then close this window:<br><pre style=\\\"white-space:pre-wrap;font-size:12px;\\\">\"+esc+\"</pre>\";return;}if(window.opener){try{window.opener.postMessage(\"authorizing:github\",\"*\");setTimeout(function(){try{window.opener.postMessage(msg,\"*\");}catch(e){}},400);}catch(e){}}document.getElementById(\"msg\").textContent=\"Sign-in complete. You can close this window.\";},100);})();")

(defn- html-response [message content]
  (let [auth-msg (str "authorization:github:" message ":" content)
        safe-payload (escape-for-json-script auth-msg)
        body (str "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Signing in…</title></head><body><main><p id=\"msg\">Completing sign-in…</p></main><script type=\"application/json\" id=\"cms-oauth-msg\">"
                  safe-payload
                  "<\\/script><script>"
                  inline-script
                  "<\\/script></body></html>")]
    (clj->js {:statusCode 200
              :headers (clj->js {"Content-Type" "text/html; charset=utf-8"
                                 "Content-Security-Policy" "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline';"
                                 "Cross-Origin-Opener-Policy" "unsafe-none"})
              :body body})))

(defn- site-url [event]
  (let [env (.-env js/process)
        url (gobj/get env "URL")]
    (if (and url (not= url ""))
      url
      (let [headers (gobj/get event "headers")
            proto (gobj/get headers "x-forwarded-proto")
            host (gobj/get headers "x-forwarded-host")]
        (str proto "://" host)))))

(defn- callback-url [site-url]
  (str (str/replace (str site-url) #"/$" "") "/.netlify/functions/cms-callback"))

(defn handler
  "Netlify function: exchange GitHub OAuth code for token, return HTML that postMessages to opener."
  [event]
  (let [q (gobj/get event "queryStringParameters")
        code (when q (gobj/get q "code"))
        env (.-env js/process)
        client-id (gobj/get env "CMS_GITHUB_CLIENT_ID")
        client-secret (gobj/get env "CMS_GITHUB_CLIENT_SECRET")
        site-url (site-url event)
        callback (callback-url site-url)]
    (cond
      (nil? code)
      (html-response "error"
                    (js/JSON.stringify (clj->js {:error "missing code"
                                                 :hint (str "GitHub did not send a code. Check the OAuth App callback URL matches exactly: " callback)})))

      (or (nil? client-id) (= "" client-id) (nil? client-secret) (= "" client-secret))
      (let [hint (if (or (nil? client-secret) (= "" client-secret))
                   "CMS_GITHUB_CLIENT_SECRET is not visible to the function. In Netlify → Environment variables: ensure it is scoped to Runtime/Functions, has a value for this deploy context (Production/Previews), and trigger a new deploy after saving."
                   "CMS_GITHUB_CLIENT_ID is not visible. Set it in Netlify → Environment variables, scope to Runtime/Functions, then redeploy.")]
        (html-response "error"
                       (js/JSON.stringify (clj->js {:error "missing env"
                                                    :hint hint
                                                    :clientIdSet (boolean client-id)
                                                    :clientSecretSet (boolean client-secret)}))))

      :else
      (js/Promise.
       (fn [resolve _reject]
         (let [params (doto (js/URLSearchParams.)
                        (.set "client_id" client-id)
                        (.set "client_secret" client-secret)
                        (.set "code" code)
                        (.set "redirect_uri" callback))
               opts #js {:method "POST"
                         :headers #js {"Accept" "application/json"
                                       "Content-Type" "application/x-www-form-urlencoded"}
                         :body (.toString params)}]
           (-> (js/fetch "https://github.com/login/oauth/access_token" opts)
               (.then (fn [res] (.json res)))
               (.then (fn [data]
                        (if (gobj/get data "error")
                          (resolve (html-response "error" (js/JSON.stringify data)))
                          (let [token (gobj/get data "access_token")
                                content (js/JSON.stringify (clj->js {:token token :provider "github"}))]
                            (resolve (html-response "success" content))))))
               (.catch (fn [err]
                         (resolve (html-response "error"
                                                 (js/JSON.stringify (clj->js {:error (.-message err)})))))))))))))
