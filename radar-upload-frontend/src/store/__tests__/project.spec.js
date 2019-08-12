/* eslint-disable no-undef */
// eslint-disable-next-line no-unused-vars
import { state, mutations, actions } from '../project';

describe('mutation', () => {
  it('setCurrentProject', () => {
    const id = 1;
    mutations.setCurrentProject(state, 1);
    expect(state.currentProject).toBe(id);
  });
});
