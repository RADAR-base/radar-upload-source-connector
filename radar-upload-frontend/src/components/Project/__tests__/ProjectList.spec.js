/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import ProjectList from '../ProjectList.vue';
import api from '@/axios/project.js';

const projectId = 'id1';
const projectName = 'name1';
api.getProjects = jest.fn().mockResolvedValue([{
  id: projectId,
  name: projectName,
  location: 'Utrecht',
  organization: 'The Hyve',
  description: 'my test project',
}]);


describe('ProjectList', () => {
  const store = new Store();
  afterEach(() => {
    api.getProjects.mockClear();
  });


  const wrapper = shallowMount(ProjectList, {
    mocks: {
      $store: store,
    },
    stubs: ['v-list',
      'v-list-item-group',
      'v-list-item',
      'v-list-item-icon',
      'v-icon',
      'v-list-item-content',
      'v-list-item-title',
    ],
    propsData: {},
  });

  it('call getProjects api to get correct projects value', async () => {
    const transformedProjects = [{ value: projectId, text: projectName }];
    expect(wrapper.vm.projects).toEqual(transformedProjects);
  });

  it('dispatch actions with corresponding project id when clicking a project', () => {
    const projectItem = wrapper.findAll('v-list-item-stub').at(0);
    projectItem.trigger('click');
    expect(store.dispatch).toBeCalledWith('project/selectProject', projectId);
  });

  it('match snapShot', () => {
    expect(wrapper.html()).toMatchSnapshot();
  });
});
