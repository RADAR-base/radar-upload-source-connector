<template>
  <v-data-table
    :headers="headers"
    :items="recordList"
    :loading="loading"
    :search="searchText"
  >
    <template #item.uploadedAt="{item}">
      <td
        class="pl-0 pb-0"
      >
        {{ item.modifiedDate | localTime }}
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
      loading: false,
      headers: [
        { text: 'Record ID', value: 'id' },
        { text: 'Status', value: 'status' },
        { text: 'Source type', value: 'sourceType' },
        { text: 'User', value: 'userId' },
        { text: 'Last modified', value: 'modifiedDate' },
      ],
      recordList: [
      ],
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.value;
    },
    searchText() {
      return this.$store.state.file.searchText;
    },
  },
  watch: {
    currentProject: {
      handler(projectId) {
        if (projectId && this.isActive) {
          this.getRecordList(projectId);
        }
      },
      immediate: true,
    },
    isActive: {
      handler(val) {
        if (val && this.currentProject) {
          this.getRecordList(this.currentProject);
        }
      },
      immediate: true,
    },
  },
  methods: {
    async getRecordList(projectId) {
      this.recordList = [];
      this.loading = true;
      const recordList = await fileAPI.filterRecords({ projectId, getRecordOnly: true })
        .catch(() => []);
      this.loading = false;
      this.recordList = recordList;
    },
  },
};
</script>

<style>

</style>
