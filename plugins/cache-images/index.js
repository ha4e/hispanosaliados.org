// Local plugin: cache public/assets/images in onPostBuild (before deploy)
// so the directory is still full when we save. netlify-plugin-cache uses
// onSuccess (after deploy), which can see an empty/moved public/ and save
// only 2 files, causing alternating full/empty cache.
const fs = require("fs");
const path = require("path");

const PATH = "public/assets/images";

function countFilesRecursive(dir) {
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) return 0;
  let n = 0;
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    n += fs.statSync(full).isDirectory() ? countFilesRecursive(full) : 1;
  }
  return n;
}

module.exports = {
  onPreBuild: async ({ utils: { cache } }) => {
    if (await cache.restore(PATH)) {
      const n = countFilesRecursive(PATH);
      console.log(`Successfully restored: ${PATH} ... ${n} files in total.`);
    } else {
      console.log(`A cache of '${PATH}' doesn't exist (yet).`);
    }
  },

  onPostBuild: async ({ utils: { cache, status } }) => {
    if (await cache.save(PATH)) {
      const n = countFilesRecursive(PATH);
      console.log(`Successfully cached: ${PATH} ... ${n} files in total.`);
      status.show({
        title: `${n} files cached`,
        summary: "Restored on the next build (saved before deploy).",
        text: PATH,
      });
    } else {
      console.log(`Attempted to cache: ${PATH} ... but failed (path may not exist).`);
    }
  },
};
