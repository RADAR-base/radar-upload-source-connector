<template>
  <v-text-field
    v-model="text"
    class="mt-4"
    prepend-inner-icon="mdi-magnify"
    label="Search the patients"
    @input="handleSearch"
  />
</template>

<script>
export default {
  data() {
    return {
      searchText: '',
    };
  },
  methods: {
    handleSearch(val) {
      this.$store.commit('patient/handleSearch', val);
    },
  },
};
</script>

<style>

</style>
