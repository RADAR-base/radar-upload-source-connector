/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import FileList from '../FileList.vue';

// eslint-disable-next-line no-undef
describe('ProjectFilter', () => {
  const files = [
    {
      sequence: 1,
      fileName: 'Audio1',
      fileType: 'mp3',
      status: 'Incomplete',
      uploadedAt: 1565186733673,
    },
  ];
  const wrapper = shallowMount(FileList, {
    propsData: {
      patientFiles: files,
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
    ],
    filters: {
      moment: () => 'filteredDate',
    },
  });
  it(' get and render props patientFiles correctly', () => {
    expect(wrapper.vm.patientFiles).toEqual(files);
    expect(wrapper.vm.loading).toEqual(false);
    expect(wrapper.vm.error).toEqual('');
    expect(wrapper.text()).toContain(files[0].fileName);
    expect(wrapper.text()).toContain('filteredDate');
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
