#!/usr/bin/env bb

(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(defn read-markdown [file]
  "Read markdown file and return content as string"
  (when (.exists (io/file file))
    (slurp file)))

(defn process-markdown [content]
  "Process markdown content (basic implementation - can be enhanced)"
  (-> content
      (str/replace #"# (.+)" "<h1>$1</h1>")
      (str/replace #"## (.+)" "<h2>$1</h2>")
      (str/replace #"### (.+)" "<h3>$1</h3>")
      (str/replace #"\*\*(.+?)\*\*" "<strong>$1</strong>")
      (str/replace #"\*(.+?)\*" "<em>$1</em>")
      (str/replace #"\n\n" "</p><p>")
      (str/replace #"^(.+)$" "<p>$1</p>")))

(defn read-template [template-name]
  "Read HTML template file"
  (read-markdown (str "src/templates/" template-name ".html")))

(defn render-template [template content-map]
  "Replace placeholders in template with content"
  (reduce-kv (fn [acc k v]
               (str/replace acc (re-pattern (str "\\{\\{" (name k) "\\}\\}")) (str v)))
             template
             content-map))

(defn copy-assets []
  "Copy static assets to public directory"
  (let [assets-dir "src/assets"
        public-assets "public/assets"]
    (when (.exists (io/file assets-dir))
      (fs/copy-tree assets-dir public-assets {:replace-existing true}))))

(defn ensure-dir [path]
  "Ensure directory exists"
  (.mkdirs (io/file path)))

(defn build-page [page-name template content]
  "Build a single HTML page"
  (let [output-dir "public"
        output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
    (let [html (render-template template content)]
      (spit output-file html)
      (println "Built:" output-file))))

(defn -main []
  "Main build function"
  (println "Building HA4E website...")
  
  ;; Clean public directory
  (when (.exists (io/file "public"))
    (fs/delete-tree "public"))
  (ensure-dir "public")
  
  ;; Copy assets
  (copy-assets)
  (println "Assets copied")
  
  ;; Build pages
  (let [pages ["index" "about" "programs" "impact" "get-involved" "donate" "contact" "privacy"]]
    (doseq [page pages]
      (let [template (read-template (if (= page "index") "home" page))
            content-file (str "src/content/" (if (= page "index") "home" page) ".md")
            content (read-markdown content-file)
            processed-content (if content (process-markdown content) "")]
        (build-page page template {:content processed-content
                                   :title (str/capitalize (if (= page "index") "Home" page))
                                   :page-name page}))))
  
  (println "Build complete!"))

(-main)
