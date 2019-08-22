<template>
  <div>
    <v-menu
      :value="menu"
      attach
      :close-on-click="false"
      :close-on-content-click="false"
      v-model="menu"
    >
      <template #activator="{on}">
        <v-btn
          color="primary lighten-1"
          class="white--text ml-0"
          v-on="on"
          flat
          depressed
        >
          <v-icon class="pr-2">
            mdi-cloud-upload-outline
          </v-icon>
          Click to upload
        </v-btn>
      </template>

      <v-card>
        <v-list>
          <!-- <v-subheader>
            {{ commonInfo.patientName }}
            -
            {{ commonInfo.projectName }}
          </v-subheader> -->
          <v-list-item>
            <v-select
              label="Select source type"
              :items="fileTypeList"
              v-model="fileType"
            />
          </v-list-item>
          <v-list-item>
            <v-file-input
              label="Select a file"
              clear-icon
              clearable
              append-icon="mdi-paperclip"
              :prepend-icon="''"
              v-model="file"
            />
          </v-list-item>
        </v-list>
        <v-card-actions>
          <v-spacer />

          <v-btn
            text
            @click.native="cancelUpload"
          >
            Cancel
          </v-btn>

          <v-btn
            color="primary"
            text
            @click.native="uploadFile"
            :disabled="!fileType||file.length===0"
          >
            Upload
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-menu>
  </div>
</template>

<script>
import fileAPI from '@/axios/file.js';

export default {
  props: {
    uploadInfo: {
      type: Object,
      default: () => ({
        data: {
          projectId: 'radar-test',
          userId: 'testUser',
          // sourceId: 'source',
        },
        // sourceType: 'Mp3Audio',
      }),
      required: true,
    },
  },
  data() {
    return {
      menu: false,
      file: [],
      fileType: '',
      fileTypeList: [],
    };
  },
  methods: {
    async getFileTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.fileTypeList = res.map(el => el.name);
      this.contentTypes = res.map(el => el.contentTypes);
    },
    cancelUpload() {
      this.menu = false;
      this.file = [];
      this.fileType = '';
      this.fileTypeList = [];
    },
    async uploadFile() {
      const { userId, projectId } = this.uploadInfo;
      const { fileType } = this;
      const postPayload = { userId, projectId, sourceType: fileType };
      const files = [];
      files.push({ fileName: this.file.name, uploading: true });
      try {
        const returnedRecord = await fileAPI.postRecords(postPayload);
        this.$emit('startUploading', { ...returnedRecord, files, active: true });
        this.menu = false;
        const putPayload = { file: this.file, fileName: this.file.name, id: returnedRecord.id };
        const uploadingFile = await fileAPI.putRecords(putPayload);
        this.$emit('addUploadingFile', uploadingFile);
      } catch (error) {
        this.menu = false;
        this.$error('Upload fails, please try again later');
      }
    },
  },
  created() {
    this.getFileTypeList();
  },
};
</script>

<style>

</style>
