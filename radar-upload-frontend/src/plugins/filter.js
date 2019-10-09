import Vue from 'vue';
import { truncate } from 'lodash';

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

Vue.filter('localTime', (val) => {
  if (!val) return '';
  return (new Date(val)).toLocaleString();
});

Vue.filter('textTruncate', (val, length = 24) => {
  if (!val) return;
  // eslint-disable-next-line consistent-return
  return truncate(val, { length });
});


Vue.filter('toMB', (val) => {
  if (!val) return 0;
  return `${(Number(val) / (1024 * 1024)).toFixed(2)} MB`;
});
