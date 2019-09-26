/* eslint-disable no-undef */
// eslint-disable-next-line no-unused-vars
import { originalState, mutations, actions } from '../file';

describe('mutations', () => {
  const state = originalState();
  it('handleSearch', () => {
    const searchText = 'xx';
    mutations.handleSearch(state, searchText);
    expect(state.searchText).toBe(searchText);
  });
});


describe('actions', () => {

});
