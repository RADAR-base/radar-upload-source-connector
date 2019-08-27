<template>
  <v-card
    flat
    :loading="loading"
  >
    <v-list>
      <v-list-item v-if="!uploadInfo.userId">
        <v-autocomplete
          label="Select a patient"
          :items="patientList"
          v-model="userId"
        />
      </v-list-item>

      <v-list-item>
        <v-select
          label="Select source type"
          :items="sourceTypeList"
          v-model="sourceType"
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
          @change="selectFile"
        />
      </v-list-item>
    </v-list>
    <v-card-actions>
      <v-spacer />

      <v-btn
        text
        @click.native="$emit('cancelClick')"
      >
        Cancel
      </v-btn>

      <v-btn
        color="primary"
        text
        @click.native="uploadFile"
        :disabled="!sourceType||file.length===0"
      >
        Upload
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import fileAPI from '@/axios/file.js';

export default {
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
      file: [],
      sourceType: '',
      sourceTypeList: [],
      userId: '',
    };
  },
  methods: {
    selectFile(file) {
      console.log(file);
    },
    async getsourceTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.sourceTypeList = res.map(el => el.name);
      this.contentTypes = res.map(el => el.contentTypes);
    },
    removeData() {
      this.file = [];
      this.sourceType = '';
      this.sourceTypeList = [];
    },
    async uploadFile() {
      const { projectId } = this.uploadInfo;
      const userId = this.uploadInfo.userId || this.userId;
      const { sourceType } = this;
      const postPayload = { userId, projectId, sourceType };
      const files = [];
      files.push({ fileName: this.file.name, uploading: true });
      try {
        this.$emit('creatingRecord');
        const returnedRecord = await fileAPI.postRecords(postPayload);
        this.$emit('startUploading', { ...returnedRecord, files, active: true });
        const putPayload = { file: this.file, fileName: this.file.name, id: returnedRecord.id };
        const uploadingFile = await fileAPI.putRecords(putPayload);
        this.$emit('finishUpload', uploadingFile);
      } catch (error) {
        this.$emit('uploadFailed');
        this.$error('Upload fails, please try again later');
      }
      this.removeData();
    },
  },
  created() {
    this.getsourceTypeList();
  },
};
</script>

<style>

</style>
