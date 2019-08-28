/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import RecordFilter from '../RecordFilter.vue';


describe('RecordFilter', () => {
  // call this api when component is created
  const $store = new Store();
  const wrapper = shallowMount(RecordFilter, {
    propsData: {
    },
    mocks: {
      $store,
    },
    stubs: ['v-text-field'],
  });

  it('handleSearch: commit search text', () => {
    const handleSearch = jest.spyOn(wrapper.vm, 'handleSearch');
    handleSearch('searchText');
    expect($store.commit).toBeCalledWith('file/handleSearch', 'searchText');
  });
});
