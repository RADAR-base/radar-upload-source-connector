

export const mutations = {
  setCurrentProject(state, { value, text }) {
    state.currentProject.value = value;
    state.currentProject.text = text;
  },
};

export const state = {
  currentProject: {
    value: '',
    text: '',
  },
};

export const actions = {};


export default { mutations, state, actions };
