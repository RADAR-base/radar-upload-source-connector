/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import flushPromises from 'flush-promises';
import { Store } from 'vuex-mock-store';
import PatientFilter from '../PatientFilter.vue';


describe('PatientFilter', () => {
  // call this api when component is created
  const store = new Store();
  const items = [
    { text: 'patient 1', value: 'patient 1' },
  ];
  const wrapper = shallowMount(PatientFilter, {
    propsData: {
    },
    mocks: {
      store,
    },
    stubs: ['v-text-field'],
  });

  it('call api to get a list of filted patients', () => {

  });
});
