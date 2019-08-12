global.expectText = (wrapper, content) => {
  expect(wrapper.html()).toContain(content);
};

global.expectNoText = (wrapper, content) => {
  expect(wrapper.html()).not.toContain(content);
};

global.expectToFind = (wrapper, selector) => {
  expect(wrapper.find(selector).exists()).toBe(true);
};

global.expectNotToFind = (wrapper, selector) => {
  expect(wrapper.find(selector).exists()).toBe(false);
};

global.triggerClick = (wrapper, button) => {
  const buttonWrapper = wrapper.find(button);
  buttonWrapper.trigger('click');
};
