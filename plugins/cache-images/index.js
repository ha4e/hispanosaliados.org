// Local plugin: cache public/assets/images in onPostBuild (before deploy)
// so the directory is still full when we save. netlify-plugin-cache uses
// onSuccess (after deploy), which can see an empty/moved public/ and save
// only 2 files, causing alternating full/empty cache.
const PATH = "public/assets/images";

module.exports = {
  onPreBuild: async ({ utils: { cache } }) => {
    if (await cache.restore(PATH)) {
      const files = await cache.list(PATH);
      console.log(`Successfully restored: ${PATH} ... ${files.length} files in total.`);
    } else {
      console.log(`A cache of '${PATH}' doesn't exist (yet).`);
    }
  },

  onPostBuild: async ({ utils: { cache, status } }) => {
    if (await cache.save(PATH)) {
      const files = await cache.list(PATH);
      console.log(`Successfully cached: ${PATH} ... ${files.length} files in total.`);
      status.show({
        title: `${files.length} files cached`,
        summary: "Restored on the next build (saved before deploy).",
        text: PATH,
      });
    } else {
      console.log(`Attempted to cache: ${PATH} ... but failed (path may not exist).`);
    }
  },
};
