/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import FileTable from '../FileTable.vue';
import fileAPI from '@/axios/file';

describe('FileTable', () => {
  // call this api when component is created
  const projectID = '1111';
  const $store = new Store({
    state: {
      project: {
        value: projectID,
      },
    },
  });
  const wrapper = shallowMount(FileTable, {
    propsData: {
      isActive: false,
      currentProject: projectID,
    },
    mocks: {
      $store,
    },
    filter: {
      moment: () => jest.fn(),
    },
    stubs: ['v-data-table'],
  });

  it('has isAtive props', () => {
    expect(wrapper.vm.isActive).toBe(false);
    expect(wrapper.vm.currentProject).toBe(projectID);
  });

  it('call api to get file list if it is active tab and a project is selected', async () => {
    const resolvedValue = [{ name: 'name' }];
    fileAPI.filterRecords = jest.fn().mockResolvedValue(resolvedValue);
    wrapper.setProps({ isActive: true });
    wrapper.setData({ currentProject: 'watchingProject' });

    expect(wrapper.vm.loading).toBe(true);
    await flushPromises();
    expect(fileAPI.filterRecords).toBeCalledWith({ projectId: 'watchingProject' });
    expect(wrapper.vm.items).toEqual(resolvedValue);
    expect(wrapper.vm.loading).toBe(false);
  });

  it('return empty array when call api with error or none project is selected', async () => {
    wrapper.setData({ currentProject: '' });
    expect(wrapper.vm.items).toEqual([]);

    fileAPI.filterRecords = jest.fn().mockRejectedValue('rejectedValue');
    wrapper.setData({ currentProject: 'anotherSelectProject' });

    expect(wrapper.vm.loading).toBe(true);
    await flushPromises();
    expect(fileAPI.filterRecords).toBeCalledWith({ projectId: 'anotherSelectProject' });
    expect(wrapper.vm.items).toEqual([]);
    expect(wrapper.vm.loading).toBe(false);
  });
});
