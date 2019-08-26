<template>
  <v-text-field
    v-model="text"
    class="mt-4"
    prepend-inner-icon="mdi-magnify"
    label="Search the patients"
    @input="handleSearch(val)"
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
      this.$store.commit('patient', val);
    },
  },
};
</script>

<style>

</style>
