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

const recordInfo = {
  recordId: 'recordId',
  recordStatus: 'recordStatus',
  recordRevision: 'revision',
  userId: 'testUser',
  sourceType: 'sourceType',
  projectId: 'radar-test',
};

describe('UploadForm', () => {
  // call this api when component is created
  const $store = new Store();
  fileAPI.getSourceTypes = jest.fn().mockReturnValue(sourceTypeList);
  const wrapper = shallowMount(UploadForm, {
    propsData: {
      recordInfo,
      isNewRecord: true,
    },
    mocks: {
      $store,
      $success: jest.fn(),
      $error: jest.fn(),
    },
    filters: {
      toMB: jest.fn(),
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

  it('has uploadInfo props', () => {
    expect(wrapper.vm.recordInfo).toEqual(recordInfo);
  });


  it('call api to get sourceTypes/resourceTypes and contentTypes when created', async () => {
    expect(wrapper.vm.sourceTypeList).toEqual(sourceTypeList.map(el => el.name));
  });


  it('closeDialog', () => {
    fileAPI.deleteRecord = jest.fn();
    wrapper.setProps({ isNewRecord: false });
    wrapper.vm.closeDialog();
    expect(fileAPI.deleteRecord).not.toBeCalled();

    wrapper.setData({ activeRecord: { id: 'recordId', revision: 1 } });
    wrapper.setProps({ isNewRecord: true });
    const removeData = jest.spyOn(wrapper.vm, 'removeData');
    wrapper.vm.closeDialog();

    expect(wrapper.emitted().cancelClick).toBeTruthy();
    expect(removeData).toBeCalled();
    expect(fileAPI.deleteRecord).toBeCalledWith({ recordId: 'recordId', revision: 1 });
  });


  it('removeData', () => {
    wrapper.vm.removeData();
    expect(wrapper.vm.files).toEqual([]);
    // expect(wrapper.vm.sourceTypeList).toEqual([]);
    expect(wrapper.vm.sourceType).toBe('');
    expect(wrapper.vm.activeRecord).toBe(null);
    expect(wrapper.vm.userId).toBe('');
  });

  it('filterUploadingFiles', () => {
    const newFile = { name: 'aaa' };
    wrapper.setData({ files: [newFile] });
    const prevent = jest.fn();
    wrapper.vm.filterUploadingFiles(newFile, null, prevent);

    expect(wrapper.vm.$error).toBeCalledWith(`File ${newFile.name} is duplicated`);
    expect(prevent).toBeCalled();
  });

  it('removeFile', async () => {
    const files = [
      { name: 'fileName1', success: true },
      { name: 'fileName2', success: false },
    ];
    wrapper.setData({ files, activeRecord: { status: 'INCOMPLETE', id: 'recordId' } });
    // remove files[1]
    wrapper.vm.removeFile(files[1]);
    expect(wrapper.vm.files).toEqual([files[0]]);

    // remove files[0]: error
    fileAPI.deleteFile = jest.fn().mockRejectedValue();
    wrapper.vm.removeFile(files[0]);
    await flushPromises();
    expect(wrapper.vm.$error).toBeCalledWith('Cannot delete this file, please try again later');
    // remove files[0]: success

    fileAPI.deleteFile = jest.fn().mockResolvedValue();
    wrapper.vm.removeFile(files[0]);
    await flushPromises();
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

  it('finishUpload', async () => {
    // success
    const closeDialog = jest.spyOn(wrapper.vm, 'closeDialog');
    fileAPI.markRecord = jest.fn().mockResolvedValue('mockedReturn');
    wrapper.vm.finishUpload();

    expect(wrapper.vm.isLoading).toBe(true);
    await flushPromises();
    expect(wrapper.vm.isLoading).toBe(false);
    expect(closeDialog).toBeCalled();

    // error
    wrapper.setData({ activeRecord: { id: 'recordId', revision: 'recordRevision' } });
    fileAPI.markRecord = jest.fn().mockRejectedValue();
    wrapper.vm.finishUpload();

    await flushPromises();
    expect(wrapper.vm.$error).toBeCalled();
  });
});
