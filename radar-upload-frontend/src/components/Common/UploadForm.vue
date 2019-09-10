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
        :disabled="!sourceType||file.length===0||(!uploadInfo.userId&&!userId)"
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
    async getsourceTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.sourceTypeList = res.map(el => el.name);
    },
    removeData() {
      this.file = [];
      this.sourceType = '';
    },
    async uploadFile() {
      const { projectId } = this.uploadInfo;
      const userId = this.uploadInfo.userId || this.userId;
      const { sourceType } = this;
      const postPayload = { userId, projectId, sourceType };
      const files = [];
      files.push({ fileName: this.file.name, uploading: true, uploadFailed: false });
      try {
        this.$emit('creatingRecord');
        const returnedRecord = await fileAPI.postRecords(postPayload);
        this.$emit('startUploading', { ...returnedRecord, files, active: true });
        const putPayload = { file: this.file, fileName: this.file.name, id: returnedRecord.id };
        const uploadingFile = await fileAPI.putRecords(putPayload)
          .catch(() => {
            this.$emit('uploadFailed', {
              fileName: this.file.name,
              uploading: false,
              uploadFailed: true,
              recordId: returnedRecord.id,
            });
            throw new Error();
          });
        this.$emit('finishUpload', uploadingFile);
        await fileAPI.markRecord({
          recordId: returnedRecord.id,
          revision: returnedRecord.revision,
        });
      } catch (error) {
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
