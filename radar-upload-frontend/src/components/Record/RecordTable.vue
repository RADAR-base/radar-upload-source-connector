<template>
  <v-data-table
    :headers="headers"
    :items="recordList"
    :loading="loading"
    single-expand
    item-key="id"
    disable-sort
    :page.sync="options.page"
    :items-per-page.sync="options.itemsPerPage"
    :expanded.sync="expandedItems"
    @update:options="$nextTick(getRecordList)"
    :server-items-length="serverItemsLength"
    :footer-props="{
      itemsPerPageOptions:[10,20,30],
      showCurrentPage: true
    }"
    show-expand
    @click:row="expandRow"
  >
    <template v-slot:top>
      <!-- filter slot -->
      <v-row
        wrap
        row
        class="mx-2"
      >
        <v-col
          cols="12"
          sm="3"
        >
          <v-select
            v-model="status"
            :items="statusList"
            label="Select a status"
            clearable
          />
        </v-col>
        <v-col
          cols="12"
          sm="3"
        >
          <v-select
            v-model="sourceType"
            :items="sourceTypeList"
            label="Select a source type"
            clearable
          />
        </v-col>
        <v-col
          cols="12"
          sm="3"
        >
          <v-text-field
            label="Enter participant ID"
            v-model="participantId"
            clearable
          />
        </v-col>
        <v-col
          cols="12"
          sm="3"
          class="mb-0 pb-0 pt-4 mt-2"
        >
          <v-btn
            color="primary lighten-1"
            :disabled="loading"
            @click="filterRecordList"
          >
            Search records
          </v-btn>
        </v-col>
      </v-row>
    </template>

    <template #item.modifiedDate="{item}">
      <td
        class="pl-0 pb-0"
      >
        {{ item.modifiedDate | localTime }}
      </td>
    </template>

    <template #item.actions="{item}">
      <!-- edit uploaded files  -->
      <Upload
        button-is-icon="mdi-circle-edit-outline"
        :is-new-upload="false"
        :source-type="item.sourceType"
        :files="item.files"
        :record="item"
        @finishEditRecord="finishEditRecord"
        :is-disabled="item.status!=='INCOMPLETE'"
      />

      <!-- open logs  -->

      <v-dialog
        v-model="dialog"
        max-width="700"
      >
        <template #activator="{ on }">
          <v-icon
            v-on="on"
            @click="viewLogs(item.logs.url)"
            color="success"
            :disabled="!item.logs"
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
            <pre><code>{{ recordLogs }}</code></pre>
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
      <v-icon
        color="error"
        @click="deleteRecord({recordId: item.id, revision: item.revision})"
        :disabled="item.status==='PROCESSING'"
        class="pa-0"
      >
        mdi-close-circle
      </v-icon>
      <v-icon
        color="success"
        @click="retryRecordUpload({recordId: item.id, revision: item. revision})"
        :disabled="item.status==='PROCESSING'"
        class="pa-0"
      >
        mdi-replay
      </v-icon>
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
            <v-list-item-title v-text="file.fileName||file.name" />
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
                    <v-list-item>
                      <a
                        :href="file.url"
                        download
                        target="_blank"
                      >Download</a>
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
import Upload from '@/components/Upload';

export default {
  components: {
    Upload,
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
        { text: 'Actions', value: 'actions' },
      ],
      recordList: [
      ],
      expandedItems: [],
      loadingLog: false,
      recordLogs: '',
      dialog: false,
      options: {
        itemsPerPage: 10,
        page: 1,
      },
      serverItemsLength: 0,
      participantId: '',
      status: '',
      sourceType: '',
      statusList: ['INCOMPLETE', 'READY', 'QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED'],
      sourceTypeList: [],
    };
  },
  computed: {
    currentProject() {
      return this.$store.state.project.currentProject.value;
    },
  },
  methods: {
    filterRecordList() {
      this.options.page = 1;
      this.getRecordList();
    },
    async getRecordList() {
      this.recordList = [];
      this.loading = true;
      const {
        status, sourceType, participantId, currentProject,
      } = this;

      const { page, itemsPerPage } = this.options;
      const { totalElements, tableData } = await fileAPI.filterRecords({
        projectId: currentProject,
        page,
        size: itemsPerPage,
        status,
        sourceType,
        userId: participantId,
      })
        .catch(() => ({ tableData: [], totalElements: 0 }));
      this.loading = false;
      this.serverItemsLength = totalElements;
      this.recordList = tableData;
    },
    async expandRow(row) {
      if (this.expandedItems[0] && (this.expandedItems[0].id === row.id)) {
        this.expandedItems = [];
      } else {
        this.expandedItems.splice(0, 1, row);
      }
    },
    async viewLogs(url) {
      this.loadingLog = true;
      this.recordLogs = await fileAPI.getRecordLog(url).catch(() => {
        this.$error('Cannot download logs, please try again later');
        this.dialog = false;
        return '';
      });
      this.loadingLog = false;
    },
    async getsourceTypeList() {
      const res = await fileAPI.getSourceTypes();
      this.sourceTypeList = res.map((el) => el.name);
    },
    deleteRecord({ recordId, revision }) {
      const recordIndex = this.recordList.findIndex((re) => re.id === recordId);
      fileAPI.deleteRecord({
        recordId,
        revision,
      });
      this.recordList.splice(recordIndex, 1);
    },
    retryRecordUpload({ recordId, revision }) {
      fileAPI.retryRecordUpload({
        recordId,
        revision,
      }).then(() => this.getRecordList());
    },
    finishEditRecord({ record }) {
      const recordIndex = this.recordList.findIndex((re) => re.id === record.id);
      this.recordList.splice(recordIndex, 1, record);
    },
  },
  created() {
    this.getsourceTypeList();
  },
};
</script>

<style>

</style>
