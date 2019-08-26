/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import PatientFilter from '../PatientFilter.vue';


describe('PatientFilter', () => {
  // call this api when component is created
  const $store = new Store();
  const wrapper = shallowMount(PatientFilter, {
    propsData: {
    },
    mocks: {
      $store,
    },
    stubs: ['v-text-field'],
  });

  it('handleSearch: commit searchText', () => {
    const handleSearch = jest.spyOn(wrapper.vm, 'handleSearch');
    const searchText = 'search ';
    handleSearch(searchText);
    expect($store.commit).toBeCalledWith('patient/handleSearch', searchText);
  });
});
