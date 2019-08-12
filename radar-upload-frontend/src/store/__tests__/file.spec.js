/* eslint-disable no-undef */
// eslint-disable-next-line no-unused-vars
import { originalState, mutations, actions } from '../file';

describe('mutations', () => {
  const state = originalState();
  it('addUploadingFile', () => {
    const fileName = 'FileName';
    mutations.addUploadingFile(state, { fileName });
    expect(state.uploadingFile).toEqual([fileName]);
  });
});


describe('actions', () => {

});
