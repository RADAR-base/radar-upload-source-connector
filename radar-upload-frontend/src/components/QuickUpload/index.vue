<template>
  <v-bottom-sheet
    v-model="sheet"
    inset
    persistent
  >
    <template v-slot:activator="{ on }">
      <v-btn
        color="primary lighten-1"
        fab
        class="mr-2"
        small
        depressed
        v-on="on"
      >
        <v-icon dark>
          mdi-cloud-upload-outline
        </v-icon>
      </v-btn>
    </template>
    <v-sheet>
      <UploadForm
        :upload-info="{projectId:currentProject}"
        :patient-list="patientList"
        :loading="loading"
        @finishUpload="finishUpload"
        @cancelClick="cancelUpload"
        @creatingRecord="loading=true"
        @uploadFailed="cancelUpload"
      />
    </v-sheet>
  </v-bottom-sheet>
</template>

<script>
import UploadForm from '@/components/Common/UploadForm.vue';
import patientAPI from '@/axios/patient';

export default {
  components: { UploadForm },
  data() {
    return {
      sheet: false,
      loading: false,
      patientList: [],
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.value;
    },
  },
  watch: {
    sheet(open) {
      if (open) {
        this.getPatientList(this.currentProject);
      }
    },
  },
  methods: {
    async getPatientList(projectId) {
      this.patientList = [];
      this.loading = true;
      const patientList = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.patientList = patientList
        .map(patient => ({ text: patient.patientName, value: patient.patientId }));
      this.loading = false;
    },
    finishUpload() {
      this.$success('Upload successfully');
      this.loading = false;
      this.sheet = false;
    },
    cancelUpload() {
      this.patientList = [];
      this.loading = false;
      this.sheet = false;
    },
  },
};
</script>
<style>

</style>
