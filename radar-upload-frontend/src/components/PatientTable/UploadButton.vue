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
          color="primary lighten-2"
          class="white--text ml-0"
          fab
          x-small
          v-on="on"
        >
          <v-icon dark>
            mdi-cloud-upload-outline
          </v-icon>
        </v-btn>
      </template>

      <v-card>
        <v-list>
          <!-- <v-subheader>{{ commonInfo.patientName }} - {{ commonInfo.projectName }}</v-subheader> -->
          <v-list-item>
            <v-select
              label="Select file type"
              :items="fileTypeList"
              v-model="fileType"
            />
          </v-list-item>
          <v-list-item>
            <v-file-input
              label="Select a file"
              clear-icon
              clearable
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
    commonInfo: {
      type: Object,
      required: true,
      default: () => ({
        projectName: 'project name',
        patientName: 'patient name',
      }),
    },
  },
  data() {
    return {
      menu: false,
      file: [],
      fileType: '',
      fileTypeList: ['file 1'],
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
      const { userId, projectId, fileType } = this;
      const postPayload = { userId, projectId, sourceType: fileType };
      const postRecordReturned = await fileAPI.postRecords(postPayload);

      const putPayload = { file: this.file, fileName: this.file.name, id: postRecordReturned.id };
      await fileAPI.putRecords(putPayload);

      this.menu = false;
    },
  },
  created() {
    this.getFileTypeList();
  },
};
</script>

<style>

</style>
