const files = require.context('.', false, /\.js$/);
const modules = {};
files.keys().forEach((fileName) => {
  if (fileName.includes('index.js')) { return; }
  const moduleName = fileName.replace(/(\.\/|\.js)/g, '');
  modules[moduleName] = {
    ...files(fileName).default,
    namespaced: true,
  };
});
// remove index.js in file list
export const fileList = files.keys().filter(el => !el.includes('index.js'));
export default modules;
