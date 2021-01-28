/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import auth from '@/axios/auth';
import AppBar from '../AppBar.vue';

describe('AppBar', () => {
  // call this api when component is created
  const wrapper = shallowMount(AppBar, {
    mocks: {
      $router: {
        replace: jest.fn(),
      },
      $route: {
        path: jest.fn(),
      },
    },
    stubs: ['v-app-bar', 'v-app-bar-nav-icon', 'v-toolbar-title', 'v-spacer', 'v-icon'],
  });

  it('logout', () => {
    auth.logout = jest.fn();
    // eslint-disable-next-line no-proto
    wrapper.vm.logout();
    expect(wrapper.vm.$router.replace).toBeCalledWith({ name: 'Login' });
    expect(auth.logout).toBeCalled();
  });
});
