

export const mutations = {
  setCurrentProject(state, projectId) {
    state.currentProject = projectId;
  },
};

export const state = {
  currentProject: '',
};

export const actions = {};


export default { mutations, state, actions };
