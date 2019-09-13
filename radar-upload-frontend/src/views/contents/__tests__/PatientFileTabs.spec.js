/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import PatientFileTabs from '../PatientFileTabs.vue';


describe('PatientFileTabs', () => {
  // call this api when component is created
  const CURRENT_PROJECT = { text: 'xxxxx', value: 1 };
  const $store = new Store(
    {
      state: {
        project: {
          currentProject: CURRENT_PROJECT,
        },
      },
    },
  );
  const wrapper = shallowMount(PatientFileTabs, {
    propsData: {
    },
    mocks: {
      $store,
    },
    stubs: [
      'v-card',
      'v-tab-item',
      'v-tabs-items',
      'v-icon',
      'v-tab',
      'v-toolbar',
      'v-toolbar-title',
      'v-spacer',
      'v-tabs',
    ],
  });


  it('toggle PatientFilter and FileFilter on tabs changes', () => {
    wrapper.setData({ tab: 0 });
    expect(wrapper.find('patientfilter-stub').isVisible()).toBe(true);
    expect(wrapper.find('recordfilter-stub').isVisible()).toBe(false);
    wrapper.setData({ tab: 1 });
    expect(wrapper.find('patientfilter-stub').isVisible()).toBe(false);
    expect(wrapper.find('recordfilter-stub').isVisible()).toBe(true);
  });

  it('display current selected project', () => {
    expect(wrapper.text()).toContain(CURRENT_PROJECT.text);
  });
});
