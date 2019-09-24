export const originalState = () => ({
  searchText: '',
});

export const mutations = {
  handleSearch(state, searchText) {
    state.searchText = searchText;
  },
};
export const actions = {};

export const state = {
  ...originalState(),
};
export default { mutations, state, actions };
