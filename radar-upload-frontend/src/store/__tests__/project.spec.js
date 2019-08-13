/* eslint-disable no-undef */
// eslint-disable-next-line no-unused-vars
import { state, mutations, actions } from '../project';

describe('mutation', () => {
  it('setCurrentProject', () => {
    const value = 1;
    const text = 'text';
    mutations.setCurrentProject(state, { value, text });
    expect(state.currentProject.value).toBe(value);
    expect(state.currentProject.text).toBe(text);
  });
});
