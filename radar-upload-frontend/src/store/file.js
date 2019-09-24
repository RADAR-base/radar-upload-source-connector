export const originalState = () => ({
  uploadingFile: [],
  searchText: '',
});

export const mutations = {
  addUploadingFile(state, { fileName }) {
    state.uploadingFile.push(fileName);
  },
  handleSearch(state, searchText) {
    state.searchText = searchText;
  },
};
export const actions = {};

export const state = {
  ...originalState(),
};
export default { mutations, state, actions };
