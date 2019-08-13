<template>
  <v-data-table
    :headers="headers"
    :items="items"
    :loading="loading"
  >
    <template #item.uploadedAt="{item}">
      <td
        class="pl-0 pb-0"
      >
        {{ item.uploadedAt | moment("YYYY/MM/DD") }}
      </td>
    </template>
  </v-data-table>
</template>

<script>
import fileAPI from '@/axios/file';

export default {
  props: {
    isActive: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      currentProject: this.$store.state.project.value,
      loading: false,
      headers: [
        { text: '#', value: 'sequence' },
        { text: 'File name', value: 'fileName' },
        { text: 'File type', value: 'fileType' },
        { text: 'Patient', value: 'patient' },
        { text: 'Uploaded at', value: 'uploadedAt' },
        { text: 'Status', value: 'status' },
        { text: 'Size', value: 'size' },
      ],
      items: [
        {
          sequence: 1,
          fileName: 'Audio1',
          fileType: 'mp3',
          status: 'Incomplete',
          uploadedAt: Date.now(),
          patient: 'Patient 1',
          size: '96 Kb',
        },
        {
          sequence: 2,
          fileName: 'Checklist',
          fileType: 'text',
          status: 'Completed',
          patient: 'Patient 2',
          uploadedAt: Date.now(),
          size: '100 Kb',
        },
      ],
    };
  },
  watch: {
    currentProject: {
      handler(val) {
        if (!val) {
          this.items = [];
        } else if (this.isActive) {
          this.getFileList(val);
        }
      },
    },
  },
  methods: {
    async getFileList(projectId) {
      this.items = [];
      this.loading = true;
      const fileList = await fileAPI.filterRecords({ projectId }).catch(() => []);
      this.loading = false;
      this.items = fileList;
    },
  },
};
</script>

<style>

</style>
