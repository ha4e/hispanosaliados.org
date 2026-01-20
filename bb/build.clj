#!/usr/bin/env bb

(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[babashka.fs :as fs]
         '[babashka.process :as p])

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

(defn wrap-paragraph [text]
  (if (or (str/includes? text "<h")
          (str/includes? text "<li")
          (str/includes? text "<p>"))
    text
    (str "<p>" (str/trim text) "</p>")))

(defn wrap-lists
  "Wrap consecutive <li> elements in <ul> tags"
  [text]
  (let [lines (str/split-lines text)
        result (loop [remaining lines
                      acc []
                      in-list false
                      list-items []]
                 (if (empty? remaining)
                   (if (seq list-items)
                     (conj acc (str "<ul>\n" (str/join "\n" list-items) "\n</ul>"))
                     acc)
                   (let [line (first remaining)
                         is-list-item (str/starts-with? line "<li>")]
                     (cond
                       (and is-list-item (not in-list))
                       (recur (rest remaining) acc true [line])
                       (and is-list-item in-list)
                       (recur (rest remaining) acc true (conj list-items line))
                       (and (not is-list-item) in-list)
                       (recur remaining
                              (conj acc (str "<ul>\n" (str/join "\n" list-items) "\n</ul>") line)
                              false
                              [])
                       :else
                       (recur (rest remaining) (conj acc line) false [])))))]
    (str/join "\n" result)))

(defn process-markdown [content]
  (if (or (nil? content) (empty? content))
    ""
    (let [lines (str/split-lines content)
          processed (map process-markdown-line lines)
          joined (str/join "\n" processed)
          with-formatting (-> joined
                              (str/replace #"\*\*([^*]+)\*\*" "<strong>$1</strong>")
                              (str/replace #"\*([^*]+)\*" "<em>$1</em>")
                              (str/replace #"\[([^\]]+)\]\(([^\)]+)\)" "<a href=\"$2\">$1</a>"))
          with-lists (wrap-lists with-formatting)]
      (wrap-paragraph with-lists))))

(defn read-template [template-name]
  (let [file (str "src/templates/" template-name ".html")]
    (when (.exists (io/file file))
      (slurp file))))

(defn render-template [template content-map]
  (reduce-kv (fn [acc k v]
               (let [placeholder (str "{{" (name k) "}}")
                     replacement (str (or v ""))]
                 (str/replace acc placeholder replacement)))
             template
             content-map))

(defn compose-page [base-template page-content active-page title page-name]
  (let [active-class (fn [page] (if (= page active-page) "active" ""))
        canonical-path (if (= page-name "index") "/" (str "/" page-name ".html"))
        content-map {:content page-content
                     :title title
                     :canonical-path canonical-path
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

(defn copy-robots-txt []
  (let [robots-file "src/robots.txt"
        public-robots "public/robots.txt"]
    (when (.exists (io/file robots-file))
      (fs/copy robots-file public-robots {:replace-existing true}))))

(defn copy-redirects []
  (let [redirects-file "src/_redirects"
        public-redirects "public/_redirects"]
    (when (.exists (io/file redirects-file))
      (fs/copy redirects-file public-redirects {:replace-existing true}))))

(defn ensure-dir [path]
  (.mkdirs (io/file path)))

(defn generate-svg-favicon [size]
  "Generate SVG favicon with HA4E text - pure Clojure, no dependencies"
  (let [text "HA4E"
        ;; Smaller font size to ensure all text fits (40% of size)
        font-size (* size 0.4)
        ;; Brand color: red (#C8102E)
        text-color "#C8102E"]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
         "<svg width=\"" size "\" height=\"" size "\" viewBox=\"0 0 " size " " size "\" xmlns=\"http://www.w3.org/2000/svg\">\n"
         "  <rect width=\"" size "\" height=\"" size "\" fill=\"transparent\"/>\n"
         "  <text x=\"50%\" y=\"50%\" font-family=\"Arial, Helvetica, sans-serif\" "
         "font-size=\"" font-size "\" font-weight=\"bold\" "
         "fill=\"" text-color "\" "
         "text-anchor=\"middle\" dominant-baseline=\"central\" "
         "letter-spacing=\"-0.02em\">" text "</text>\n"
         "</svg>")))

(defn generate-favicons []
  "Generate favicon files with HA4E text - pure Clojure solution"
  (let [output-dir "src/assets/images"
        ;; Generate SVG favicon (works everywhere, modern browsers support it)
        svg-favicon (generate-svg-favicon 64)
        svg-path (str output-dir "/favicon.svg")]
    (ensure-dir output-dir)
    
    ;; Write SVG favicon
    (spit svg-path svg-favicon)
    (println "Generated:" svg-path)
    
    ;; For PNG favicons, try to use system tools if available
    ;; Otherwise, browsers will use the SVG
    (let [png-sizes [{:name "favicon.png" :size 64}
                     {:name "favicon-32x32.png" :size 32}
                     {:name "favicon-16x16.png" :size 16}]
          ;; Try to find image conversion tools
          convert-cmd (cond
                        ;; macOS - use sips
                        (and (= (System/getProperty "os.name") "Mac OS X")
                             (= (:exit @(p/process ["which" "sips"] {:out :string :err :string})) 0))
                        "sips"
                        ;; Linux/Unix - use convert (ImageMagick)
                        (= (:exit @(p/process ["which" "convert"] {:out :string :err :string})) 0)
                        "convert"
                        :else nil)]
      
      (if convert-cmd
        (do
          ;; Generate PNGs using system tool
          (doseq [{:keys [name size]} png-sizes]
            (let [output-path (str output-dir "/" name)]
              (try
                (if (= convert-cmd "sips")
                  ;; macOS sips
                  (let [result @(p/process ["sips" "-s" "format" "png" 
                                            "-z" (str size) (str size)
                                            svg-path "--out" output-path]
                                           {:dir (System/getProperty "user.dir")})]
                    (if (= (:exit result) 0)
                      (println "Generated:" output-path (str "(" size "x" size ")"))
                      (println "Warning: Failed to generate" output-path)))
                  ;; ImageMagick convert
                  (let [result @(p/process ["convert" "-background" "transparent"
                                            "-size" (str size "x" size)
                                            svg-path output-path]
                                           {:dir (System/getProperty "user.dir")})]
                    (if (= (:exit result) 0)
                      (println "Generated:" output-path (str "(" size "x" size ")"))
                      (println "Warning: Failed to generate" output-path))))
                (catch Exception e
                  (println "Warning: Could not generate" output-path ":" (.getMessage e))))))
          true)
        (do
          ;; No conversion tool available - just use SVG
          (println "Note: No image conversion tool found. Using SVG favicon only.")
          (println "Modern browsers support SVG favicons. For PNG fallbacks, install ImageMagick or use macOS sips.")
          true)))))

(defn build-page [page-name base-template page-template content title]
  (let [output-dir "public"
        output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
      (let [page-html (if page-template
                      (render-template page-template {:content content})
                      content)
          full-html (compose-page base-template page-html page-name title page-name)]
      (spit output-file full-html)
      (println "Built:" output-file))))

;; Main build execution
(println "Building HA4E website...")

(when (.exists (io/file "public"))
  (fs/delete-tree "public"))
(ensure-dir "public")

(generate-favicons)
(println "Favicons generated")
(copy-assets)
(println "Assets copied")
(copy-robots-txt)
(copy-redirects)

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
      (build-page page-name base-template page-template processed-content title))))

(println "Build complete!")
