#!/usr/bin/env bb

(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
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

(def ^:private givebutter-script
  "<!-- GiveButter donation widget (loaded only on Donate page) -->\n    <script src=\"https://widgets.givebutter.com/latest.umd.cjs?acct=tU1dQXJcQXXOpiB7&p=other\"></script>")

(defn compose-page [base-template page-content active-page title page-name]
  (let [active-class (fn [page] (if (= page active-page) "active" ""))
        canonical-path (if (= page-name "index") "/" (str "/" page-name ".html"))
        head-extra (if (= page-name "donate") givebutter-script "")
        ;; Use Netlify's official status badge only when SITE_ID is set (on Netlify); otherwise text link only (no unofficial graphic)
        site-id (System/getenv "SITE_ID")
        netlify-badge-content (if (and site-id (not (str/blank? (str/trim site-id))))
                                (str "<img src=\"https://api.netlify.com/api/v1/badges/" (str/trim site-id) "/deploy-status\" alt=\"Deploys by Netlify\" class=\"netlify-badge\" loading=\"lazy\">")
                                "Deploys by Netlify")
        content-map {:content page-content
                     :title title
                     :canonical-path canonical-path
                     :head-extra head-extra
                     :netlify-badge-content netlify-badge-content
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

(defn copy-static-file [src-path public-path]
  (when (.exists (io/file src-path))
    (fs/copy src-path public-path {:replace-existing true})))

(defn copy-robots-txt []
  (copy-static-file "src/robots.txt" "public/robots.txt"))

(defn copy-redirects []
  (copy-static-file "src/_redirects" "public/_redirects"))

(defn copy-headers []
  (copy-static-file "src/_headers" "public/_headers"))

(defn minify-assets []
  "Optional: minify CSS and JS when npx/csso-cli and npx/terser are available."
  (let [css-src "public/assets/css/style.css"
        js-src "public/assets/js/main.js"]
    (when (and (.exists (io/file css-src)) (.exists (io/file js-src)))
      (let [css-result (p/process ["npx" "--yes" "csso-cli" css-src "-o" css-src]
                                 {:dir (System/getProperty "user.dir")
                                  :out :string :err :string})]
        (if (= (:exit @css-result) 0)
          (println "Minified CSS")
          (println "Note: CSS minification skipped (npx csso-cli not available or failed).")))
      (let [js-result (p/process ["npx" "--yes" "terser" js-src "-o" js-src "-c" "-m"]
                                 {:dir (System/getProperty "user.dir")
                                  :out :string :err :string})]
        (if (= (:exit @js-result) 0)
          (println "Minified JS")
          (println "Note: JS minification skipped (npx terser not available or failed)."))))))

(defn skip-webp? [path]
  (let [name (fs/file-name path)]
    (or (str/includes? (str/lower-case name) "favicon")
        (str/ends-with? (str/lower-case name) "-16x16.png")
        (str/ends-with? (str/lower-case name) "-32x32.png"))))

;; Photos that use full-size .webp/.avif in templates (no srcset); others use responsive -480w/-800w/-1200w only.
(def ^:private photos-need-full-size #{"programs-high-school-outreach-presentation"
                                       "about-community-empowerment"
                                       "impact-student-mentorship"
                                       "home-core-pillar-scholarships"
                                       "get-involved-volunteer-mentorship"})

(defn skip-full-size-webp? [path]
  "Skip full-size WebP/AVIF for photos that only use responsive srcset in templates."
  (when (str/includes? path "/photos/")
    (let [base (str/replace (fs/file-name path) #"(?i)\.(png|jpe?g)$" "")]
      (not (contains? photos-need-full-size base)))))

(defn- source-modified-time
  "Last modified time (ms) for path: use git log if in repo (stable across checkout), else file mtime."
  [src-path]
  (let [f (io/file src-path)]
    (when (.exists f)
      (let [r (p/process ["git" "log" "-1" "--format=%ct" "--" src-path]
                        {:dir (System/getProperty "user.dir") :out :string :err :string})
            s (when (= (:exit @r) 0) (str/trim (or (:out @r) "")))]
        (if (seq s)
          (try (* 1000 (Long/parseLong s)) (catch Exception _ (.lastModified f)))
          (.lastModified f))))))

(defn generate-webp []
  "Generate WebP versions of PNG/JPG under public/assets/images when sharp-cli is available."
  (let [images-dir (io/file "public/assets/images")
        ext? (fn [f ext]
               (str/ends-with? (str/lower-case (.getName f)) (str "." ext)))]
    (when (.isDirectory images-dir)
      (let [files (->> (file-seq images-dir)
                      (filter #(.isFile %))
                      (filter (fn [f]
                                (let [p (.getPath f)]
                                  (and (not (skip-webp? p))
                                       (not (skip-full-size-webp? p))
                                       (or (ext? f "png") (ext? f "jpg") (ext? f "jpeg")))))))]
        (if (empty? files)
          (println "No PNG/JPG images to convert to WebP (or only favicons / responsive-only photos).")
          (let [ok (atom 0)
                ;; Use git last-commit time for source so cached outputs skip after checkout (fs mtime changes).
                need-generate? (fn [public-path out-path]
                                 (let [src-path (str/replace public-path #"^public/" "src/")
                                       src-time (or (source-modified-time src-path)
                                                    (.lastModified (io/file src-path)))
                                       out-f (io/file out-path)]
                                   (or (not (.exists out-f))
                                       (nil? src-time)
                                       (> src-time (.lastModified out-f)))))]
            (doseq [f files]
              (let [path (.getPath f)
                    out (str/replace path #"(?i)\.(png|jpe?g)$" ".webp")]
                (when (and (not= path out) (need-generate? path out))
                  (let [result (p/process ["npx" "--yes" "sharp-cli" "--input" path "--output" out "--format" "webp" "--quality" "85"]
                                         {:dir (System/getProperty "user.dir")
                                          :out :string :err :string})]
                    (if (= (:exit @result) 0)
                      (do (swap! ok inc) (println "WebP:" out))
                      (println "Note: WebP skipped for" path ":" (:err @result)))))))
            (when (> @ok 0)
              (println "Generated" @ok "WebP image(s). Use <picture> in templates to serve them (see docs/performance.md)."))))))))

(defn generate-avif []
  "Generate AVIF versions of PNG/JPG under public/assets/images when sharp-cli supports it."
  (let [images-dir (io/file "public/assets/images")
        ext? (fn [f ext]
               (str/ends-with? (str/lower-case (.getName f)) (str "." ext)))]
    (when (.isDirectory images-dir)
      (let [files (->> (file-seq images-dir)
                      (filter #(.isFile %))
                      (filter (fn [f]
                                (let [p (.getPath f)]
                                  (and (not (skip-webp? p))
                                       (not (skip-full-size-webp? p))
                                       (or (ext? f "png") (ext? f "jpg") (ext? f "jpeg")))))))]
        (if (empty? files)
          (println "No PNG/JPG images to convert to AVIF (or only favicons / responsive-only photos).")
          (let [ok (atom 0)
                need-generate? (fn [public-path out-path]
                                 (let [src-path (str/replace public-path #"^public/" "src/")
                                       src-time (or (source-modified-time src-path)
                                                    (.lastModified (io/file src-path)))
                                       out-f (io/file out-path)]
                                   (or (not (.exists out-f))
                                       (nil? src-time)
                                       (> src-time (.lastModified out-f)))))]
            (doseq [f files]
              (let [path (.getPath f)
                    out (str/replace path #"(?i)\.(png|jpe?g)$" ".avif")]
                (when (and (not= path out) (need-generate? path out))
                  (let [result (p/process ["npx" "--yes" "sharp-cli" "--input" path "--output" out "--format" "avif" "--quality" "70"]
                                         {:dir (System/getProperty "user.dir")
                                          :out :string :err :string})]
                    (if (= (:exit @result) 0)
                      (do (swap! ok inc) (println "AVIF:" out))
                      (println "Note: AVIF skipped for" path ":" (:err @result)))))))
            (when (> @ok 0)
              (println "Generated" @ok "AVIF image(s). Add <source type=\"image/avif\"> before WebP in <picture> (see docs/performance.md)."))))))))

(def ^:private responsive-widths [480 800 1200])
(def ^:private photos-src-dir "src/assets/images/photos")

(defn generate-responsive []
  "Generate multi-width variants (480, 800, 1200) for content photos for srcset/sizes."
  (let [src-dir (io/file photos-src-dir)
        out-dir "public/assets/images/photos"]
    (when (and (.exists src-dir) (.isDirectory src-dir))
      (let [files (->> (file-seq src-dir)
                      (filter #(.isFile %))
                      (filter (fn [f]
                                (let [p (.getPath f)
                                      name (str/lower-case (.getName f))]
                                  (and (not (skip-webp? p))
                                       (or (str/ends-with? name ".png")
                                           (str/ends-with? name ".jpg")
                                           (str/ends-with? name ".jpeg")))))))]
        (if (empty? files)
          (println "No content photos to generate responsive variants.")
          (let [ok (atom 0)
                need? (fn [src-path out-path]
                        (let [src-f (io/file src-path)
                              out-f (io/file out-path)]
                          (or (not (.exists out-f))
                              (> (.lastModified src-f) (.lastModified out-f)))))
                base-name (fn [path] (str/replace path #"(?i)\.(png|jpe?g)$" ""))]
            (.mkdirs (io/file out-dir))
            (doseq [f files]
              (let [src-path (.getPath f)
                    name-no-ext (fs/file-name (base-name src-path))]
                (doseq [w responsive-widths]
                  (let [suffix (str "-" w "w")
                        out-png (str out-dir "/" name-no-ext suffix ".png")
                        out-webp (str out-dir "/" name-no-ext suffix ".webp")
                        out-avif (str out-dir "/" name-no-ext suffix ".avif")]
                    (when (need? src-path out-png)
                      (let [r (p/process ["npx" "--yes" "sharp-cli" "--input" src-path "--output" out-png "resize" (str w)]
                                         {:dir (System/getProperty "user.dir") :out :string :err :string})]
                        (when (= (:exit @r) 0) (swap! ok inc) (println "Responsive PNG:" out-png))))
                    (when (need? src-path out-webp)
                      (let [r (p/process ["npx" "--yes" "sharp-cli" "--input" src-path "--output" out-webp "resize" (str w) "--format" "webp" "--quality" "85"]
                                         {:dir (System/getProperty "user.dir") :out :string :err :string})]
                        (when (= (:exit @r) 0) (swap! ok inc) (println "Responsive WebP:" out-webp))))
                    (when (need? src-path out-avif)
                      (let [r (p/process ["npx" "--yes" "sharp-cli" "--input" src-path "--output" out-avif "resize" (str w) "--format" "avif" "--quality" "70"]
                                         {:dir (System/getProperty "user.dir") :out :string :err :string})]
                        (when (= (:exit @r) 0) (swap! ok inc) (println "Responsive AVIF:" out-avif))))))))
            (when (> @ok 0)
              (println "Generated" @ok "responsive image(s). Use srcset/sizes in templates (see docs/performance.md)."))))))))

(defn ensure-dir [path]
  (.mkdirs (io/file path)))

(defn generate-svg-favicon [size]
  "Generate SVG favicon with HA (blue) and 4E (red) text on white background"
  (let [text-ha "HA"
        text-4e "4E"
        ;; Smaller font size to ensure all text fits (40% of size)
        font-size (* size 0.4)
        ;; Brand colors
        color-blue "#1E3A5F"
        color-red "#C41E3A"
        color-white "#FFFFFF"]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
         "<svg width=\"" size "\" height=\"" size "\" viewBox=\"0 0 " size " " size "\" xmlns=\"http://www.w3.org/2000/svg\">\n"
         "  <!-- White background -->\n"
         "  <rect width=\"" size "\" height=\"" size "\" fill=\"" color-white "\" rx=\"2\"/>\n"
         "  <!-- HA4E with HA in blue and 4E in red -->\n"
         "  <text x=\"50%\" y=\"50%\" font-family=\"Arial, Helvetica, sans-serif\" "
         "font-size=\"" font-size "\" font-weight=\"bold\" "
         "text-anchor=\"middle\" dominant-baseline=\"central\" "
         "letter-spacing=\"-0.02em\">\n"
         "    <tspan fill=\"" color-blue "\">" text-ha "</tspan>"
         "<tspan fill=\"" color-red "\">" text-4e "</tspan>\n"
         "  </text>\n"
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

(defn read-spotlights []
  (let [f (io/file "src/content/spotlights.edn")]
    (when (.exists f)
      (try (edn/read-string (slurp f))
           (catch Exception _ nil)))))

(def ^:private max-previous-spotlights 6)

(defn- escape-html [s]
  (when s (str/replace (str s) #"<" "&lt;")))

(defn- spotlight-name-html [name url]
  (let [safe-name (escape-html name)]
    (if (and url (seq (str/trim url)))
      (str "<a href=\"" (str/replace (str/trim url) #"\"" "&quot;") "\" rel=\"noopener noreferrer\" target=\"_blank\" class=\"spotlight-name-link\">" safe-name "</a>")
      safe-name)))

(defn render-spotlight-section []
  (let [data (read-spotlights)
        current (:current data)
        previous (take max-previous-spotlights (:previous data))]
    (if (empty? current)
      ""
      (let [blurb-html (process-markdown (:blurb current))
            img-path (:image current)
            img-avif (str/replace img-path #"(?i)\.(png|jpe?g)$" ".avif")
            img-webp (str/replace img-path #"(?i)\.(png|jpe?g)$" ".webp")
            img-html (str "<picture>"
                         "<source srcset=\"" img-avif "\" type=\"image/avif\">"
                         "<source srcset=\"" img-webp "\" type=\"image/webp\">"
                         "<img src=\"" img-path "\" alt=\"\" class=\"spotlight-photo\" width=\"200\" height=\"200\" loading=\"lazy\">"
                         "</picture>")
            name-html (spotlight-name-html (:name current) (:url current))
            current-html (str "<div class=\"spotlight-current\">"
                              "<div class=\"spotlight-photo-wrap\">"
                              img-html
                              "</div>"
                              "<div class=\"spotlight-details\">"
                              "<p class=\"spotlight-date\"><em>" (:month current) " " (:year current) "</em></p>"
                              "<h3 class=\"spotlight-name\">" name-html "</h3>"
                              "<p class=\"spotlight-role\">" (escape-html (:role current)) "</p>"
                              "<div class=\"spotlight-blurb\">" blurb-html "</div>"
                              "</div></div>")
            previous-html (if (empty? previous)
                          ""
                          (str "<div class=\"spotlight-previous\">"
                               "<p class=\"spotlight-previous-title\">Previous spotlights</p>"
                               "<ul class=\"spotlight-previous-list\" role=\"list\">"
                               (str/join (for [p previous]
                                           (let [name-html (spotlight-name-html (:name p) (:url p))
                                                 date (str (:month p) " " (:year p))
                                                 role (some-> (:role p) str (str/replace #"<" "&lt;"))
                                                 label (if (seq role) (str date " – " role) date)
                                                 img-path (:image p)
                                                 img-html (when (seq img-path)
                                                            (str "<img src=\"" img-path "\" alt=\"\" class=\"spotlight-previous-photo\" width=\"48\" height=\"48\" loading=\"lazy\">"))]
                                             (str "<li class=\"spotlight-previous-item\">"
                                                  (when img-html (str img-html " "))
                                                  "<span class=\"spotlight-previous-text\">" name-html " <span class=\"spotlight-previous-date\">– " label "</span></span></li>"))))
                               "</ul></div>"))]
        (str "<section class=\"volunteer-spotlight\" aria-labelledby=\"spotlight-heading\">"
             "<div class=\"container\">"
             "<h2 id=\"spotlight-heading\">Volunteer Spotlight</h2>"
             "<div class=\"section-divider\"></div>"
             current-html
             previous-html
             "</div></section>")))))

(defn build-page [page-name base-template page-template content-map title]
  (let [output-dir "public"
        output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
    (let [page-html (if page-template
                      (render-template page-template content-map)
                      (:content content-map))
          full-html (compose-page base-template page-html page-name title page-name)]
      (spit output-file full-html)
      (println "Built:" output-file))))

;; Main build execution
(println "Building HA4E website...")

;; Keep public/ so WebP/AVIF from previous build can be skipped when src images unchanged.
(ensure-dir "public")

(generate-favicons)
(println "Favicons generated")
(copy-assets)
(println "Assets copied")
(generate-responsive)
(generate-webp)
(generate-avif)
(minify-assets)
(copy-robots-txt)
(copy-redirects)
(copy-headers)

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
          processed-content (if content (process-markdown content) "")
          content-map (cond-> {:content processed-content}
                        (= page-name "get-involved") (merge {:spotlight-section (render-spotlight-section)}))]
      (build-page page-name base-template page-template content-map title))))

(println "Build complete!")
