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

      <UploadForm
        @finishUpload="finishUpload"
        @startUploading="startUploading"
        @uploadFailed="uploadFailed"
        @cancelClick="closeMenu"
        @creatingRecord="creatingRecord"
        :upload-info="uploadInfo"
        :loading="loading"
      />
    </v-menu>
  </div>
</template>

<script>
import UploadForm from '@/components/Common/UploadForm.vue';

export default {
  components: {
    UploadForm,
  },
  props: {
    uploadInfo: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      menu: false,
      loading: false,
    };
  },
  methods: {
    finishUpload(file) {
      this.loading = false;
      this.$emit('addUploadingFile', file);
      this.menu = false;
    },
    startUploading(payload) {
      this.$emit('startUploading', payload);
    },
    closeMenu() {
      this.menu = false;
      this.loading = false;
    },
    creatingRecord() {
      this.menu = false;
    },
    uploadFailed(record) {
      this.closeMenu();
      this.$emit('uploadFailed', record);
    },
  },
};
</script>

<style>

</style>
