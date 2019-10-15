<template>
  <div v-show="currentProject">
    <v-card outlined>
      <!-- <v-app-bar-nav-icon /> -->

      <v-card-title>
        <span class="pr-2">{{ currentProject }} </span>
        <Upload />
      </v-card-title>

      <v-tabs
        v-model="tab"
        right
      >
        <v-tab
          active-class
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
    </v-card>


    <v-card
      class="mt-2"
      outlined
    >
      <v-tabs-items
        v-model="tab"
      >
        <v-tab-item :value="0">
          <PatientTable />
        </v-tab-item>

        <v-tab-item :value="1">
          <RecordTable :is-active="tab==1" />
        </v-tab-item>
      </v-tabs-items>
    </v-card>
  </div>
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
