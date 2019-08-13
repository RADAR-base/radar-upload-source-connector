<template>
  <v-data-table
    :items="items"
    single-expand
    show-expand
    hide-default-header
    item-key="sequence"
    :loading="loading"
    loading-text="Downloading patients info, please wait"
    @item-expanded="getPatientFiles"
  >
    <template #item.updatedAt="{item}">
      <td class="pl-0">
        {{ item.updatedAt | moment("YYYY/MM/DD") }}
      </td>
    </template>


    <template #item.uploadButton="{item}">
      <!-- {{ item }} -->
      <td class="pl-0">
        <UploadButton />
      </td>
    </template>


    <template #expanded-item="{item}">
      <td
        :colspan="12"
        class="py-4"
      >
        <FileList
          :patient-files="patientFiles"
          :loading="fileLoading"
          :error="fileLoadingError"
        />
      </td>
    </template>
  </v-data-table>
</template>

<script>
import UploadButton from './UploadButton';
import FileList from './FileList';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file';

export default {
  components: {
    UploadButton,
    FileList,
  },
  props: {
    isActive: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      currentProject: this.$store.state.project.currentProject.value,
      loading: false,
      fileLoading: false,
      fileLoadingError: '',

      items: [
        {
          sequence: 1,
          patientName: 'Patient 1',
          updatedAt: Date.now(),
          patientId: 'xx',
        },
        {
          sequence: 2,
          patientName: 'Patient 2',
          updatedAt: Date.now(),
          patientId: 'xxx',
        },
      ],
      patientFiles: [
        {
          sequence: 1,
          fileName: 'Audio1',
          fileType: 'mp3',
          status: 'Incomplete',
          uploadedAt: Date.now(),
        },
        {
          sequence: 2,
          fileName: 'Checklist',
          fileType: 'text',
          status: 'Completed',
          uploadedAt: Date.now(),
        },
        {
          sequence: 1,
          fileName: 'Audio1',
          fileType: 'mp3',
          status: 'Incomplete',
          uploadedAt: Date.now(),
        },
        {
          sequence: 2,
          fileName: 'Checklist',
          fileType: 'text',
          status: 'Completed',
          uploadedAt: Date.now(),
        },
      ],
      // headers: [
      //   { text: '#', value: 'sequence' },
      //   { text: 'Patient name', value: 'patientName' },
      //   { text: 'Updated at', value: 'updatedAt' },
      //   { text: 'Upload', value: 'uploadButton', sortable: false },
      // ],
    };
  },
  watch: {
    currentProject: {
      handler(val) {
        if (!val) {
          this.items = [];
        } else if (this.isActive) {
          this.getPatientList(val);
        }
      },
      immediate: true,
    },
  },
  methods: {
    async getPatientList(projectId) {
      this.loading = true;
      const patientList = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.items = patientList;
      this.loading = false;
    },
    async getPatientFiles({ patientId }) {
      this.patientFiles = [];
      this.fileLoading = true;
      const files = await fileAPI
        .filterRecords({ userId: patientId, projectId: this.currentProject })
        .catch((error) => {
          this.fileLoadingError = error || 'Error when loading file, please try again later';
          return [];
        });
      this.patientFiles = files;
      this.fileLoading = false;
    },
    resetData() {
      this.loading = false;
      this.fileLoading = false;
      this.fileLoadingError = '';
      this.items = [];
      this.patientFiles = [];
    },
  },
};
</script>

<style>

</style>
