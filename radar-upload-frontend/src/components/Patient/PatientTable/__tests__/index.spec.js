/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import index from '../index.vue';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file';

describe('index', () => {
  // call this api when component is created
  const PROJECT_ID = '';
  const $store = new Store({
    state: {
      project: {
        currentProject: { value: PROJECT_ID },
      },
      patient: {
        searchText: '',
      },
    },
  });

  const wrapper = shallowMount(index, {
    propsData: {
      isActive: false,
    },
    mocks: {
      $store,
    },
    stubs: ['v-data-table'],
  });

  it('not show the table if no project is selected', () => {
    expect(wrapper.vm.items).toEqual([]);
  });

  it('watch: currentProject', async () => {
    wrapper.setProps({ isActive: false });
    wrapper.vm.$store.state.project.currentProject.value = 'original';
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');
    expect(getPatientList).not.toBeCalled();

    wrapper.setProps({ isActive: true });
    wrapper.vm.$store.state.project.currentProject.value = '12312';
    await flushPromises();
    expect(getPatientList).toBeCalledWith('12312');
  });

  it('watch:isActive, if true & currentProject, getPatientList', () => {
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');

    wrapper.setProps({ isActive: false });
    wrapper.setProps({ isActive: true });
    expect(wrapper.vm.items).toEqual([]);
    expect(getPatientList).toBeCalledWith(wrapper.vm.currentProject);
  });

  it('getPatientList', async () => {
    const projectId = 'projectId';
    // const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');
    const patientList = [{
      sequence: 1,
      patientName: 'alex',
      updatedAt: '2010-10-10',
      patientId: 'xxxx',
    }];
    patientAPI.filteredPatients = jest.fn().mockResolvedValue(patientList);
    wrapper.vm.getPatientList(projectId);
    expect(wrapper.vm.loading).toEqual(true);

    await flushPromises();
    expect(wrapper.vm.loading).toEqual(false);
    expect(wrapper.vm.items).toEqual(patientList);
  });

  it('load file list of a patient when open the dropdown:SUCCESS CASE', async () => {
    const patientId = 'patientID';
    const item = { patientId, projectId: wrapper.vm.currentProject };
    const records = [{
      sequence: 1,
      fileName: 'Audio1',
      fileType: 'mp3',
      status: 'Incomplete',
      uploadedAt: '12-12-2019',
    }];
    fileAPI.filterRecords = jest.fn().mockResolvedValue(records);

    wrapper.vm.getPatientRecords({ item });
    expect(wrapper.vm.fileLoading).toBe(true);
    expect(wrapper.vm.patientRecords).toEqual([]);
    await flushPromises();
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.patientRecords).toEqual(records);
    fileAPI.filterRecords.mockClear();
  });

  it('load file list of a patient when open the dropdown:ERROR CASE', async () => {
    fileAPI.filterRecords = jest.fn().mockRejectedValue('error');
    wrapper.vm.getPatientRecords({ item: { patientId: 'xx', projectId: 'xx' } });

    expect(wrapper.vm.fileLoading).toBe(true);
    expect(wrapper.vm.patientRecords).toEqual([]);
    await flushPromises();
    expect(wrapper.vm.fileLoadingError).toBe('error');
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.patientRecords).toEqual([]);
  });


  it('resetData', () => {
    wrapper.vm.resetData();
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.fileLoadingError).toBe('');
    expect(wrapper.vm.items).toEqual([]);
    expect(wrapper.vm.patientRecords).toEqual([]);
  });

  it('startUploading', () => {
    const record = 'record';
    wrapper.vm.startUploading(record);
    expect(wrapper.vm.patientRecords).toContain(record);
  });

  it('addUploadingFile', () => {
    const file = 'file 0';
    const patientRecords = [{ files: ['file 1', 'file 2'] }];
    wrapper.setData({ patientRecords });
    wrapper.vm.addUploadingFile(file);
    expect(wrapper.vm.patientRecords[0].files[0]).toBe(file);
  });

  it('expandRow', async () => {
    const getPatientRecords = jest.spyOn(wrapper.vm, 'getPatientRecords');
    const patientId = 'patientId';
    const clickedRow = { patientId };
    wrapper.setData({ expandedItems: [{ patientId }] });

    wrapper.vm.expandRow(clickedRow);
    expect(wrapper.vm.expandedItems).toEqual([]);

    await wrapper.vm.expandRow(clickedRow);
    expect(getPatientRecords).toBeCalledWith({ item: clickedRow });
    expect(wrapper.vm.expandedItems).toEqual([clickedRow]);
  });
});
