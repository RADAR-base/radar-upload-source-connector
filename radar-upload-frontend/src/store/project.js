export function initialState() {
  return {
    currentProject: {
      value: '',
      text: '',
    },
  };
}

export const mutations = {
  setCurrentProject(state, { value, text }) {
    state.currentProject.value = value;
    state.currentProject.text = text;
  },
};

export const state = initialState();

export const actions = {};


export default { mutations, state, actions };
