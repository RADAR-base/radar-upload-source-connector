/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import { Store } from 'vuex-mock-store';
import flushPromises from 'flush-promises';
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
  const $store = new Store();
  afterEach(() => {
    api.getProjects.mockClear();
  });


  const wrapper = shallowMount(ProjectList, {
    mocks: {
      $store,
      $route: {
        params: {
          projectId: '',
        },
      },
      $router: {
        push: jest.fn(),
      },
      $error: jest.fn(),
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

  it('watch: projects', () => {
    const selectProjectFromRoute = jest.spyOn(wrapper.vm, 'selectProjectFromRoute');
    wrapper.setData({ selectedProject: '12', projects: [1, 2] });
    expect(selectProjectFromRoute).not.toBeCalled();

    wrapper.setData({ selectedProject: '', projects: [1, 2, 3] });
    expect(selectProjectFromRoute).toBeCalled();
    selectProjectFromRoute.mockClear();
  });

  it('watch: $route', async () => {
    const selectProjectFromRoute = jest.spyOn(wrapper.vm, 'selectProjectFromRoute');
    wrapper.setData({ selectedProject: '12' });
    wrapper.setData({ $route: { params: { projectId: '12' } } });
    expect(selectProjectFromRoute).not.toBeCalled();

    await flushPromises();
    wrapper.setData({ selectedProject: '23' });
    wrapper.setData({ $route: { params: { projectId: 'randomNumber' } } });

    expect(selectProjectFromRoute).toBeCalled();
    selectProjectFromRoute.mockClear();
  });

  it('selectProjectFromRoute', async () => {
    wrapper.setMethods({ selectProject: jest.fn() });

    const selectedProject = { value: 'projectId' };
    wrapper.setData({ projects: [selectedProject] });

    // projectId in route different from that in projects
    wrapper.setData({ $route: { params: { projectId: 'differentPRojectId' } } });
    wrapper.vm.selectProjectFromRoute();
    expect(wrapper.vm.selectProject).not.toBeCalledWith(selectedProject);

    // projectId in route found in projects
    wrapper.setData({ $route: { params: { projectId: 'projectId' } } });
    wrapper.vm.selectProjectFromRoute();
    expect(wrapper.vm.selectProject).toBeCalledWith(selectedProject);
  });
});
