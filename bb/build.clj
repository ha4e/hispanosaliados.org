#!/usr/bin/env bb

(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[babashka.json :as json]
         '[babashka.fs :as fs]
         '[babashka.process :as p])

(defn strip-frontmatter
  "If content has YAML frontmatter (starts with ---), return only the body. Else return content."
  [content]
  (if (and content (str/starts-with? (str/trim content) "---"))
    (let [trimmed (str/trim content)
          after-first (subs trimmed 3)
          close-idx (str/index-of after-first "\n---")]
      (if close-idx
        (str/trim (subs after-first (+ close-idx 4)))
        content))
    content))

(defn read-markdown [file]
  (when (.exists (io/file file))
    (-> (slurp file) strip-frontmatter)))

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

(defn wrap-paragraphs
  "Split content by blank lines and wrap each non-block segment in <p> so that
   e.g. 'Last updated' on its own line stays on its own line (block)."
  [text]
  (let [segments (str/split (str/trim text) #"\n\n+")
        wrap-if-inline (fn [s]
                         (let [t (str/trim s)]
                           (if (or (str/blank? t)
                                   (str/starts-with? t "<h")
                                   (str/starts-with? t "<ul")
                                   (str/starts-with? t "<p>"))
                             t
                             (str "<p>" t "</p>"))))]
    (str/join "\n\n" (map wrap-if-inline segments))))

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
          with-lists (wrap-lists with-formatting)
          with-paragraphs (wrap-paragraphs with-lists)]
      (wrap-paragraph with-paragraphs))))

