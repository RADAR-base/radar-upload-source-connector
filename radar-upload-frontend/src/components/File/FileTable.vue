<template>
  <v-data-table
    :headers="headers"
    :items="fileList"
    :loading="loading"
    :search="searchText"
  >
    <template #item.uploadedAt="{item}">
      <td
        class="pl-0 pb-0"
      >
        {{ item.uploadedAt | localTime }}
      </td>
    </template>

    <template #top="{items}">
      <v-layout align-content-start>
        <v-flex
          xs12
          md4
          offset-md-8
        >
          <FileFilter
            :files="items"
            @filterFiles="filterFiles"
          />
        </v-flex>
      </v-layout>
    </template>
  </v-data-table>
</template>

<script>
import fileAPI from '@/axios/file';
import FileFilter from './FileFilter';

export default {
  components: {
    FileFilter,
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
      searchText: '',
      headers: [
        { text: 'File name', value: 'fileName' },
        { text: 'File type', value: 'fileType' },
        { text: 'Patient', value: 'patient' },
        { text: 'Uploaded at', value: 'uploadedAt' },
        { text: 'Size', value: 'fileSize' },
      ],
      fileList: [
        // {
        //   sequence: 1,
        //   fileName: 'Audio1',
        //   fileType: 'mp3',
        //   status: 'Incomplete',
        //   uploadedAt: Date.now(),
        //   patient: 'Patient 1',
        //   size: '96 Kb',
        // },
        // {
        //   sequence: 2,
        //   fileName: 'Checklist',
        //   fileType: 'text',
        //   status: 'Completed',
        //   patient: 'Patient 2',
        //   uploadedAt: Date.now(),
        //   size: '100 Kb',
        // },
      ],
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.value;
    },
  },
  watch: {
    currentProject: {
      handler(projectId) {
        if (projectId && this.isActive) {
          this.getFileList(projectId);
        }
      },
      immediate: true,
    },
    isActive: {
      handler(val) {
        if (val && this.currentProject) {
          this.getFileList(this.currentProject);
        }
      },
      immediate: true,
    },
  },
  methods: {
    async getFileList(projectId) {
      this.fileList = [];
      this.loading = true;
      const fileList = await fileAPI.filterRecords({ projectId, getFileOnly: true })
        .catch(() => []);
      this.loading = false;
      this.fileList = fileList;
    },
    filterFiles(searchText) {
      this.searchText = searchText;
    },
  },
};
</script>

<style>

</style>
