<template>
  <v-navigation-drawer
    :value="drawer"
    app
    clipped
    @input="(val)=>$emit('transitionend', val)"
  >
    <!-- <template #prepend>
      <ProjectFilter />
      <v-divider />
    </template> -->

    <template>
      <ProjectList />
    </template>
  </v-navigation-drawer>
</template>

<script>
import ProjectList from '@/components/Project/ProjectList.vue';
// import ProjectFilter from '@/components/Project/ProjectFilter.vue';

export default {
  components: {
    ProjectList,
    // ProjectFilter,
  },
  props: {
    drawer: {
      type: Boolean,
      default: true,
    },
  },
};
</script>

<style>

</style>
