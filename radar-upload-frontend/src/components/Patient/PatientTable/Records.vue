<template>
  <v-list
    shaped
    three-line
    subheader
  >
    <v-layout justify-center>
      <v-progress-circular
        class="mt-2"
        v-show="loading"
        indeterminate
        color="primary"
      />
      <v-alert
        type="error"
        v-text="error"
        v-show="error"
        dense
        text
      />
    </v-layout>
    <v-subheader>
      <span v-show="!loading">
        <slot name="fileListSubHeader" />
      </span>
      <span v-show="!loading&&patientRecords.length==0">
        This patient does not have any records
      </span>
    </v-subheader>
    <v-list-group
      v-for="record in records"
      :key="record.id"
      v-model="record.active"
      no-action
    >
      <template #activator>
        <v-list-item-content>
          <v-list-item-title>
            Records ID:
            {{ record.id }} ({{ record.sourceType }})
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ record.message }}
          </v-list-item-subtitle>
          <v-list-item-subtitle>
            {{ record.modifiedDate | localTime }}
          </v-list-item-subtitle>
        </v-list-item-content>
      </template>
      <!-- view logs dialog -->

      <v-dialog
        v-model="dialog"
        max-width="700"
      >
        <template #activator="{ on }">
          <v-list-item-title
            v-show="record.logs"
            class="pl-10 ml-10"
            style="cursor: pointer;"
            @click="viewLogs(record.logs&&record.logs.url)"
            v-on="on"
          >
            <v-icon color="primary">
              mdi-logout-variant
            </v-icon>
            View logs
          </v-list-item-title>
        </template>
        <v-card>
          <v-card-title v-show="!loadingLog">
            Record ID: {{ record.id }}
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

      <!-- End of view logs dialog -->

      <v-list-item
        v-for="(file,fileIndex) in record.files"
        :key="fileIndex"
      >
        <v-list-item-avatar>
          <v-icon v-show="!file.uploading&&!file.uploadFailed">
            mdi-file
          </v-icon>

          <v-icon
            v-show="file.uploadFailed"
            color="error"
          >
            mdi-close-octagon
          </v-icon>

          <v-progress-circular
            class="mt-2"
            v-show="!file.uploadFailed&&file.uploading"
            indeterminate
            color="primary"
          />
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
                  <v-list-item @click="downloadFile(record.id, file.fileName)">
                    Download
                  </v-list-item>
                </v-list-item-group>
              </v-list>
            </v-card>
          </v-menu>
        </v-list-item-action>
      </v-list-item>
    </v-list-group>
  </v-list>
</template>


<script>
import fileAPI from '@/axios/file';

export default {
  props: {
    patientRecords: {
      type: Array,
      required: true,
    },
    error: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      recordLogs: '',
      dialog: false,
      loadingLog: false,
    };
  },
  computed: {
    records() {
      return this.patientRecords.map(record => ({ active: false, ...record }));
    },
  },
  methods: {
    async downloadFile(recordId, fileName) {
      await fileAPI.download({ recordId, fileName });
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
