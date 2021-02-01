/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file.js';
import index from '../index.vue';

const sourceTypeList = [
  {
    name: 'audioMp3',
    sourceType: ['audio/mp3'],
  },
];
const patientListResolved = [{ patientName: '', patientValue: '' }];

describe('Upload', () => {
  // call this api when component is created
  const wrapper = shallowMount(index, {
    mocks: {
      $store: {
        state: {
          project: {
            currentProject: {
              value: 'value',
            },
          },
        },
      },
    },
    stubs: ['v-dialog'],
  });
  beforeEach(() => {
    // mock api
    fileAPI.getSourceTypes = jest.fn().mockResolvedValue(sourceTypeList);
    patientAPI.filteredPatients = jest.fn().mockResolvedValue(patientListResolved);
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  it('getsourceTypeList', async () => {
    wrapper.vm.getsourceTypeList();
    await flushPromises();
    expect(wrapper.vm.sourceTypeList).toEqual(sourceTypeList.map((el) => el.name));
  });

  it('getPatientList: SUCCESS', async () => {
    wrapper.setData({ patientList: ['value'] });
    const projectId = 'projectId';

    wrapper.vm.getPatientList(projectId);

    expect(wrapper.vm.patientList).toEqual([]);
    expect(wrapper.vm.loading).toEqual(true);
    await flushPromises();
    expect(patientAPI.filteredPatients).toBeCalledWith(projectId);
    expect(wrapper.vm.patientList)
      .toEqual(patientListResolved
        .map((patient) => ({ text: patient.patientName, value: patient.patientId })));
  });

  it('getPatientList: ERROR', async () => {
    patientAPI.filteredPatients = jest.fn().mockRejectedValueOnce();
    await wrapper.vm.getPatientList('projectId');
    expect(wrapper.vm.patientList).toEqual([]);
  });

  it('watch:dialog => call getPatientList', async () => {
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');
    const getsourceTypeList = jest.spyOn(wrapper.vm, 'getsourceTypeList');
    wrapper.setData({ dialog: false });
    await flushPromises();
    wrapper.setData({ dialog: true });
    await flushPromises();
    expect(getPatientList).toBeCalledWith(wrapper.vm.currentProject);
    expect(getsourceTypeList).toBeCalled();
  });

  it('removeData', () => {
    wrapper.setData({ patientList: ['value'], loading: true, dialog: true });
    wrapper.vm.removeData();

    expect(wrapper.vm.patientList).toEqual([]);
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.dialog).toEqual(false);
  });
});
