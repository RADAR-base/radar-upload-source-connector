<template>
  <v-text-field
    v-model="text"
    class="mt-4"
    prepend-inner-icon="mdi-magnify"
    label="Search your patients"
    deletable-chips
    chips
  />
</template>

<script>
export default {
  data() {
    return {
      text: '',
    };
  },
};
</script>

<style>

</style>
