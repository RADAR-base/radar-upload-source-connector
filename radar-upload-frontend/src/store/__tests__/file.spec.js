/* eslint-disable no-undef */
// eslint-disable-next-line no-unused-vars
import { mutations } from '../file';

describe('mutations', () => {
  it('addUploadingFile', () => {
    const state = { uploadingFile: [] };
    const fileName = 'fileName';
    mutations.addUploadingFile(state, { fileName });
    expect(state.uploadingFile).toEqual([fileName]);
  });
});
