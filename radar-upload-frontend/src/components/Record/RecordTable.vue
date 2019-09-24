<template>
  <v-data-table
    :headers="headers"
    :items="recordList"
    :loading="loading"
    :search="searchText"
    single-expand
    item-key="id"
    :expanded.sync="expandedItems"
    show-expand
    @click:row="expandRow"
  >
    <template #item.uploadedAt="{item}">
      <td
        class="pl-0 pb-0"
      >
        {{ item.modifiedDate | localTime }}
      </td>
    </template>

    <template #item.logs="{item}">
      <td
        class="pl-0 pb-0"
        style="cursor: pointer;"
      >
        <v-dialog
          v-model="dialog"
          max-width="700"
        >
          <template #activator="{ on }">
            <v-icon
              v-if="item.logs"
              v-on="on"
              @click="viewLogs(item.logs.url)"
            >
              mdi-folder-open-outline
            </v-icon>
          </template>
          <v-card>
            <v-card-title v-show="!loadingLog">
              Record ID: {{ item.id }}
            </v-card-title>

            <v-card-text
              color="black"
              v-show="!loadingLog"
            >
              {{ recordLogs }}
            </v-card-text>

            <v-card-text v-show="loadingLog">
              Loading logs...
              <v-progress-circular
                indeterminate
                color="white"
                class="mb-0"
              />
            </v-card-text>
          </v-card>
        </v-dialog>
      </td>
    </template>
    <!-- file list -->
    <template #expanded-item="{item}">
      <td
        :colspan="$vuetify.breakpoint.name==='xs' ? 0 :12"
        class="pa-2"
      >
        <v-list-item
          v-for="(file,fileIndex) in item.files"
          :key="fileIndex"
        >
          <v-list-item-avatar>
            <v-icon>
              mdi-file
            </v-icon>
          </v-list-item-avatar>
          <v-list-item-content>
            <v-list-item-title v-text="file.fileName" />
            <v-list-item-subtitle>Size: {{ file.size }} Bytes</v-list-item-subtitle>
            <v-list-item-subtitle>{{ file.createdDate | localTime }}</v-list-item-subtitle>
          </v-list-item-content>

          <v-list-item-action>
            <v-menu
              open-on-hover
              bottom
              offset-y
            >
              <template #activator="{on}">
                <v-btn
                  icon
                  v-on="on"
                >
                  <v-icon color="primary">
                    mdi-dots-vertical
                  </v-icon>
                </v-btn>
              </template>
              <v-card>
                <v-list
                  shaped
                >
                  <v-list-item-group color="primary lighten-2">
                    <v-list-item @click="downloadFile(item.id, file.fileName)">
                      Download
                    </v-list-item>
                  </v-list-item-group>
                </v-list>
              </v-card>
            </v-menu>
          </v-list-item-action>
        </v-list-item>
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
        { text: 'Participant ID', value: 'userId' },
        { text: 'Last modified', value: 'modifiedDate' },
        { text: 'Logs', value: 'logs' },
      ],
      recordList: [
      ],
      expandedItems: [],
      loadingLog: false,
      recordLogs: '',
      dialog: false,
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
      const recordList = await fileAPI.filterRecords({ projectId })
        .catch(() => []);
      this.loading = false;
      this.recordList = recordList;
    },
    async expandRow(row) {
      if (this.expandedItems[0] && (this.expandedItems[0].id === row.id)) {
        this.expandedItems = [];
      } else {
        this.expandedItems.splice(0, 1, row);
      }
    },
    downloadFile(recordId, fileName) {
      fileAPI.download({ recordId, fileName });
    },
    async viewLogs(url) {
      this.loadingLog = true;
      const recordLogs = await fileAPI.getRecordLog(url).catch(() => {
        this.$error('Cannot download logs, please try again later');
        this.dialog = false;
        return '';
      });
      this.recordLogs = recordLogs;
      this.loadingLog = false;
    },
  },

};
</script>

<style>

</style>
