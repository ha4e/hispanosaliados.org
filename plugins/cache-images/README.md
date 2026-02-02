# cache-images (Netlify plugin)

Caches `public/assets/images` in `onPostBuild` so the directory is still full when Netlify saves the cache. Built with [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html) from ClojureScript.

## Build

When you change the CLJS, from repo root:

```bash
make plugin
```

Or from this directory: `npm install && npm run build`. Requires Node and Java (for shadow-cljs). The built `target/cache-images.js` is committed so Netlify (and local) can load the plugin without building.

## Source

- `src/cache_images/core.cljs` – plugin logic (onPreBuild, onPostBuild)
- `shadow-cljs.edn` – build config (`:node-library` target)
- `index.js` – wrapper that requires the built file and exports the hook API

## Security (npm audit)

`npm audit` may report 6 low-severity issues in shadow-cljs’s transitive deps (elliptic → crypto-browserify → node-libs-browser). Those are used for shadow-cljs’s browser bundling, not by this plugin at runtime (we only use Node `fs` and `path`). The only fix npm suggests is `npm audit fix --force`, which would upgrade to shadow-cljs 3.x (breaking). We don’t run that for now; if you want to address the advisories, plan on upgrading to shadow-cljs 3.x and testing the build.
