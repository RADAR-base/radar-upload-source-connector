/* eslint-disable no-undef */
import { shallowMount } from '@vue/test-utils';
import AppBar from '../AppBar.vue';
import auth from '@/axios/auth';


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
    const localStoreRemoveToken = jest.spyOn(localStorage.__proto__, 'removeItem');
    wrapper.vm.logout();
    expect(localStoreRemoveToken).toBeCalledWith('token');
    expect(wrapper.vm.$router.replace).toBeCalledWith('/login');
    expect(auth.logout).toBeCalled();
  });
});
