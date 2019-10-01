/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import RecordTable from '../RecordTable.vue';
import fileAPI from '@/axios/file';

describe('RecordTable', () => {
  // call this api when component is created
  const mockSourceType = [{ name: 'sourceType' }];
  fileAPI.getSourceTypes = jest.fn().mockResolvedValue(mockSourceType);
  const projectID = '1111';
  const $store = new Store({
    state: {
      project: {
        currentProject: {
          value: projectID,
        },
      },
      file: {
        searchText: 'search text',
      },
    },
  });
  afterEach(() => {
    jest.clearAllMocks();
  });
  const wrapper = shallowMount(RecordTable, {
    mocks: {
      $store,
      $error: jest.fn(),
    },
    filter: {
      moment: () => jest.fn(),
    },
    stubs: ['v-data-table'],
  });
  it('filterRecordList', () => {
    const getRecordList = jest.spyOn(wrapper.vm, 'getRecordList');
    wrapper.vm.filterRecordList();
    expect(wrapper.vm.options.page).toBe(1);
    expect(getRecordList).toBeCalled();
  });

  it('getRecordList: CASE SUCCESS', async () => {
    const tableData = ['record List'];
    const totalElements = 10;
    const {
      status, sourceType, participantId, currentProject,
    } = wrapper.vm;
    const { page, itemsPerPage } = wrapper.vm.options;

    fileAPI.filterRecords = jest.fn().mockResolvedValue({ tableData, totalElements });
    wrapper.vm.getRecordList();
    expect(wrapper.vm.recordList).toEqual([]);
    expect(wrapper.vm.loading).toBe(true);
    await flushPromises();
    expect(fileAPI.filterRecords).toBeCalledWith({
      projectId: currentProject,
      page,
      size: itemsPerPage,
      status,
      sourceType,
      userId: participantId,
    });
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.recordList).toEqual(tableData);
    expect(wrapper.vm.serverItemsLength).toBe(totalElements);
  });

  it('getRecordList: CASE ERROR', async () => {
    const projectId = 'project id';
    fileAPI.filterRecords = jest.fn().mockRejectedValue('rejectedValue');
    wrapper.vm.getRecordList({ projectId });
    expect(wrapper.vm.loading).toBe(true);
    expect(wrapper.vm.recordList).toEqual([]);
    await flushPromises();
    expect(wrapper.vm.recordList).toEqual([]);
    expect(wrapper.vm.serverItemsLength).toBe(0);
    expect(wrapper.vm.loading).toBe(false);
  });

  it('expandRow', async () => {
    const clickedRow = { };
    wrapper.setData({ expandedItems: [{ }] });

    wrapper.vm.expandRow(clickedRow);
    expect(wrapper.vm.expandedItems).toEqual([]);

    await wrapper.vm.expandRow(clickedRow);
    expect(wrapper.vm.expandedItems).toEqual([clickedRow]);
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
});
