/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import { wrap } from 'module';
import RecordTable from '../RecordTable.vue';
import fileAPI from '@/axios/file';

describe('RecordTable', () => {
  // call this api when component is created
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
    propsData: {
      isActive: false,
      currentProject: projectID,
    },
    mocks: {
      $store,
      $error: jest.fn(),
    },
    filter: {
      moment: () => jest.fn(),
    },
    stubs: ['v-data-table'],
  });

  it('has isActive props', () => {
    expect(wrapper.vm.isActive).toBe(false);
    expect(wrapper.vm.currentProject).toBe(projectID);
  });

  it('updateOptions', () => {
    const getRecordList = jest.spyOn(wrapper.vm, 'getRecordList');
    const option = { itemsPerPage: 10, page: 10 };
    const projectId = wrapper.vm.currentProject;
    wrapper.vm.updateOptions(option);
    expect(getRecordList).toBeCalledWith({
      page: option.page,
      size: option.itemsPerPage,
      projectId,
    });
  });

  it('getRecordList: CASE SUCCESS', async () => {
    const projectId = 'projectId';
    const page = 10;
    const size = 100;
    const tableData = ['record List'];
    const totalElements = 10;
    fileAPI.filterRecords = jest.fn().mockResolvedValue({ tableData, totalElements });
    wrapper.vm.getRecordList({ projectId, page, size });
    expect(wrapper.vm.recordList).toEqual([]);
    expect(wrapper.vm.loading).toBe(true);
    await flushPromises();
    expect(fileAPI.filterRecords).toBeCalledWith({ projectId, page, size });
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

  it('downloadFile', () => {
    fileAPI.download = jest.fn();
    wrapper.vm.downloadFile('id', 'filename');
    expect(fileAPI.download).toBeCalledWith({ recordId: 'id', fileName: 'filename' });
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
