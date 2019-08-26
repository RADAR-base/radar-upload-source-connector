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

  it('match snapShot', () => {
    expect(wrapper.html()).toMatchSnapshot();
  });
});
