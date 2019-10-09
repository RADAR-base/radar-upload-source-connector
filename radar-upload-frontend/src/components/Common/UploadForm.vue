<template>
  <v-card
    flat
    :loading="loading"
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
      <v-list-item
        v-if="!uploadInfo.userId"
      >
        <v-autocomplete
          class="pt-2"
          :disabled="!!activeRecord"
          label="Select a patient"
          :items="patientList"
          v-model="userId"
        />
      </v-list-item>

      <v-list-item>
        <v-select
          class="pt-0"
          :disabled="!!activeRecord"
          label="Select source type"
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
      <template #item.name="{value}">
        {{ value | textTruncate }}
      </template>
      <template #item.size="{value}">
        {{ value | toMB }}
      </template>
      <template #item.status="{ item}">
        <span v-if="!item.active&&!item.success&&!item.error">Waiting for upload</span>
        <span v-if="item.active">
          <v-progress-linear :value="item.progress" />
        </span>
        <span v-if="item.success&&!item.active">
          Uploaded
          <v-icon color="success">mdi-checkbox-marked-circle</v-icon>
        </span>
        <span
          v-if="item.error&&!item.active"
        >
          Upload failed
          <v-icon
            color="error"
            @click="removeErrorFile(item.id)"
          >
            mdi-close-circle
          </v-icon>
        </span>
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
        text
        @click="startUpload"
        v-show="!!activeRecord&&!allFilesUploaded"
        :disabled="!files.length||uploading"
      >
        <span v-show="!allFilesUploaded">Upload</span>
        <span v-show="allFilesUploaded">Done</span>
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
    uploadInfo: {
      type: Object,
      required: true,
    },
    patientList: {
      type: Array,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      files: [],
      sourceType: '',
      sourceTypeList: [],
      userId: '',
      activeRecord: null,
      isLoading: false,
      headers: [
        { text: 'File name', value: 'name' },
        { text: 'File size', value: 'size' },
        { text: 'Status', value: 'status' },
      ],
    };
  },
  computed: {
    totalFileSize() {
      return this.files.length === 0 ? 0
        : this.files.map(file => file.size)
          .reduce((preVal, currentVal) => Number(preVal) + Number(currentVal));
    },
    allFilesUploaded() {
      return this.files.length && this.files.findIndex(file => !file.success) === -1;
    },
    uploading() {
      return this.files.length && this.files.findIndex(file => file.active) > 0;
    },
  },
  methods: {
    async getsourceTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.sourceTypeList = res.map(el => el.name);
    },
    closeDialog() {
      this.$emit('cancelClick');
      this.removeData();
      // todo: delete record
    },
    removeData() {
      this.files.splice(0);
      this.sourceType = '';
      this.activeRecord = null;
      this.userId = '';
      this.sourceType = '';
      // this.sourceTypeList.splice(0);
    },
    removeErrorFile(fileId) {
      const fileIndex = this.files.findIndex(file => file.id === fileId);
      this.files.splice(fileIndex, 1);
    },
    async createRecord() {
      const { projectId } = this.uploadInfo;
      const userId = this.uploadInfo.userId || this.userId;
      const { sourceType } = this;
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
      const fileIndex = this.files.findIndex(file => file.id === fileObject.id);
      const self = this;
      const onUploadProgress = (progressEvent) => {
        const percentCompleted = Math.floor((progressEvent.loaded * 100) / progressEvent.total);
        self.files[fileIndex].progress = percentCompleted;
      };

      const uploadPayload = {
        file: fileObject.file,
        fileType: fileObject.type,
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
        .catch((e) => {
          console.log('Error when mark record', e);
          this.$error('Error when submitting data, please try again');
        });
      this.isLoading = false;
      if (!res) return;
      this.closeDialog();
    },
  },
  created() {
    this.getsourceTypeList();
  },
};
</script>
