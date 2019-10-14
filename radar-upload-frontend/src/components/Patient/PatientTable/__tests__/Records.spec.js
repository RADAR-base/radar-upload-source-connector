/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import Records from '../Records.vue';
import fileAPI from '@/axios/file';
// eslint-disable-next-line no-undef
describe('Records', () => {
  const patientRecords = [
    {
      active: false,
      callbackUrl: null,
      committedDate: null,
      createdDate: '2019-08-21T07:51:05.666391Z',
      files: ['file'],
      id: 6,
      logs: null,
      message: 'Data successfully uploaded, ready for processing.',
      modifiedDate: '2019-08-21T07:51:05.767623Z',
      revision: 2,
      sourceType: 'phone-acceleration',
      status: 'READY',
      userId: 'b7897d2b-4e7a-43b9-8a51-735c112b4c2a',
    },
  ];
  const wrapper = shallowMount(Records, {
    propsData: {
      patientRecords,
      error: '',
      loading: false,
    },
    mocks: {
      $error: jest.fn(),
    },
    stubs: [
      'v-list',
      'v-subheader',
      'v-list-item-group',
      'v-list-item',
      'v-list-item-icon',
      'v-list-item-content',
      'v-list-item-title',
      'v-list-item-subtitle',
      'v-list-item-action',
      'v-icon',
      'v-alert',
      'v-progress-circular',
      'v-layout',
      'v-list-group',
      'v-list-item-avatar',
      'v-card',
      'v-card-text',
      'v-card-title',
      'v-dialog',
      'v-divider',

    ],
    filters: {
      localTime: () => 'filteredDate',
    },
  });

  it('get and render props patientRecords correctly', () => {
    expect(wrapper.vm.patientRecords).toEqual(patientRecords);
    expect(wrapper.vm.loading).toEqual(false);
    expect(wrapper.vm.error).toEqual('');
  });

  it('show error  and loading if any', () => {
    expect(wrapper.find('v-progress-circular-stub').isVisible()).toBe(false);
    expect(wrapper.find('v-alert-stub').isVisible()).toBe(false);
    wrapper.setProps({ error: 'error', loading: true });
    expect(wrapper.find('v-progress-circular-stub').isVisible()).toBe(true);
    expect(wrapper.find('v-alert-stub').isVisible()).toBe(true);
  });

  it('viewlogs', async () => {
    const url = 'logs url';
    const logs = 'logs';
    fileAPI.getRecordLog = jest.fn().mockResolvedValue(logs);
    wrapper.vm.viewLogs(url);
    expect(wrapper.vm.loadingLog).toBe(true);
    await flushPromises();
    expect(fileAPI.getRecordLog).toBeCalledWith(url);
    expect(wrapper.vm.recordLogs).toBe(logs);
    expect(wrapper.vm.loadingLog).toBe(false);


    // fail case;
    fileAPI.getRecordLog.mockClear();
    fileAPI.getRecordLog = jest.fn().mockRejectedValue('');
    wrapper.vm.viewLogs(url);
    await flushPromises();
    expect(wrapper.vm.$error).toBeCalled();
    expect(wrapper.vm.recordLogs).toBe('');
    expect(wrapper.vm.dialog).toBe(false);
  });

  it('deleteRecord', () => {
    fileAPI.deleteRecord = jest.fn();
    const recordId = 'recordId';
    const revision = 'revision';
    const recordIndex = 0;
    wrapper.setData({ records: ['record0', 'record1'] });
    wrapper.vm.deleteRecord({ recordId, revision, recordIndex });
    expect(fileAPI.deleteRecord).toBeCalledWith({ recordId, revision });
    expect(wrapper.vm.records).toEqual(['record1']);
  });
});
