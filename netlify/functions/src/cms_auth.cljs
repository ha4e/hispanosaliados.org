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

(defn handler
  "Netlify function: redirect to GitHub OAuth authorize URL."
  [event]
  (let [env (.-env js/process)
        client-id (gobj/get env "CMS_GITHUB_CLIENT_ID")
        site-url (site-url event)
        callback (callback-url site-url)]
    (if (or (nil? client-id) (= "" client-id))
      (clj->js {:statusCode 500 :body "CMS_GITHUB_CLIENT_ID not set"})
      (let [crypto (js/require "crypto")
            state (.toString (.randomBytes crypto 16) "hex")
            params (doto (js/URLSearchParams.)
                    (.set "client_id" client-id)
                    (.set "redirect_uri" callback)
                    (.set "scope" "repo,user")
                    (.set "state" state))
            url (str "https://github.com/login/oauth/authorize?" (.toString params))]
        (clj->js {:statusCode 302
                  :headers (clj->js {"Location" url})
                  :body ""})))))
