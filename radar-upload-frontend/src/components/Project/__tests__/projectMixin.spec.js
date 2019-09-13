/* eslint-disable no-undef */
import flushPromise from 'flush-promises';
import { shallowMount } from '@vue/test-utils';
import projectMixin from '../projectMixin';
import api from '@/axios/project.js';

const mockComponent = { template: '<div></div>' };

describe('projectMixin', () => {
  const $store = {
    commit: jest.fn(),
  };

  const wrapper = shallowMount(mockComponent, {
    mixins: [projectMixin],
    mocks: {
      $store,
      $router: {
        push: jest.fn(),
      },
      $route: {
        path: '/some-path',
      },
    },
  });

  it('getProjects CASE:SUCCESS', async () => {
    const projects = ['project'];
    api.getProjects = jest.fn().mockResolvedValue(projects);

    wrapper.vm.getProjects();

    expect(wrapper.vm.loading).toBe(true);

    await flushPromise();

    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.projects).toEqual(projects);
  });

  it('getProjects CASE:ERROR', async () => {
    api.getProjects = jest.fn().mockRejectedValue();

    wrapper.vm.getProjects();
    expect(wrapper.vm.loading).toBe(true);

    await flushPromise();
    expect(wrapper.vm.errorMessage).toBe('Loading project fails, please try again later');
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.projects).toEqual([]);
  });

  it('selectProject', async () => {
    // const selectProject = jest.spyOn(wrapper.vm, 'selectProject');
    const project = { value: 1 };
    wrapper.vm.selectProject(project);
    expect($store.commit).toBeCalledWith('project/setCurrentProject', project);
    expect(wrapper.vm.$router.push).toBeCalledWith(`/projects/${project.value}`);
  });
});
