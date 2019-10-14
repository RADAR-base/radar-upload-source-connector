/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import index from '../index.vue';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file.js';


describe('Upload', () => {
  // call this api when component is created
  const wrapper = shallowMount(index, {
    mocks: {
      $success: jest.fn(),
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

  it('watch:dialog => call getPatientList', () => {
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');
    const getsourceTypeList = jest.spyOn(wrapper.vm, 'getsourceTypeList');
    wrapper.setData({ dialog: false });
    wrapper.setData({ dialog: true });
    expect(getPatientList).toBeCalledWith(wrapper.vm.currentProject);
    expect(getsourceTypeList).toBeCalled();
  });

  it('getsourceTypeList', async () => {
    const sourceTypeList = [
      {
        name: 'audioMp3',
        sourceType: ['audio/mp3'],
      },
    ];
    fileAPI.getSourceTypes = jest.fn().mockReturnValue(sourceTypeList);
    wrapper.vm.getsourceTypeList();
    await flushPromises();
    expect(wrapper.vm.sourceTypeList).toEqual(sourceTypeList.map(el => el.name));
  });


  it('getPatientList: SUCCESS', async () => {
    // mock api
    const resolvedValue = [{ patientName: '', patientValue: '' }];
    patientAPI.filteredPatients = jest.fn().mockResolvedValueOnce(resolvedValue);

    wrapper.setData({ patientList: ['value'] });
    const projectId = 'projectId';

    wrapper.vm.getPatientList(projectId);

    expect(wrapper.vm.patientList).toEqual([]);
    expect(wrapper.vm.loading).toEqual(true);
    await flushPromises();
    expect(patientAPI.filteredPatients).toBeCalledWith(projectId);
    expect(wrapper.vm.patientList)
      .toEqual(resolvedValue
        .map(patient => ({ text: patient.patientName, value: patient.patientId })));
    patientAPI.filteredPatients.mockClear();
  });

  it('getPatientList: ERROR', async () => {
    patientAPI.filteredPatients = jest.fn().mockRejectedValueOnce();
    await wrapper.vm.getPatientList('projectId');
    expect(wrapper.vm.patientList).toEqual([]);
  });

  it('removeData', () => {
    wrapper.setData({ patientList: ['value'], loading: true, dialog: true });
    wrapper.vm.removeData();

    expect(wrapper.vm.patientList).toEqual([]);
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.dialog).toEqual(false);
  });
});
