/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import UploadButton from '../UploadButton.vue';
import fileAPI from '@/axios/file.js';

const sourceTypeList = [
  {
    name: 'audioMp3',
    contentTypes: ['audio/mp3'],
  },
];


const postRecordBody = {
  projectId: 'radar-test',
  userId: 'testUser',
};


describe('UploadButton', () => {
  // call this api when component is created
  const $store = new Store();
  fileAPI.getSourceTypes = jest.fn().mockReturnValue(sourceTypeList);
  const wrapper = shallowMount(UploadButton, {
    propsData: {
      uploadInfo: postRecordBody,
    },
    mocks: {
      $store,
      $success: jest.fn(),
      $error: jest.fn(),
    },
    stubs: ['v-btn',
      'v-icon',
      'v-menu',
    ],
  });

  beforeEach(() => {
    fileAPI.getSourceTypes.mockClear();
    jest.fn().mockClear();
  });
  it('has uploadInfo props', () => {
    expect(wrapper.vm.uploadInfo).toEqual(postRecordBody);
  });

  it('finishUpload', () => {
    wrapper.setData({ menu: true, loading: true });
    const file = {};
    wrapper.vm.finishUpload(file);
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.emitted().addUploadingFile[0][0]).toEqual(file);
    expect(wrapper.vm.menu).toBe(false);
  });

  it('startUploading', () => {
    const payload = '';
    wrapper.vm.startUploading(payload);
    expect(wrapper.emitted().startUploading[0][0]).toEqual(payload);
  });

  it('closeMenu', () => {
    wrapper.setData({ menu: true, loading: true });
    wrapper.vm.closeMenu();
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.menu).toBe(false);
  });
});
