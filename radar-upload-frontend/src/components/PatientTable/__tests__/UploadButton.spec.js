/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import UploadButton from '../UploadButton.vue';
import fileAPI from '@/axios/file.js';

const fileTypeList = [
  {
    name: 'audioMp3',
    contentTypes: ['audio/mp3'],
  },
];


const postRecordBody = {
  data: {
    projectId: 'radar-test',
    userId: 'testUser',
    // sourceId: 'source',
  },
  // sourceType: 'Mp3Audio',
};
const commonInfo = {
  projectName: 'project name',
  patientName: 'patient name',
};

describe('UploadButton', () => {
  // call this api when component is created
  fileAPI.getSourceTypes = jest.fn().mockReturnValue(fileTypeList);
  const wrapper = shallowMount(UploadButton, {
    propsData: {
      uploadInfo: postRecordBody,
      commonInfo,
    },
    stubs: ['v-btn',
      'v-icon',
      'v-menu',
      'v-card',
      'v-list',
      'v-subheader',
      'v-card-actions',
      'v-spacer',
      'v-card-text',
      'v-list-item',
      'v-select',
      'v-file-input'],
  });

  beforeEach(() => {
    fileAPI.getSourceTypes.mockClear();
    jest.fn().mockClear();
  });

  it('call api to get fileTypes/resourceTypes and contentTypes when created', async () => {
    expect(wrapper.vm.fileTypeList).toEqual(fileTypeList.map(el => el.name));
    expect(wrapper.vm.contentTypes).toEqual(fileTypeList.map(el => el.contentTypes));
  });

  it('receive correct uploadInfo and common props', () => {
    expect(wrapper.vm.uploadInfo).toEqual(postRecordBody);
    expect(wrapper.vm.commonInfo).toEqual(commonInfo);
    // expect(wrapper.text()).toContain(commonInfo.projectName);
    // expect(wrapper.text()).toContain(commonInfo.patientName);
  });

  it('upload btn disabled if either file or fileType not selected', () => {
    const file = new File([''], 'filename1');
    const uploadButton = wrapper.findAll('v-btn-stub').at(1);
    expect(uploadButton.attributes().disabled).toBe('true');
    wrapper.setData({ fileType: '123' });
    expect(uploadButton.attributes().disabled).toBe('true');
    wrapper.setData({ file });
    expect(uploadButton.attributes().disabled).not.toBe('true');
  });

  it('click upload btn: call POST and "then" PUT request to upload selected file with correct payload, then close menu', async () => {
    // POST /records
    const postReturnVal = { id: 'id1', createdDate: '2019-10-10' };
    const { projectId, userId } = wrapper.vm.uploadInfo;
    const postRecordPayload = { projectId, userId, sourceType: wrapper.vm.fileType };
    fileAPI.postRecords = jest.fn().mockResolvedValue(postReturnVal);

    const uploadButton = wrapper.findAll('v-btn-stub').at(1);
    uploadButton.trigger('click');
    await flushPromises();
    expect(fileAPI.postRecords).toBeCalledWith(postRecordPayload);
    await flushPromises();
    // PUT records
    const { file } = wrapper.vm;
    const putRecordPayload = { id: postReturnVal.id, file, fileName: file.name };
    const putReturnVal = 'return value';
    fileAPI.putRecords = jest.fn().mockResolvedValue(putReturnVal);

    expect(fileAPI.putRecords).toBeCalledWith(putRecordPayload);


    expect(wrapper.vm.menu).toBe(false);
  });

  it('click cancel btn: close menu and remove data selected', () => {
    const cancelBtn = wrapper.findAll('v-btn-stub').at(0);
    cancelBtn.trigger('click');
    expect(wrapper.vm.menu).toBe(false);
    expect(wrapper.vm.file).toEqual([]);
    expect(wrapper.vm.fileType).toBe('');
    expect(wrapper.vm.fileTypeList).toEqual([]);
  });
});
