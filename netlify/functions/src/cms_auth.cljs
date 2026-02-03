(ns cms-auth
  "Decap CMS OAuth: redirect to GitHub authorize URL. Env: CMS_GITHUB_CLIENT_ID."
  (:require [clojure.string :as str]
            [goog.object :as gobj]))

(defn- site-url [event]
  (let [env (.-env js/process)
        url (gobj/get env "URL")]
    (if (and url (not= url ""))
      url
      (let [headers (.-headers event)
            proto (gobj/get headers "x-forwarded-proto")
            host (gobj/get headers "x-forwarded-host")]
        (str proto "://" host)))))

(defn- callback-url [site-url]
  (str (str/replace (str site-url) #"/$" "") "/.netlify/functions/cms-callback"))

(defn- ok-response [status-code headers body]
  ;; Return a plain JS object so Netlify always sees a valid statusCode (avoids "status code 0").
  #js {:statusCode (int status-code)
       :headers (clj->js headers)
       :body (str body)})

(defn handler
  "Netlify function: redirect to GitHub OAuth authorize URL.
   Returns legacy Lambda shape so Netlify decodes the response correctly."
  [event]
  (try
    (let [env (.-env js/process)
          client-id (gobj/get env "CMS_GITHUB_CLIENT_ID")
          site-url (site-url event)
          callback (callback-url site-url)]
      (if (or (nil? client-id) (= "" client-id))
        (ok-response 500 {} "CMS_GITHUB_CLIENT_ID not set")
        (let [crypto (js/require "crypto")
              state (.toString (.randomBytes crypto 16) "hex")
              params (doto (js/URLSearchParams.)
                      (.set "client_id" client-id)
                      (.set "redirect_uri" callback)
                      (.set "scope" "repo,user")
                      (.set "state" state))
              url (str "https://github.com/login/oauth/authorize?" (.toString params))]
          (ok-response 302 {"Location" url} ""))))
    (catch js/Error e
      (ok-response 500 {} (str "cms-auth error: " (.-message e))))))
