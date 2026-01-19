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
  "Process markdown content to HTML"
  (if (empty? content)
    ""
    (-> content
        ;; Headers
        (str/replace #"^### (.+)$" "<h3>$1</h3>")
        (str/replace #"^## (.+)$" "<h2>$1</h2>")
        (str/replace #"^# (.+)$" "<h1>$1</h1>")
        ;; Bold and italic
        (str/replace #"\*\*(.+?)\*\*" "<strong>$1</strong>")
        (str/replace #"\*(.+?)\*" "<em>$1</em>")
        ;; Links
        (str/replace #"\[([^\]]+)\]\(([^\)]+)\)" "<a href=\"$2\">$1</a>")
        ;; Lists
        (str/replace #"^- (.+)$" "<li>$1</li>")
        ;; Paragraphs (split by double newlines)
        (str/split #"\n\n+")
        (->> (map (fn [para]
                    (if (or (str/starts-with? para "<h")
                            (str/starts-with? para "<li")
                            (str/starts-with? para "<ul")
                            (str/starts-with? para "<ol"))
                      para
                      (str "<p>" (str/trim para) "</p>"))))
             (str/join "\n")))))

(defn read-template [template-name]
  "Read HTML template file"
  (let [file (str "src/templates/" template-name ".html")]
    (when (.exists (io/file file))
      (slurp file))))

(defn render-template [template content-map]
  "Replace placeholders in template with content"
  (reduce-kv (fn [acc k v]
               (str/replace acc (re-pattern (str "\\{\\{" (name k) "\\}\\}")) (or (str v) "")))
             template
             content-map))

(defn compose-page [base-template page-content active-page]
  "Compose full page from base template and page content"
  (let [active-class (fn [page] (if (= page active-page) "active" ""))
        content-map {:content page-content
                     :active-home (active-class "home")
                     :active-about (active-class "about")
                     :active-programs (active-class "programs")
                     :active-impact (active-class "impact")
                     :active-get-involved (active-class "get-involved")
                     :active-donate (active-class "donate")
                     :active-contact (active-class "contact")}]
    (render-template base-template content-map)))

(defn copy-assets []
  "Copy static assets to public directory"
  (let [assets-dir "src/assets"
        public-assets "public/assets"]
    (when (.exists (io/file assets-dir))
      (fs/copy-tree assets-dir public-assets {:replace-existing true}))))

(defn ensure-dir [path]
  "Ensure directory exists"
  (.mkdirs (io/file path)))

(defn build-page [page-name base-template page-template content title]
  "Build a single HTML page"
  (let [output-dir "public"
        output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
    (let [page-html (if page-template
                      (render-template page-template {:content content})
                      content)
          full-html (compose-page base-template page-html page-name)]
      (spit output-file (str/replace full-html #"\{\{title\}\}" title))
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
  
  ;; Read base template
  (let [base-template (read-template "base")
        pages [["index" "home" "Home"]
               ["about" "about" "About Us"]
               ["programs" "programs" "Programs"]
               ["impact" "impact" "Impact"]
               ["get-involved" "get-involved" "Get Involved"]
               ["donate" "donate" "Donate"]
               ["contact" "contact" "Contact"]
               ["privacy" "privacy" "Privacy Policy"]]]
    (doseq [[page-name template-name title] pages]
      (let [page-template (read-template template-name)
            content-file (str "src/content/" template-name ".md")
            content (read-markdown content-file)
            processed-content (if content (process-markdown content) "")]
        (build-page page-name base-template page-template processed-content title)))))
  
  (println "Build complete!"))

(-main)
