<template>
  <v-data-table
    :items="items"
    single-expand
    :search="searchText"
    :expanded.sync="expandedItems"
    show-expand
    :headers="headers"
    item-key="patientId"
    :loading="loading"
    loading-text="Retrieving participants info, please wait"
    @click:row="expandRow"
  >
    <template #item.updatedAt="{item}">
      <td class="pl-0">
        {{ item.updatedAt | localTime }}
      </td>
    </template>

    <template #expanded-item="{item}">
      <td
        :colspan="$vuetify.breakpoint.name==='xs' ? 0 :12"
        class="pa-2"
      >
        <Records
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
              @uploadFailed="uploadFailed"
            />
          </template>
        </Records>
      </td>
    </template>
  </v-data-table>
</template>

<script>
import { mapState } from 'vuex';
import UploadButton from './UploadButton';
import Records from './Records';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file';

export default {
  components: {
    UploadButton,
    Records,
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
      expandedItems: [],
      items: [
      ],
      patientRecords: [
      ],
      headers: [
        { text: 'Paticipant ID', value: 'patientId' },
        { text: 'External identifier', value: 'patientName' },
        { text: 'Status', value: 'status' },
      ],
    };
  },
  computed: {
    ...mapState('project', {
      currentProject: state => state.currentProject.value,
    }),
    ...mapState('patient', {
      searchText: state => state.searchText,
    }),
  },
  watch: {
    currentProject: {
      handler(projectId) {
        if (projectId && this.isActive) {
          this.items = [];
          this.expandedItems = [];
          this.getPatientList(projectId);
        }
      },
      immediate: true,
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
      this.items = patientList;
      this.loading = false;
    },
    async getPatientRecords({ item }) {
      this.patientRecords = [];
      this.fileLoading = true;
      const records = await fileAPI
        .filterRecords({ userId: item.patientId, projectId: this.currentProject })
        .catch((error) => {
          this.fileLoadingError = error;
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
    uploadFailed({
      recordId, fileName, uploading, uploadFailed,
    }) {
      const failedRecordIndex = this.patientRecords.findIndex(record => record.id === recordId);
      this.patientRecords[failedRecordIndex].files
        .splice(0, 1, { fileName, uploading, uploadFailed });
    },
    async expandRow(row) {
      if (this.expandedItems[0] && (this.expandedItems[0].patientId === row.patientId)) {
        this.expandedItems = [];
      } else {
        await this.getPatientRecords({ item: row });
        this.expandedItems.splice(0, 1, row);
      }
    },
  },
};
</script>
