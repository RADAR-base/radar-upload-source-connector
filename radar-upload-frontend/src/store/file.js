export const originalState = () => ({
  uploadingFile: [],
});

export const mutations = {
  addUploadingFile(state, { fileName }) {
    state.uploadingFile.push(fileName);
  },
};
export const actions = {};

export const state = {
  ...originalState(),
};
export default { mutations, state, actions };
