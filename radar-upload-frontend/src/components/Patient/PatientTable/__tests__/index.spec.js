/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file';
import index from '../index.vue';

describe.only('index', () => {
  // call this api when component is created
  const PROJECT_ID = '';
  let wrapper;
  beforeEach(() => {
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
    wrapper = shallowMount(index, {
      mocks: {
        $store,
      },
      stubs: ['v-data-table'],
    });
  });

  it('not show the table if no project is selected', () => {
    expect(wrapper.vm.items).toEqual([]);
  });

  it('watch: currentProject', async () => {
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');

    wrapper.setProps({ isActive: true });
    wrapper.vm.$store.state.project.currentProject.value = '12312';
    await flushPromises();
    expect(getPatientList).toBeCalledWith('12312');
  });

  it('getPatientList', async () => {
    const projectId = 'projectId';
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
    patientAPI.filteredPatients.mockClear();
  });

  it('getPatientRecords:SUCCESS CASE', async () => {
    const patientId = 'patientID';
    const item = { patientId, projectId: wrapper.vm.currentProject };
    const tableData = [{
      sequence: 1,
      fileName: 'Audio1',
      fileType: 'mp3',
      status: 'Incomplete',
      uploadedAt: '12-12-2019',
    }];
    fileAPI.filterRecords = jest.fn().mockResolvedValue({ tableData });

    wrapper.vm.getPatientRecords({ item });
    expect(wrapper.vm.fileLoading).toBe(true);
    expect(wrapper.vm.patientRecords).toEqual([]);
    await flushPromises();
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.patientRecords).toEqual(tableData);
    fileAPI.filterRecords.mockClear();
  });

  it('getPatientRecords:ERROR CASE', async () => {
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