(defn read-yaml-flat
  "Parse flat YAML (key: value per line). Values may be quoted with \" or '. Returns map with keyword keys."
  [content]
  (when content
    (let [lines (str/split-lines content)
          unquote (fn [v]
                    (let [v (str/trim v)]
                      (cond
                        (and (str/starts-with? v "\"") (str/ends-with? v "\""))
                        (str/replace (subs v 1 (dec (count v))) #"\\\"" "\"")
                        (and (str/starts-with? v "'") (str/ends-with? v "'"))
                        (str/replace (subs v 1 (dec (count v))) #"\\'" "'")
                        :else v)))
          parse-line (fn [line]
                       (let [trimmed (str/trim line)]
                         (when (and (seq trimmed) (not (str/starts-with? trimmed "#")))
                           (let [colon-idx (str/index-of trimmed ": ")]
                             (when colon-idx
                               (let [k (str/trim (subs trimmed 0 colon-idx))
                                     v-raw (str/trim (subs trimmed (+ 2 colon-idx)))]
                                 (when (seq k)
                                   [(keyword (str/replace k #"\s+" "_")) (unquote v-raw)])))))))]
      (into {} (keep parse-line lines)))))

(defn read-i18n [locale]
  "Load i18n strings for locale from src/i18n/<locale>.yaml. Returns map with keyword keys (empty if missing)."
  (let [path (str "src/i18n/" locale ".yaml")
        f (io/file path)]
    (if (.exists f)
      (read-yaml-flat (slurp path))
      {})))

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

(def ^:private lcp-preload
  "<!-- LCP hero image preload (homepage only) -->\n    <link rel=\"preload\" as=\"image\" href=\"/assets/images/photos/home-hero-empowering-futures-promotional-800w.avif\" type=\"image/avif\">")

(def ^:private site-base "https://www.hispanosaliados.org")

(defn compose-page [base-template page-content title page-name locale i18n]
  (let [active-nav-page (if (= page-name "index") "home" page-name)
        active-class (fn [page] (if (= page active-nav-page) "active" ""))
        locale-prefix (if (= locale "en") "" (str "/" locale))
        canonical-path (if (= page-name "index")
                         (if (= locale "en") "/" (str "/" locale "/"))
                         (str locale-prefix "/" page-name ".html"))
        head-extra (cond (= page-name "donate") givebutter-script
                         (= page-name "index") lcp-preload
                         :else "")
        robots-meta (if (= page-name "404")
                     "noindex, nofollow"
                     "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1")
        site-id (System/getenv "SITE_ID")
        netlify-badge-content (if (and site-id (not (str/blank? (str/trim site-id))))
                                (str "<img src=\"https://api.netlify.com/api/v1/badges/" (str/trim site-id) "/deploy-status\" alt=\"Deploys by Netlify\" class=\"netlify-badge\" width=\"114\" height=\"51\" loading=\"lazy\">")
                                "Deploys by Netlify")
        ;; Alternate language URLs for switcher and hreflang
        path-en (if (= page-name "index") "/" (str "/" page-name ".html"))
        path-es (if (= page-name "index") "/es/" (str "/es/" page-name ".html"))
        switch-to-en-url path-en
        switch-to-es-url path-es
        hreflang-links (str "<link rel=\"alternate\" hreflang=\"en\" href=\"" site-base path-en "\">\n    "
                           "<link rel=\"alternate\" hreflang=\"es\" href=\"" site-base path-es "\">")
        html-lang (if (= locale "en") "en" "es")
        curr-lang-en (if (= locale "en") "true" "")
        curr-lang-es (if (= locale "es") "true" "")
        content-map (merge i18n
                           {:content page-content
                            :robots robots-meta
                            :title title
                            :canonical-path canonical-path
                            :head-extra head-extra
                            :netlify-badge-content netlify-badge-content
                            :locale-prefix locale-prefix
                            :html-lang html-lang
                            :switch-to-en-url switch-to-en-url
                            :switch-to-es-url switch-to-es-url
                            :hreflang-links hreflang-links
                            :current-lang-en curr-lang-en
                            :current-lang-es curr-lang-es
                            :active-home (active-class "home")
                            :active-about (active-class "about")
                            :active-programs (active-class "programs")
                            :active-impact (active-class "impact")
                            :active-get-involved (active-class "get-involved")
                            :active-donate (active-class "donate")
                            :active-contact (active-class "contact")})]
    (render-template base-template content-map)))

(defn copy-assets
  "Copy src/assets into public/assets, merging without deleting existing files.
   Preserves cached build outputs (e.g. .webp, .avif) restored by netlify-plugin-cache."
  []
  (let [assets-dir (io/file "src/assets")
        public-assets "public/assets"
        prefix (str (.getPath assets-dir) "/")]
    (when (.exists assets-dir)
      (doseq [f (file-seq assets-dir)]
        (when (.isFile f)
          (let [path-str (.getPath f)
                rel (subs path-str (count prefix))
                dest (str public-assets "/" rel)]
            (fs/create-dirs (fs/parent dest))
            (fs/copy path-str dest {:replace-existing true})))))))

(defn copy-static-file [src-path public-path]
  (when (.exists (io/file src-path))
    (fs/copy src-path public-path {:replace-existing true})))

(defn copy-robots-txt []
  (copy-static-file "src/robots.txt" "public/robots.txt"))

(defn copy-redirects []
  (copy-static-file "src/_redirects" "public/_redirects"))

(defn copy-headers []
  (copy-static-file "src/_headers" "public/_headers"))

(def ^:private decap-cms-version "3.10.0")
(def ^:private decap-cms-url (str "https://unpkg.com/decap-cms@" decap-cms-version "/dist/decap-cms.js"))

(defn copy-admin
  "Copy src/admin (Decap CMS) to public/admin so /admin is available on the live site.
   If decap-cms.js is not present in public/admin, download it from unpkg (self-hosted for performance)."
  []
  (let [admin-dir (io/file "src/admin")
        public-admin "public/admin"
        decap-dest (str public-admin "/decap-cms.js")]
    (when (.exists admin-dir)
      (.mkdirs (io/file public-admin))
      (doseq [f (file-seq admin-dir)]
        (when (.isFile f)
          (let [name (.getName f)
                dest (str public-admin "/" name)]
            (fs/copy (.getPath f) dest {:replace-existing true})
            (println "Copied admin:" name))))
      ;; Self-host Decap CMS bundle: download if missing (avoids unpkg redirect + enables caching)
      (when (not (.exists (io/file decap-dest)))
        (println "Downloading decap-cms.js for self-hosting...")
        (let [result (p/process ["curl" "-sL" decap-cms-url "-o" decap-dest]
                               {:dir (System/getProperty "user.dir")
                                :out :string :err :string})]
          (if (= (:exit @result) 0)
            (println "Downloaded decap-cms.js (" decap-cms-version ")")
            (println "Warning: failed to download decap-cms.js:" (:err @result))))))))

(def ^:private sitemap-base "https://www.hispanosaliados.org")

(defn write-sitemap [locales pages]
  "Write public/sitemap.xml with indexable pages for all locales (excludes 404)."
  (let [today (str (java.time.LocalDate/now))
        indexable-pages (remove (fn [[page-name _ _]] (= page-name "404")) pages)
        url-entry (fn [locale page-name]
                    (let [prefix (if (= locale "en") "" (str "/" locale))
                          path (if (= page-name "index")
                                 (if (= locale "en") "/" (str prefix "/"))
                                 (str prefix "/" page-name ".html"))
                          loc (str sitemap-base path)]
                      (str "  <url>\n    <loc>" loc "</loc>\n    <lastmod>" today "</lastmod>\n    <changefreq>weekly</changefreq>\n  </url>")))
        entries (for [locale locales
                     [page-name _ _] indexable-pages]
                 (url-entry locale page-name))
        xml (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                 "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                 (str/join "\n" entries) "\n"
                 "</urlset>\n")]
    (spit "public/sitemap.xml" xml)
    (println "Wrote sitemap.xml")))

(defn minify-assets
  "Optional: minify CSS and JS when npx/csso-cli and npx/terser are available."
  []
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

(defn skip-full-size-webp?
  "Skip full-size WebP/AVIF for photos that only use responsive srcset in templates."
  [path]
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

(defn generate-webp
  "Generate WebP versions of PNG/JPG under public/assets/images when sharp-cli is available."
  []
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

(defn generate-avif
  "Generate AVIF versions of PNG/JPG under public/assets/images when sharp-cli supports it."
  []
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

(defn generate-responsive
  "Generate multi-width variants (480, 800, 1200) for content photos for srcset/sizes."
  []
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
                ;; Use git last-commit time for source so cached outputs skip after restore (fs mtime changes).
                need? (fn [src-path out-path]
                        (let [src-time (or (source-modified-time src-path)
                                           (.lastModified (io/file src-path)))
                              out-f (io/file out-path)]
                          (or (not (.exists out-f))
                              (nil? src-time)
                              (> src-time (.lastModified out-f)))))
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

(def ^:private logo-256w-name "FINAL-logo-HA4E-empowering-futures")
(def ^:private logo-256w-size 256)

(defn generate-logo-256w
  "Generate 256px logo variants for header (smaller than full-size for LCP)."
  []
  (let [src-png (str "public/assets/images/logo/" logo-256w-name ".png")
        src-file (io/file src-png)
        out-dir "public/assets/images/logo"
        out-webp (str out-dir "/" logo-256w-name "-256w.webp")
        out-avif (str out-dir "/" logo-256w-name "-256w.avif")
        src-for-mtime (str "src/assets/images/logo/" logo-256w-name ".png")
        src-time (or (source-modified-time src-for-mtime)
                     (when (.exists (io/file src-png)) (.lastModified (io/file src-png))))]
    (when (.exists src-file)
      (.mkdirs (io/file out-dir))
      (doseq [[out-path fmt qual] [[out-webp "webp" 85] [out-avif "avif" 70]]]
        (let [out-f (io/file out-path)
              need? (or (not (.exists out-f))
                        (nil? src-time)
                        (> src-time (.lastModified out-f)))]
          (when need?
            (let [result (p/process ["npx" "--yes" "sharp-cli" "--input" src-png "--output" out-path
                                    "resize" (str logo-256w-size) "--format" fmt "--quality" (str qual)]
                                   {:dir (System/getProperty "user.dir") :out :string :err :string})]
              (if (= (:exit @result) 0)
                (println (str (str/upper-case (subs fmt 0 1)) (subs fmt 1) " 256w logo:" out-path))
                (println "Note: 256w logo skipped for" out-path ":" (:err @result))))))))))

(defn ensure-dir [path]
  (.mkdirs (io/file path)))

(defn generate-svg-favicon
  "Generate SVG favicon with HA (blue) and 4E (red) text on white background"
  [size]
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

(defn generate-favicons
  "Generate favicon files with HA4E text - pure Clojure solution"
  []
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

(defn read-spotlights [locale]
  "Read spotlights for locale from src/content/<locale>/spotlights.json (Decap CMS)."
  (let [json-file (io/file (str "src/content/" locale "/spotlights.json"))]
    (when (.exists json-file)
      (try (json/read-str (slurp (.getPath json-file)))
           (catch Exception _ nil)))))

(def ^:private max-previous-spotlights 6)

(defn- escape-html [s]
  (when s (str/replace (str s) #"<" "&lt;")))

(defn- spotlight-name-html [name url]
  (let [safe-name (escape-html name)]
    (if (and url (seq (str/trim url)))
      (str "<a href=\"" (str/replace (str/trim url) #"\"" "&quot;") "\" rel=\"noopener noreferrer\" target=\"_blank\" class=\"spotlight-name-link\">" safe-name "</a>")
      safe-name)))

(defn render-spotlight-section [locale i18n]
  (let [data (read-spotlights locale)
        current (:current data)
        previous (take max-previous-spotlights (:previous data))
        spotlight-heading (get i18n :spotlight_heading "Volunteer Spotlight")
        spotlight-previous-title (get i18n :spotlight_previous_title "Previous spotlights")]
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
                               "<p class=\"spotlight-previous-title\">" spotlight-previous-title "</p>"
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
             "<h2 id=\"spotlight-heading\">" spotlight-heading "</h2>"
             "<div class=\"section-divider\"></div>"
             current-html
             previous-html
             "</div></section>")))))

(defn build-page [page-name base-template page-template content-map title locale i18n output-dir]
  (let [output-file (str output-dir "/" (if (= page-name "index") "index.html" (str page-name ".html")))]
    (ensure-dir output-dir)
    (let [page-html (if page-template
                      (render-template page-template content-map)
                      (:content content-map))
          full-html (compose-page base-template page-html title page-name locale i18n)]
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
(generate-logo-256w)
(minify-assets)
(copy-robots-txt)
(copy-redirects)
(copy-headers)
(copy-admin)

(def ^:private locales ["en" "es"])

(def ^:private page-title-keys
  "Map page-name to i18n key for locale-specific page title (used in <title> and og:title)."
  {"index" :nav_home "about" :about_heading "programs" :programs_heading "impact" :impact_heading_page
   "get-involved" :get_involved_heading "donate" :donate_heading "contact" :contact_heading
   "contact-success" :contact_success_heading "privacy" :privacy_heading "404" :error_404_heading})

(let [base-template (read-template "base")
      pages [["index" "home" "Home"]
             ["about" "about" "About Us"]
             ["programs" "programs" "Programs"]
             ["impact" "impact" "Impact"]
             ["get-involved" "get-involved" "Get Involved"]
             ["donate" "donate" "Donate"]
             ["contact" "contact" "Contact"]
             ["contact-success" "contact-success" "Thank You"]
             ["privacy" "privacy" "Privacy Policy"]
             ["404" "404" "Page Not Found"]]]
  (doseq [locale locales]
    (let [i18n (read-i18n locale)
          output-dir (if (= locale "en") "public" (str "public/" locale))
          content-dir (str "src/content/" locale "/")]
      (ensure-dir output-dir)
      (doseq [[page-name template-name title] pages]
        (let [locale-prefix (if (= locale "en") "" (str "/" locale))
              page-title (or (get i18n (get page-title-keys page-name)) title)
              content-file (str content-dir template-name ".md")
              fallback-file (when (= locale "en") (str "src/content/" template-name ".md"))
              content (or (read-markdown content-file)
                          (read-markdown (or fallback-file "")))
              processed-content (if content (process-markdown content) "")
              content-map (-> {:content processed-content :title page-title :locale-prefix locale-prefix}
                              (merge i18n)
                              (cond-> (= page-name "get-involved")
                                (merge {:spotlight-section (render-spotlight-section locale i18n)})))
              page-template (read-template template-name)]
          (build-page page-name base-template page-template content-map page-title locale i18n output-dir)))))
  (write-sitemap locales pages))

(println "Build complete!")
