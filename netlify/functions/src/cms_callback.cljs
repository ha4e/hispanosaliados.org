(ns cms-callback
  "Decap CMS OAuth: exchange code for token, return HTML that postMessages token to opener.
   GitHub OAuth App callback URL must be: https://YOUR_SITE/.netlify/functions/cms-callback
   Env: CMS_GITHUB_CLIENT_ID, CMS_GITHUB_CLIENT_SECRET."
  (:require [clojure.string :as str]
            [goog.object :as gobj]))

(defn- escape-for-json-script
  "Escape payload so it can be placed inside <script type=\"application/json\"> without closing the tag."
  [s]
  (str/replace (str s) #"(?i)</script" "<\\/script"))

(defn- escape-js-string-literal
  "Escape string for embedding inside a JavaScript double-quoted string literal."
  [s]
  (-> (str s)
      (str/replace #"\\" "\\\\")
      (str/replace #"\"" "\\\"")
      (str/replace #"\n" "\\n")
      (str/replace #"\r" "\\r")))

(def ^:private -msg-id "msg")
(def ^:private -data-id "cms-oauth-msg")
(def ^:private -no-response "No response. Close this window and try again.")
(def ^:private -error-prefix "authorization:github:error:")
(def ^:private -fail-html-prefix "Sign-in failed. Copy this and fix the issue, then close this window:<br><pre style=\"white-space:pre-wrap;font-size:12px;\">")
(def ^:private -fail-html-suffix "</pre>")
(def ^:private -authorizing "authorizing:github")
(def ^:private -target-origin "*")
(def ^:private -success-msg "Sign-in complete. You can close this window.")
(def ^:private -error-msg "Something went wrong. Close this window.")
(def ^:private -completing-msg "Completing sign-in…")

(defn- callback-page-script
  "Return the JavaScript source for the callback page inline script (run in browser)."
  []
  (let [q escape-js-string-literal]
    (str "(function(){"
         "var msgEl=document.getElementById(\"" (q -msg-id) "\");"
         "function done(t){if(msgEl)msgEl.textContent=t;}"
         "function run(){"
         "try{"
         "var el=document.getElementById(\"" (q -data-id) "\");"
         "var rawMsg=el&&el.textContent?el.textContent.trim():\"\";"
         "if(!rawMsg){done(\"" (q -no-response) "\");return;}"
         "var isError=rawMsg.indexOf(\"" (q -error-prefix) "\")===0;"
         "if(isError){"
         "var esc=rawMsg.replace(/&/g,\"&amp;\").replace(/</g,\"&lt;\").replace(/>/g,\"&gt;\");"
         "if(msgEl)msgEl.innerHTML=\"" (q -fail-html-prefix) "\"+esc+\"" (q -fail-html-suffix) "\";"
         "return;}"
         "if(window.opener){"
         "try{window.opener.postMessage(\"" (q -authorizing) "\",\"" (q -target-origin) "\");"
         "setTimeout(function(){try{window.opener.postMessage(rawMsg,\"" (q -target-origin) "\");}catch(e){}},400);"
         "}catch(e){}}"
         "done(\"" (q -success-msg) "\");"
         "}catch(e){done(\"" (q -error-msg) "\");}"
         "finally{"
         "if(msgEl&&msgEl.textContent===\"" (q -completing-msg) "\")msgEl.textContent=\"" (q -success-msg) "\";"
         "}}"
         "if(document.readyState===\"loading\")document.addEventListener(\"DOMContentLoaded\",run);else run();"
         "})();")))

(def ^:private page-script-cache (atom nil))

(defn- load-page-script
  "Load transpiled cms-callback-page from main.js (same dir as this function). Returns nil if not found."
  []
  (when (nil? @page-script-cache)
    (try
      (let [fs (js/require "fs")
            path (js/require "path")
            dir (.-dirname js/__dirname)
            script-path (.join path dir "main.js")]
        (reset! page-script-cache (.readFileSync fs script-path "utf8")))
      (catch :default _
        (reset! page-script-cache false))))
  (when (string? @page-script-cache) @page-script-cache))

(defn- inline-script []
  (or (load-page-script) (callback-page-script)))

(defn- html-response [message content]
  (let [auth-msg (str "authorization:github:" message ":" content)
        safe-payload (escape-for-json-script auth-msg)
        script (inline-script)
        body (str "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Signing in…</title></head><body><main><p id=\"" -msg-id "\">" -completing-msg "</p></main><script type=\"application/json\" id=\"" -data-id "\">"
                  safe-payload
                  "<\\/script><script>"
                  script
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
