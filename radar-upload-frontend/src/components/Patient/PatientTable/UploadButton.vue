<template>
  <div>
    <v-menu
      :value="menu"
      attach
      max-width="300px"
      min-width="300px"
      :close-on-click="false"
      :close-on-content-click="false"
      v-model="menu"
    >
      <template #activator="{on}">
        <v-btn
          color="primary lighten-1"
          class="white--text ml-0"
          v-on="on"
          text
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
            @click.native="menu=false"
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
    </v-menu>
  </div>
</template>

<script>
import fileAPI from '@/axios/file.js';

export default {
  props: {
    uploadInfo: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      menu: false,
      file: [],
      sourceType: '',
      sourceTypeList: [],
    };
  },
  watch: {
    menu: {
      handler(open) {
        if (!open) {
          this.removeData();
        }
      },
    },
  },
  methods: {
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
      const { userId, projectId } = this.uploadInfo;
      const { sourceType } = this;
      const postPayload = { userId, projectId, sourceType };
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
    this.getsourceTypeList();
  },
};
</script>

<style>

</style>
