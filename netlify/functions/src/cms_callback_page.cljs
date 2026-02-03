(ns cms-callback-page
  "Runs in the browser on the OAuth callback page. Reads payload from #cms-oauth-msg,
   postMessages to opener, and updates #msg. Built separately and inlined by cms-callback."
  (:require [clojure.string :as str]))

(def ^:private msg-id "msg")
(def ^:private data-id "cms-oauth-msg")
(def ^:private no-response "No response. Close this window and try again.")
(def ^:private error-prefix "authorization:github:error:")
(def ^:private fail-html-prefix "Sign-in failed. Copy this and fix the issue, then close this window:<br><pre style=\"white-space:pre-wrap;font-size:12px;\">")
(def ^:private fail-html-suffix "</pre>")
(def ^:private authorizing "authorizing:github")
(def ^:private target-origin "*")
(def ^:private success-msg "Sign-in complete. You can close this window.")
(def ^:private error-msg "Something went wrong. Close this window.")
(def ^:private completing-msg "Completing sign-in…")

(defn- escape-html [s]
  (-> (str s)
      (str/replace #"&" "&amp;")
      (str/replace #"<" "&lt;")
      (str/replace #">" "&gt;")))

(defn- run-inner []
  (let [msg-el (.getElementById js/document msg-id)
        done (fn [t]
               (when msg-el
                 (set! (.-textContent msg-el) t)))]
    (try
      (let [el (.getElementById js/document data-id)
            raw (when el (.-textContent el))
            raw-msg (when raw (str/trim raw))]
        (if (or (nil? raw-msg) (empty? raw-msg))
          (done no-response)
          (let [is-error (zero? (.indexOf raw-msg error-prefix))]
            (if is-error
              (when msg-el
                (set! (.-innerHTML msg-el)
                      (str fail-html-prefix (escape-html raw-msg) fail-html-suffix)))
              (do
                (when (.-opener js/window)
                  (try
                    (.postMessage (.-opener js/window) authorizing target-origin)
                    (js/setTimeout
                     (fn []
                       (try
                         (.postMessage (.-opener js/window) raw-msg target-origin)
                         (catch :default _)))
                     400)
                    (catch :default _)))
                (done success-msg))))))
      (catch :default _
        (done error-msg))
      (finally
        (when (and msg-el (= completing-msg (.-textContent msg-el)))
          (set! (.-textContent msg-el) success-msg))))))

(defn ^:export run
  "Entry point for the callback page script. Called by shadow-cljs :init-fn when script loads."
  []
  (if (= "loading" (.-readyState js/document))
    (.addEventListener js/document "DOMContentLoaded" run-inner)
    (run-inner)))
