<template>
  <v-card v-show="currentProject">
    <v-toolbar
      flat
    >
      <!-- <v-app-bar-nav-icon /> -->

      <v-toolbar-title>{{ currentProject }}</v-toolbar-title>

      <v-spacer />

      <Upload />

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
            Participants
          </v-tab>

          <v-tab>
            <v-icon class="pr-2">
              mdi-file-multiple
            </v-icon>
            Records
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-tabs-items
      v-model="tab"
    >
      <v-card
        flat
      >
        <v-tab-item :value="0">
          <PatientTable />
        </v-tab-item>

        <v-tab-item :value="1">
          <RecordTable :is-active="tab==1" />
        </v-tab-item>
      </v-card>
    </v-tabs-items>
  </v-card>
</template>

<script>
import PatientTable from '@/components/Patient/PatientTable';
import RecordTable from '@/components/Record/RecordTable';
import Upload from '@/components/Upload';

export default {
  name: 'PatientFilterTabs',
  components: {
    RecordTable,
    PatientTable,
    Upload,
  },
  data() {
    return {
      tab: 0,
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.text
      || this.$store.state.project.currentProject.value;
    },
  },
};
</script>

<style>

</style>
