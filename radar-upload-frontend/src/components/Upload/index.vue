<template>
  <v-dialog
    v-model="dialog"
    persistent
    max-width="700px"
  >
    <template v-slot:activator="{ on }">
      <v-btn
        color="primary lighten-1"
        class="mr-2"
        :disabled="record&&record.status!=='INCOMPLETE'"
        v-on="on"
      >
        {{ buttonText }}
        <v-icon
          dark
          class="pl-2"
          v-show="isNewUpload"
        >
          mdi-cloud-upload-outline
        </v-icon>
      </v-btn>
    </template>
    <UploadForm
      @cancelClick="closeDialog"
      :patient-list="patientList"
      :source-type-list="sourceTypeList"
      :old-files="files"
      :old-record="record"
      :old-user-id="userId"
      :old-source-type="sourceType"
      :is-new-upload="isNewUpload"
      v-if="dialog"
      @finishEditRecord="(payload)=>$emit('finishEditRecord',payload)"
    />
  </v-dialog>
</template>

<script>
import UploadForm from './UploadForm.vue';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file.js';

export default {
  props: {
    isNewUpload: {
      type: Boolean,
      default: true,
    },
    userId: {
      type: String,
      default: '',
    },
    sourceType: {
      type: String,
      default: '',
    },
    record: {
      type: Object,
      default: null,
    },
    files: {
      type: Array,
      default: () => [],
    },
  },
  components: { UploadForm },
  data() {
    return {
      dialog: false,
      loading: false,
      patientList: [],
      sourceTypeList: [],
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.value;
    },
    buttonText() {
      return this.isNewUpload ? 'Upload' : 'Edit Record';
    },
  },
  watch: {
    dialog(open) {
      if (open) {
        this.getPatientList(this.currentProject);
        this.getsourceTypeList();
      }
    },
  },
  methods: {
    async getsourceTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.sourceTypeList = res.map(el => el.name);
    },
    async getPatientList(projectId) {
      this.patientList = [];
      this.loading = true;
      const patientList = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.patientList = patientList
        .map(patient => ({ text: patient.patientName, value: patient.patientId }));
      this.loading = false;
    },
    closeDialog() {
      this.removeData();
    },
    removeData() {
      this.patientList.splice(0);
      this.sourceTypeList.splice(0);
      this.loading = false;
      this.dialog = false;
    },
  },
};
</script>
<style>

</style>
