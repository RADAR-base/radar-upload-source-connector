<template>
  <v-card
    flat
  >
    <v-toolbar
      color="primary"
      dark
    >
      <v-toolbar-title>Upload Form</v-toolbar-title>
    </v-toolbar>
    <!-- create record -->
    <v-list>
      <v-subheader v-show="!activeRecord">
        Create a record
      </v-subheader>
      <v-list-item>
        <v-autocomplete
          class="pt-2"
          :disabled="!!activeRecord||!!oldUserId"
          label="Participant"
          :items="patientList"
          v-model="userId"
        />
      </v-list-item>

      <v-list-item>
        <v-select
          class="pt-0"
          :disabled="!!activeRecord||!!oldSourceType"
          label="Source type"
          :items="sourceTypeList"
          v-model="sourceType"
        />
      </v-list-item>

      <v-list-item
        v-show="!!activeRecord"
      >
        <v-text-field
          class="pt-0"
          label="Created record"
          :value="activeRecord ? `ID: ${activeRecord.id} (status: ${activeRecord.status})`: ''"
          disabled
        />
      </v-list-item>
      <v-list-item v-show="!activeRecord">
        <v-btn
          color="primary"
          @click="createRecord"
          :loading="isLoading"
          :disabled="!sourceType||!userId"
        >
          Create record
        </v-btn>
      </v-list-item>
    </v-list>

    <!-- Upload File -->
    <v-list v-show="activeRecord">
      <v-list-item-content
        style="'align-items':center"
        class="px-4 py-0"
      >
        <VueUploadComponent
          v-model="files"
          :drop="true"
          class="pa-4 py-6 vue-upload-component"
          style="border:1px grey dashed"
          multiple
          @input-filter="filterUploadingFiles"
        >
          <v-icon
            color="primary"
            large
          >
            mdi-cloud-upload-outline
          </v-icon>
          <div class="body-2">
            Drag and drop or browse files to upload
          </div>
        </VueUploadComponent>
      </v-list-item-content>
      <v-list-item-content
        class="px-4 py-0 my-0 caption font-italic font-weight-light"
      >
        Total file(s): {{ files.length }}
      </v-list-item-content>
      <v-list-item-content
        class="px-4 py-0 my-0 caption font-italic font-weight-light"
      >
        Total size(s): {{ totalFileSize | toMB }}
      </v-list-item-content>
    </v-list>
    <!-- List of files -->
    <v-data-table
      v-show="files.length&&!!activeRecord"
      :items="files"
      :headers="headers"
      hide-default-footer
      disable-sort
    >
      <template
        #item.name="{item, value}"
      >
        <span :style="{color:item.error?'red':''}">
          {{ value | textTruncate }}
        </span>
      </template>
      <template #item.size="{value, item}">
        <span :style="{color:item.error?'red':''}">
          {{ value | toMB }}
        </span>
      </template>
      <template #item.status="{ item}">
        <span v-if="!item.active&&!item.success&&!item.error">Waiting for upload</span>
        <span v-if="item.active">
          <v-progress-linear :value="item.progress" />
        </span>
        <span v-if="item.success&&!item.active">
          Uploaded
          <v-icon
            color="success"
            small
          >mdi-checkbox-marked-circle</v-icon>
        </span>
        <span
          v-if="item.error&&!item.active"
          :style="{color:'red'}"
        >
          Failed
        </span>
      </template>

      <template #item.action="{item}">
        <v-icon
          small
          :color="item.error?'red':''"
          @click="removeFile(item)"
        >
          mdi-close-circle
        </v-icon>
      </template>
    </v-data-table>

    <v-card-actions>
      <v-spacer />

      <v-btn
        text
        @click.native="closeDialog"
      >
        Cancel
      </v-btn>

      <v-btn
        color="primary"
        @click="startUpload"
        v-show="!!activeRecord&&!allFilesUploaded"
        :disabled="!files.length||hasUploadingFile"
      >
        Upload
      </v-btn>

      <v-btn
        color="success"
        @click="finishUpload"
        v-show="!!activeRecord&&allFilesUploaded"
        :loading="isLoading"
      >
        Done
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import VueUploadComponent from 'vue-upload-component';
import fileAPI from '@/axios/file.js';

