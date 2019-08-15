<template>
  <v-card>
    <v-toolbar
      flat
    >
      <!-- <v-app-bar-nav-icon /> -->

      <v-toolbar-title>{{ $store.state.project.currentProject.text }}</v-toolbar-title>

      <v-spacer />

      <v-toolbar-title>
        <PatientFilter v-show="tab==0" />
        <FileFilter v-show="tab==1" />
      </v-toolbar-title>

      <template #extension>
        <v-tabs
          v-model="tab"
        >
          <v-tabs-slider />
          <v-tab
            active-class
            class="ml-0"
          >
            <v-icon class="pr-2">
              mdi-account-box-multiple
            </v-icon>
            Patients
          </v-tab>

          <v-tab>
            <v-icon class="pr-2">
              mdi-file-multiple
            </v-icon>
            Files
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-tabs-items
      v-model="tab"
    >
      <v-tab-item :value="0">
        <v-card
          flat
          class="mt-4 pa-2"
        >
          <PatientTable :is-active="tab==0" />
        </v-card>
      </v-tab-item>
      <v-tab-item
        :value="1"
      >
        <v-card
          flat
          class="mt-4 pa-2"
        >
          <FileTable :is-active="tab==1" />
        </v-card>
      </v-tab-item>
    </v-tabs-items>
  </v-card>
</template>

<script>
import PatientTable from '@/components/Patient/PatientTable';
import PatientFilter from '@/components/Patient/PatientFilter';
import FileTable from '@/components/File/FileTable';
import FileFilter from '@/components/File/FileFilter';

export default {
  name: 'PatientFilterTabs',
  components: {
    FileTable,
    PatientTable,
    FileFilter,
    PatientFilter,
  },
  data() {
    return {
      tab: 0,
    };
  },
};
</script>

<style>

</style>
