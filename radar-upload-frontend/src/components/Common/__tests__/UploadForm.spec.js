/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import UploadForm from '../UploadForm.vue';
import fileAPI from '@/axios/file.js';

const sourceTypeList = [
  {
    name: 'audioMp3',
    sourceType: ['audio/mp3'],
  },
];

const postRecordBody = {
  projectId: 'radar-test',
  userId: 'testUser',
};

describe('UploadForm', () => {
  // call this api when component is created
  const $store = new Store();
  fileAPI.getSourceTypes = jest.fn().mockReturnValue(sourceTypeList);
  const wrapper = shallowMount(UploadForm, {
    propsData: {
      uploadInfo: postRecordBody,
    },
    mocks: {
      $store,
      $success: jest.fn(),
      $error: jest.fn(),
    },
    stubs: [
      'v-btn',
      'v-icon',
      'v-card',
      'v-list',
      'v-subheader',
      'v-card-actions',
      'v-spacer',
      'v-autocomplete',
      'v-card-text',
      'v-list-item',
      'v-select',
      'v-file-input',
      'v-list-item-content',
      'v-data-table',
      'v-text-field',
      'v-toolbar',
      'v-toolbar-title',
    ],
  });

  beforeEach(() => {
    fileAPI.getSourceTypes.mockClear();
    jest.fn().mockClear();
  });
  it('has uploadInfo props', () => {
    expect(wrapper.vm.uploadInfo).toEqual(postRecordBody);
  });


  it('call api to get sourceTypes/resourceTypes and contentTypes when created', async () => {
    expect(wrapper.vm.sourceTypeList).toEqual(sourceTypeList.map(el => el.name));
  });


  it.only('closeDialog', async () => {
    wrapper.vm.closeDialog();
    await flushPromises();
    const removeData = jest.spyOn(wrapper.vm, 'removeData');
    expect(wrapper.emitted().cancelClick).toBeTruthy();
    expect(removeData).toBeCalled();
  });


  it('removeData', () => {
    wrapper.vm.removeData();
    expect(wrapper.vm.files).toEqual([]);
    // expect(wrapper.vm.sourceTypeList).toEqual([]);
    expect(wrapper.vm.sourceType).toBe('');
    expect(wrapper.vm.activeRecord).toBe(null);
    expect(wrapper.vm.userId).toBe('');
  });

  it('removeErrorFile', () => {
    const fileId = 'fileId';
    wrapper.setData({ files: [{ fileId: 'fileId' }] });
    wrapper.vm.removeErrorFile(fileId);
    expect(wrapper.vm.files).toEqual([]);
  });

  it('createRecord', async () => {
    const createRecordReturn = { id: 'id1', revision: 'revision' };
    fileAPI.postRecords = jest.fn().mockResolvedValue(createRecordReturn);
    wrapper.vm.createRecord();
    expect(wrapper.vm.isLoading).toBe(true);
    await flushPromises();
    expect(wrapper.vm.activeRecord).toEqual(createRecordReturn);
    expect(wrapper.vm.isLoading).toBe(false);

    // error case
    fileAPI.postRecords.mockClear();
    fileAPI.postRecords = jest.fn().mockRejectedValue();
    wrapper.vm.createRecord();
    await flushPromises();
    expect(wrapper.vm.$error).toBeCalled();
  });

  it('startUpload', () => {
    const files = [
      { success: true, error: true },
      { success: false, error: false },
    ];
    wrapper.setData({
      files,
    });
    const processUpload = jest.spyOn(wrapper.vm, 'processUpload');
    wrapper.vm.startUpload();
    expect(processUpload).toBeCalledTimes(1);
    expect(processUpload).toBeCalledWith(files[1]);
  });

  it('processUpload', async () => {
    const uploadedFile = 'uploadedFile';
    fileAPI.putRecords = jest.fn().mockResolvedValue(uploadedFile);
    const fileObject = {
      id: 'fileId',
      name: 'upload file',
      file: { },
      active: false,
      success: false,
      fileType: 'fileType',
      error: '',
    };
    const activeRecord = { id: 'recordId' };
    wrapper.setData({ activeRecord, files: [fileObject] });

    wrapper.vm.processUpload(fileObject);
    expect(wrapper.vm.files[0].active).toBe(true);
    await flushPromises();
    expect(wrapper.vm.files[0].success).toBe(true);
    expect(wrapper.vm.files[0].active).toBe(false);

    // error case
    fileAPI.putRecords.mockClear();
    fileAPI.putRecords = jest.fn().mockRejectedValue();
    await wrapper.vm.processUpload(fileObject);
    expect(wrapper.vm.files[0].error).toBe(true);
  });

  // it('disable button', () => {
  //   const file = new File([''], 'filename1');
  //   const uploadbtn = wrapper.findAll('v-btn-stub').at(1);
  //   expect(uploadbtn.attributes().disabled).toBe('true');

  //   wrapper.setData({ sourceType: '123' });
  //   expect(uploadbtn.attributes().disabled).toBe('true');

  //   wrapper.setData({ files: [file] });
  //   expect(uploadbtn.attributes().disabled).not.toBe('true');

  //   wrapper.setProps({ uploadInfo: { userId: null } });
  //   expect(uploadbtn.attributes().disabled).toBe('true');

  //   wrapper.setData({ userId: 'userId' });
  //   expect(uploadbtn.attributes().disabled).not.toBe('true');
  // });
});
