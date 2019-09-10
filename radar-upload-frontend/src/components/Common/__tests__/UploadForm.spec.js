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
    stubs: ['v-btn',
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
      'v-file-input'],
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

  it('disable button', () => {
    const file = new File([''], 'filename1');
    const uploadbtn = wrapper.findAll('v-btn-stub').at(1);
    expect(uploadbtn.attributes().disabled).toBe('true');

    wrapper.setData({ sourceType: '123' });
    expect(uploadbtn.attributes().disabled).toBe('true');

    wrapper.setData({ file });
    expect(uploadbtn.attributes().disabled).not.toBe('true');

    wrapper.setProps({ uploadInfo: { userId: null } });
    expect(uploadbtn.attributes().disabled).toBe('true');

    wrapper.setData({ userId: 'userId' });
    expect(uploadbtn.attributes().disabled).not.toBe('true');
  });

  it('upload files success', async () => {
    wrapper.setData({ uploadInfo: postRecordBody });
    const removeData = jest.spyOn(wrapper.vm, 'removeData');
    // mock POST /records
    const postReturnVal = { id: 'id1', createdDate: '2019-10-10' };
    const { projectId, userId } = wrapper.vm.uploadInfo;
    const postRecordPayload = { projectId, userId, sourceType: wrapper.vm.sourceType };
    fileAPI.postRecords = jest.fn().mockResolvedValue(postReturnVal);
    // mock PUT request
    const { file } = wrapper.vm;
    const putRecordPayload = { id: postReturnVal.id, file, fileName: file.name };
    const putReturnVal = { id: 'id1' };
    fileAPI.putRecords = jest.fn().mockResolvedValue(putReturnVal);
    // mock markRecord
    fileAPI.markRecord = jest.fn().mockResolvedValue();
    // file uploaded
    const files = [];
    files.push({ fileName: file.name, uploading: true, uploadFailed: false });


    wrapper.vm.uploadFile();
    expect(wrapper.emitted().creatingRecord).toBeTruthy();
    await flushPromises();
    // postRecords
    expect(fileAPI.postRecords).toBeCalledWith(postRecordPayload);
    // eslint-disable-next-line max-len
    expect(wrapper.emitted().startUploading[0][0]).toEqual({ ...postReturnVal, files, active: true });

    // putRecords
    expect(fileAPI.putRecords).toBeCalledWith(putRecordPayload);
    expect(wrapper.emitted().finishUpload[0][0]).toEqual(putReturnVal);
    expect(fileAPI.markRecord).toBeCalled();
    expect(removeData).toBeCalled();

    fileAPI.postRecords.mockClear();
  });

  it('uploadfile: POST error', async () => {
    const removeData = jest.spyOn(wrapper.vm, 'removeData');
    fileAPI.postRecords = jest.fn().mockRejectedValue();

    wrapper.vm.uploadFile();
    await flushPromises();
    expect(wrapper.vm.$error).toBeCalled();
    expect(removeData).toBeCalled();
  });


  it('uploadfile: PUT error', async () => {
    fileAPI.postRecords = jest.fn().mockResolvedValue({ id: 'id1', createdDate: '2019-10-10' });
    fileAPI.putRecords = jest.fn().mockRejectedValue();

    wrapper.vm.uploadFile();

    await flushPromises();
    expect(wrapper.vm.$error).toBeCalled();
    expect(wrapper.emitted().uploadFailed).toBeTruthy();
  });

  it('removeData', () => {
    wrapper.vm.removeData();
    expect(wrapper.vm.file).toEqual([]);
    expect(wrapper.vm.sourceType).toBe('');
  });
});
