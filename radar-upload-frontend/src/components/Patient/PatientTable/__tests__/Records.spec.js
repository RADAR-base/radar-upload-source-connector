/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import Records from '../Records.vue';

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
    ],
    filters: {
      localTime: () => 'filteredDate',
    },
  });
  it(' get and render props patientRecords correctly', () => {
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
  // it('match snapShopt', () => {
  //   expect(wrapper.html()).toMatchSnapshot();
  // });
});
