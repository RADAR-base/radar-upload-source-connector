/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import index from '../index.vue';
import patientAPI from '@/axios/patient';


describe('QuickUpload', () => {
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
    stubs: ['v-bottom-sheet', 'v-sheet'],
  });

  it('watch:sheet => call getPatientList', () => {
    const getPatientList = jest.spyOn(wrapper.vm, 'getPatientList');
    wrapper.setData({ sheet: false });
    wrapper.setData({ sheet: true });
    expect(getPatientList).toBeCalledWith(wrapper.vm.currentProject);
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
  });

  it('getPatientList: ERROR', async () => {
    patientAPI.filteredPatients = jest.fn().mockRejectedValueOnce();
    await wrapper.vm.getPatientList('projectId');
    expect(wrapper.vm.patientList).toEqual([]);
  });

  it('finishUpload', () => {
    const removeData = jest.spyOn(wrapper.vm, 'removeData');
    wrapper.vm.finishUpload();
    expect(wrapper.vm.$success).toBeCalled();
    expect(removeData).toBeCalled();
  });

  it('removeData', () => {
    wrapper.setData({ patientList: ['value'], loading: true, sheet: true });
    wrapper.vm.removeData();

    expect(wrapper.vm.patientList).toEqual([]);
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.sheet).toEqual(false);
  });
});
