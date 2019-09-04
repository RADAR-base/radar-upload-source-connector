<template>
  <div>
    <v-text-field
      v-model="searchText"
      class="mt-4"
      prepend-inner-icon="mdi-magnify"
      label="Search the patients"
      @input="handleSearch"
    />
  </div>
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
