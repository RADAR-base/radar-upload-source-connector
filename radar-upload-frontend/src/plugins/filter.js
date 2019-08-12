import Vue from 'vue';

Vue.filter('capitalize', (value) => {
  if (!value) return '';
  const transformedValue = value.toString();
  return transformedValue.charAt(0).toUpperCase() + value.slice(1);
});

Vue.filter('addSpace', (value) => {
  if (!value) return '';
  const transformedValue = value.toString();
  return transformedValue.replace(/([A-Z0-9])/g, ' $1').trim();
});

Vue.filter('removeHyphen', (value) => {
  if (!value) return '';
  const transformedValue = value.toString();
  return transformedValue.replace(/-/g, ' ');
});

Vue.filter('upperCase', (value) => {
  if (!value) return '';
  return value.toString().toUpperCase();
});
