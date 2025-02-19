<template>
  <v-dialog
    v-model="dialog"
    persistent
    max-width="700px"
  >
    <template #activator="{ on }">
      <v-btn
        v-if="!buttonIsIcon"
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
          :disabled="isDisabled"
        >
          mdi-cloud-upload-outline
        </v-icon>
      </v-btn>
      <v-icon
        color="primary"
        v-if="!!buttonIsIcon"
        v-on="on"
        :disabled="isDisabled"
      >
        {{ buttonIsIcon }}
      </v-icon>
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
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file.js';
import UploadForm from './UploadForm.vue';

export default {
  props: {
    isNewUpload: {
      type: Boolean,
      default: true,
    },
    buttonIsIcon: {
      type: String,
      default: '',
    },
    isDisabled: {
      type: Boolean,
      default: false,
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
      if (this.buttonIsIcon) { return ''; }
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
      this.sourceTypeList = res.map((el) => el.name);
    },
    async getPatientList(projectId) {
      this.patientList = [];
      this.loading = true;
      const patientList = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.patientList = patientList
        .map((patient) => ({ text: patient.patientName, value: patient.patientId }));
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
