// Wrapper: load transpiled ClojureScript (target/cache-images.js) and re-export Netlify hook API.
// Build with: cd plugins/cache-images && npm run build
const built = require("./target/cache-images.js");
module.exports = {
  onPreBuild: built.onPreBuild,
  onPostBuild: built.onPostBuild,
};
