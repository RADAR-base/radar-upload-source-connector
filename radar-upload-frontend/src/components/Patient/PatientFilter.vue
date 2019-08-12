<template>
  <v-text-field
    v-model="autocomplete"
    class="mt-4"
    prepend-inner-icon="mdi-magnify"
    label="Search your patients"
    :items="items"
    deletable-chips
    chips
  />
</template>

<script>
export default {

};
</script>

<style>

</style>
