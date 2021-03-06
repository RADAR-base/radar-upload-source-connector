<template>
  <v-data-table
    :items="items"
    single-expand
    :search="searchText"
    :expanded.sync="expandedItems"
    show-expand
    :headers="headers"
    item-key="patientId"
    :loading="loading"
    loading-text="Retrieving participants info, please wait"
    @click:row="expandRow"
    @item-expanded="({item}) =>expandRow(item)"
    :footer-props="{
      itemsPerPageOptions:[10,20,30],
      showCurrentPage: true
    }"
  >
    <template #top>
      <v-row
        class="mx-2 py-0"
        align="end"
      >
        <v-col
          cols="12"
          sm="3"
          order="last"
        >
          <v-text-field
            label="Enter a participant to search"
            v-model="searchText"
          />
        </v-col>
      </v-row>
    </template>
    <template #item.updatedAt="{item}">
      <td class="pl-0">
        {{ item.updatedAt | localTime }}
      </td>
    </template>

    <template #expanded-item="{}">
      <td
        :colspan="$vuetify.breakpoint.name==='xs' ? 0 :12"
        class="pa-2"
      >
        <Records
          :patient-records="patientRecords"
          :loading="fileLoading"
          :error="fileLoadingError"
        />
      </td>
    </template>
  </v-data-table>
</template>

<script>
import { mapState } from 'vuex';
import patientAPI from '@/axios/patient';
import fileAPI from '@/axios/file';
import Records from './Records';

export default {
  components: {
    Records,
  },
  data() {
    return {
      loading: false,
      fileLoading: false,
      fileLoadingError: '',
      expandedItems: [],
      items: [
      ],
      patientRecords: [
      ],
      headers: [
        { text: 'Paticipant ID', value: 'patientId' },
        { text: 'External identifier', value: 'patientName' },
        { text: 'Status', value: 'status' },
      ],
      searchText: '',
    };
  },
  computed: {
    ...mapState('project', {
      currentProject: (state) => state.currentProject.value,
    }),
  },
  watch: {
    currentProject: {
      handler(projectId) {
        if (projectId) {
          this.items = [];
          this.expandedItems = [];
          this.getPatientList(projectId);
        }
      },
      immediate: true,
    },
  },
  methods: {
    async getPatientList(projectId) {
      this.items = [];
      this.loading = true;
      this.items = await patientAPI.filteredPatients(projectId).catch(() => ([]));
      this.loading = false;
    },
    async getPatientRecords({ item }) {
      this.patientRecords = [];
      this.fileLoading = true;
      const { tableData } = await fileAPI
        .filterRecords({
          userId: item.patientId, projectId: this.currentProject, page: 1, size: 100,
        })
        .catch((error) => {
          this.fileLoadingError = error;
          return { tableData: [] };
        });
      this.patientRecords = tableData;
      this.fileLoading = false;
    },
    resetData() {
      this.loading = false;
      this.fileLoading = false;
      this.fileLoadingError = '';
      this.items = [];
      this.patientRecords = [];
    },
    async expandRow(row) {
      if (this.expandedItems[0] && (this.expandedItems[0].patientId === row.patientId)) {
        this.expandedItems = [];
      } else {
        this.getPatientRecords({ item: row });
        this.expandedItems.splice(0, 1, row);
      }
    },
  },
};
</script>
