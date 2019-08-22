<template>
  <v-data-table
    :items="items"
    single-expand
    show-expand
    :headers="headers"
    hide-default-header
    item-key="sequence"
    :loading="loading"
    loading-text="Downloading patients info, please wait"
    @item-expanded="getPatientRecords"
  >
    <template #item.updatedAt="{item}">
      <td class="pl-0">
        {{ item.updatedAt | localTime }}
      </td>
    </template>


    <!-- <template #item.uploadButton="{item}">
      <td class="pl-0">
        <UploadButton
          :upload-info="{
            userId: item.patientId,
            projectId: currentProject
          }"
          @addUploadingFile="addUploadingFile"
          @startUploading="startUploading"
        />
      </td>
    </template> -->


    <template #expanded-item="{item}">
      <td
        :colspan="12"
        class="py-4"
      >
        <FileList
          :patient-records="patientRecords"
          :loading="fileLoading"
          :error="fileLoadingError"
        >
          <template #fileListSubHeader>
            <UploadButton
              :upload-info="{
                userId: item.patientId,
                projectId: currentProject
              }"
              @addUploadingFile="addUploadingFile"
              @startUploading="startUploading"
            />
          </template>
        </FileList>
      </td>
    </template>
  </v-data-table>
</template>

<script>
import { mapState } from 'vuex';
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
      loading: false,
      fileLoading: false,
      fileLoadingError: '',
      items: [
      ],
      patientRecords: [
      ],
      headers: [
        { text: '#', value: 'sequence' },
        { text: 'Patient name', value: 'patientName' },
        { text: 'Status', value: 'status' },
        // { text: 'Upload', value: 'uploadButton', sortable: false },
      ],
    };
  },
  computed: {
    ...mapState('project', {
      currentProject: state => state.currentProject.value,
    }),
  },
  watch: {
    currentProject: {
      handler(projectId) {
        if (projectId && this.isActive) {
          this.getPatientList(projectId);
        }
      },
    },
    isActive: {
      handler(val) {
        if (val && this.currentProject) {
          this.items = [];
          this.getPatientList(this.currentProject);
        }
      },
    },
  },
  methods: {
    async getPatientList(projectId) {
      this.items = [];
      this.loading = true;
      const patientList = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.items.push(...patientList);
      this.loading = false;
    },
    async getPatientRecords({ item }) {
      this.patientRecords = [];
      this.fileLoading = true;
      const records = await fileAPI
        .filterRecords({ userId: item.patientId, projectId: this.currentProject })
        .catch((error) => {
          this.fileLoadingError = error || 'Error when loading file, please try again later';
          return [];
        });
      this.patientRecords = records;
      this.fileLoading = false;
    },
    resetData() {
      this.loading = false;
      this.fileLoading = false;
      this.fileLoadingError = '';
      this.items = [];
      this.patientRecords = [];
    },
    startUploading(record) {
      this.patientRecords.unshift(record);
    },
    addUploadingFile(file) {
      this.patientRecords[0].files.splice(0, 1, file);
    },
  },
};
</script>
