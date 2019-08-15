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
    },
  });

  const wrapper = shallowMount(index, {
    propsData: {
      isActive: false,
    },
    computed: {
      currentProject() { return this.$store.state.project.currentProject.value; },
    },
    mocks: {
      $store,
    },
    stubs: ['v-data-table'],
  });

  it('not show the table if no project is selected', () => {
    expect(wrapper.vm.items).toEqual([]);
  });

  it('call api to load patient list when a project selected and tab is active', async () => {
    const patientList = [{
      sequence: 1,
      patientName: 'alex',
      updatedAt: '2010-10-10',
      patientId: 'xxxx',
    }];
    patientAPI.filteredPatients = jest.fn().mockResolvedValue(patientList);
    wrapper.setData({ currentProject: 'originalProject' });
    expect(patientAPI.filteredPatients).not.toBeCalledWith('originalProject');

    wrapper.setProps({ isActive: true });
    wrapper.vm.$store.state.project.currentProject.value = '12312';
    await flushPromises();

    expect(patientAPI.filteredPatients).toBeCalledWith('12312');
  });

  it('load file list of a patient when open the dropdown', async () => {
    const patientId = 'patientID';
    const fileList = [{
      sequence: 1,
      fileName: 'Audio1',
      fileType: 'mp3',
      status: 'Incomplete',
      uploadedAt: '12-12-2019',
    }];
    fileAPI.filterRecords = jest.fn().mockResolvedValue(fileList);

    wrapper.vm.getPatientFiles({ patientId });
    expect(wrapper.vm.fileLoading).toBe(true);
    await flushPromises();
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.patientFiles).toEqual(fileList);
    fileAPI.filterRecords.mockClear();
  });

  it('load file return error message and empty array', async () => {
    const error = 'api errors';
    fileAPI.filterRecords = jest.fn().mockRejectedValue(error);
    wrapper.vm.getPatientFiles({ patientId: 'id' });

    expect(wrapper.vm.fileLoading).toBe(true);
    await flushPromises();
    expect(wrapper.vm.fileLoadingError).toBe(error);
    expect(wrapper.vm.patientFiles).toEqual([]);
    expect(wrapper.vm.fileLoading).toBe(false);
  });

  it('resetData', () => {
    wrapper.vm.resetData();
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.fileLoading).toBe(false);
    expect(wrapper.vm.fileLoadingError).toBe('');
    expect(wrapper.vm.items).toEqual([]);
    expect(wrapper.vm.patientFiles).toEqual([]);
  });
});
