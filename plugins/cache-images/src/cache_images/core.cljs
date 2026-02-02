(ns cache-images.core
  "Netlify build plugin: cache public/assets/images in onPostBuild (before deploy)
   so the directory is still full when we save."
  (:require [cljs.nodejs :as nodejs]))

(def ^js fs (nodejs/require "fs"))
(def ^js path-module (nodejs/require "path"))

(def ^:const path "public/assets/images")

(defn count-files-recursive [dir]
  (if (or (not (.existsSync fs dir))
          (not (.isDirectory (.statSync fs dir))))
    0
    (let [names (array-seq (.readdirSync fs dir))
          reducer (fn [n name]
                    (let [full (.join path-module dir name)
                          stat (.statSync fs full)]
                      (+ n (if (.isDirectory stat)
                             (count-files-recursive full)
                             1))))]
      (reduce reducer 0 names))))

(defn ^:export onPreBuild [^js context]
  (let [utils (.-utils context)
        cache (.-cache utils)]
    (.then (.restore cache path)
           (fn [restored]
             (if restored
               (let [n (count-files-recursive path)]
                 (println (str "Successfully restored: " path " ... " n " files in total.")))
               (println (str "A cache of '" path "' doesn't exist (yet).")))))))

(defn ^:export onPostBuild [^js context]
  (let [utils (.-utils context)
        cache (.-cache utils)
        status (.-status utils)]
    (.then (.save cache path)
           (fn [saved]
             (if saved
               (let [n (count-files-recursive path)]
                 (println (str "Successfully cached: " path " ... " n " files in total."))
                 (.show status #js {:title (str n " files cached")
                                    :summary "Restored on the next build (saved before deploy)."
                                    :text path}))
               (println (str "Attempted to cache: " path " ... but failed (path may not exist).")))))))