export default {
  components: {
    VueUploadComponent,
  },
  props: {
    patientList: {
      type: Array,
      default: () => [],
    },
    sourceTypeList: {
      type: Array,
      default: () => [],
    },
    isNewUpload: {
      type: Boolean,
      default: true,
    },
    oldFiles: {
      type: Array,
      default: () => [],
    },
    oldRecord: {
      type: Object,
      default: null,
    },
    oldUserId: {
      type: String,
      default: '',
    },
    oldSourceType: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      files: this.oldFiles.map(file => ({
        ...file, success: true, error: '', name: file.fileName,
      })),
      sourceType: this.oldSourceType,
      userId: this.oldUserId,
      activeRecord: this.oldRecord,
      isLoading: false,
      headers: [
        { text: 'File name', value: 'name' },
        { text: 'File size', value: 'size' },
        { text: 'Status', value: 'status' },
        { text: 'Action', value: 'action' },
      ],
    };
  },
  computed: {
    projectId() {
      return this.$store.state.project.currentProject.value;
    },
    totalFileSize() {
      return this.files.length === 0 ? 0
        : this.files.map(file => file.size)
          .reduce((preVal, currentVal) => Number(preVal) + Number(currentVal));
    },
    allFilesUploaded() {
      return this.files.length && this.files.findIndex(file => !file.success) === -1;
    },
    hasUploadingFile() {
      return this.files.length && this.files.findIndex(file => file.active) > 0;
    },
  },
  methods: {
    closeDialog() {
      // delete record if it is new record
      if (this.activeRecord && this.isNewUpload && !this.allFilesUploaded) {
        fileAPI.deleteRecord({
          recordId: this.activeRecord.id,
          revision: this.activeRecord.revision,
        });
      }
      this.removeData();
      this.$emit('cancelClick');
    },
    removeData() {
      this.files.splice(0);
      this.sourceType = '';
      this.activeRecord = null;
      this.userId = '';
      this.sourceType = '';
    },
    filterUploadingFiles(newFile, oldFile, prevent) {
      if (newFile) {
        const newFileIndex = this.files.findIndex(file => file.name === newFile.name);
        if (newFileIndex > -1) {
          this.$error(`File ${newFile.name} is duplicated`);
          prevent();
        }
      }
    },
    async removeFile(removedFile) {
      const fileIndex = this.files.findIndex(file => file.name === removedFile.name);
      if (!removedFile.success) { // file not yet uploaded
        this.files.splice(fileIndex, 1);
      } else if (this.activeRecord.status === 'INCOMPLETE') {
        fileAPI
          .deleteFile({ recordId: this.activeRecord.id, fileName: removedFile.name })
          .then(() => {
            this.files.splice(fileIndex, 1);
          })
          .catch(() => { this.$error('Cannot delete this file, please try again later'); });
      }
    },
    async createRecord() {
      const { sourceType, projectId, userId } = this;
      const postPayload = { userId, projectId, sourceType };
      this.isLoading = true;
      const createdRecord = await fileAPI.postRecords(postPayload)
        .catch(() => {
          this.$error('Cannot create record, please try again');
        });
      if (createdRecord) this.activeRecord = createdRecord;
      this.isLoading = false;
    },
    startUpload() {
      // filter out error and uploaded files
      this.files
        .filter(file => !file.success && !file.error)
        .forEach((file) => {
          this.processUpload(file);
        });
    },
    async processUpload(fileObject) {
      // only newly pushed files have id
      const fileIndex = this.files.findIndex(file => file.id === fileObject.id);
      const self = this;
      const onUploadProgress = (progressEvent) => {
        const percentCompleted = Math.floor((progressEvent.loaded * 100) / progressEvent.total);
        self.files[fileIndex].progress = percentCompleted;
      };
      const fileType = fileObject.type;
      const fileContentType = (fileType == null || fileType.length === 0) ? 'application/octet-stream' : fileObject.type;
      const uploadPayload = {
        file: fileObject.file,
        fileType: fileContentType,
        fileName: fileObject.name,
        id: this.activeRecord.id,
        onUploadProgress,
      };
      // use active set loading state
      this.files[fileIndex].active = true;
      const uploadedFile = await fileAPI.putRecords(uploadPayload)
        .catch(() => {
          this.files[fileIndex].error = true;
        });
      if (uploadedFile) {
        // upload success
        this.files[fileIndex].success = true;
      }
      this.files[fileIndex].active = false;
    },
    async finishUpload() {
      this.isLoading = true;
      const res = await fileAPI.markRecord({
        recordId: this.activeRecord.id,
        revision: this.activeRecord.revision,
      })
        .catch(() => {
          this.$error('Error when submitting data, please try again');
        });
      this.isLoading = false;
      if (!res) return;
      if (!this.isNewUpload) {
        this.$emit('finishEditRecord', {
          record: { ...this.activeRecord, status: 'READY', files: this.files.slice() },
        });
      }
      this.closeDialog();
    },
  },
};
</script>
