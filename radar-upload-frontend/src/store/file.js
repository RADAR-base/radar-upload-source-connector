export const originalState = () => ({
  uploadingFile: [],
});
export const state = {
  ...originalState(),
};
export const mutations = {
  addUploadingFile(state, { fileName }) {
    state.uploadingFile.push(fileName);
  },
};
export const actions = {};


export default { mutations, state, actions };
