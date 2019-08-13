/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import ProjectList from '../ProjectList.vue';
import api from '@/axios/project.js';

const projectId = 'id1';
const projectName = 'name1';
const transformedProjects = [{ value: projectId, text: projectName }];

api.getProjects = jest.fn().mockResolvedValue([{
  id: projectId,
  name: projectName,
  location: 'Utrecht',
  organization: 'The Hyve',
  description: 'my test project',
}]);


describe('ProjectList', () => {
  const $store = new Store();
  afterEach(() => {
    api.getProjects.mockClear();
  });


  const wrapper = shallowMount(ProjectList, {
    mocks: {
      $store,
    },
    stubs: ['v-list',
      'v-list-item-group',
      'v-list-item',
      'v-list-item-icon',
      'v-icon',
      'v-list-item-content',
      'v-list-item-title',
      'v-progress-circular',
      'v-layout',
      'v-alert',
    ],
    propsData: {},
  });
  it('show loader when loading the list', () => {
    const loader = wrapper.find('v-progress-circular-stub');
    wrapper.setData({ loading: true });
    expect(loader.isVisible()).toBe(true);
    wrapper.setData({ loading: false });
    expect(loader.isVisible()).toBe(false);
  });


  it('call getProjects api to get correct projects value', async () => {
    expect(wrapper.vm.projects).toEqual(transformedProjects);
    expect(wrapper.find('v-alert-stub').isVisible()).toBe(false);
    expect(wrapper.find('v-alert-stub').text()).toBe('');
  });

  it('show error messages if loading project list fails', () => {
    api.getProjects.mockRejectedValue();
    const errorMessage = 'Loading fails';

    expect(wrapper.find('v-alert-stub').isVisible()).toBe(false);
    wrapper.setData({ errorMessage });
    expect(wrapper.text()).toContain(errorMessage);
    expect(wrapper.find('v-alert-stub').isVisible()).toBe(true);
  });

  it('mutate  with corresponding project id when clicking a project', () => {
    const projectItem = wrapper.findAll('v-list-item-stub').at(0);
    projectItem.trigger('click');
    expect($store.commit).toBeCalledWith('project/setCurrentProject', transformedProjects[0]);
  });

  it('match snapShot', () => {
    expect(wrapper.html()).toMatchSnapshot();
  });
});
