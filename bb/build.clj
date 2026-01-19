#!/usr/bin/env bb

(ns build
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(defn read-markdown [file]
  (when (.exists (io/file file))
    (slurp file)))

(defn process-markdown-line [line]
  (cond
    (str/starts-with? line "### ") (str "<h3>" (subs line 4) "</h3>")
    (str/starts-with? line "## ") (str "<h2>" (subs line 3) "</h2>")
    (str/starts-with? line "# ") (str "<h1>" (subs line 2) "</h1>")
    (str/starts-with? line "- ") (str "<li>" (subs line 2) "</li>")
    :else line))

(defn process-markdown [content]
  (if (or (nil? content) (empty? content))
    ""
    (let [lines (str/split-lines content)
          processed (map process-markdown-line lines)
          joined (str/join "\n" processed)]
      (-> joined
          ;; Bold
          (str/replace #"\*\*([^*]+)\*\*" "<strong>$1</strong>")
          ;; Italic
          (str/replace #"\*([^*]+)\*" "<em>$1</em>")
          ;; Links
          (str/replace #"\[([^\]]+)\]\(([^\)]+)\)" "<a href=\"$2\">$1</a>")
          ;; Wrap consecutive non-header/list items in paragraphs
          (str/replace #"((?:<p>)?[^<]+(?:</p>)?)" 
                       (fn [match]
                         (if (or (str/includes? match "<h")
                                 (str/includes? match "<li")
                                 (str/includes? match "<p>"))
                           match
                           (str "<p>" (str/trim match) "</p>")))))))

(defn read-template [template-name]
  (let [file (str "src/templates/" template-name ".html")]
    (when (.exists (io/file file))
      (slurp file))))

(defn render-template [template content-map]
  (reduce-kv (fn [acc k v]
               (str/replace acc (re-pattern (str "\\{\\{" (name k) "\\}\\}")) (or (str v) "")))
             template
             content-map))

(defn compose-page [base-template page-content active-page title]
  (let [active-class (fn [page] (if (= page active-page) "active" ""))
        content-map {:content page-content
                     :title title
                     :active-home (active-class "home")
                     :active-about (active-class "about")
                     :active-programs (active-class "programs")
                     :active-impact (active-class "impact")
                     :active-get-involved (active-class "get-involved")
                     :active-donate (active-class "donate")
                     :active-contact (active-class "contact")}]
    (render-template base-template content-map)))

(defn copy-assets []
  (let [assets-dir "src/assets"
        public-assets "public/assets"]
    (when (.exists (io/file assets-dir))
      (fs/copy-tree assets-dir public-assets {:replace-existing true}))))

(defn ensure-dir [path]
  (.mkdirs (io/file path)))

(defn build-page [page-name base-template page-template content title]
  (let [output-dir "public"
        output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
    (let [page-html (if page-template
                      (render-template page-template {:content content})
                      content)
          full-html (compose-page base-template page-html page-name title)]
      (spit output-file full-html)
      (println "Built:" output-file))))

(defn -main []
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
               ["contact-success" "contact-success" "Thank You"]
               ["privacy" "privacy" "Privacy Policy"]]]
    (doseq [[page-name template-name title] pages]
      (let [page-template (read-template template-name)
            content-file (str "src/content/" template-name ".md")
            content (read-markdown content-file)
            processed-content (if content (process-markdown content) "")]
        (build-page page-name base-template page-template processed-content title)))))
  
  (println "Build complete!"))

(-main)
