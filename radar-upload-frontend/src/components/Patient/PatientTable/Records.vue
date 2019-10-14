<template>
  <v-list
    subheader
  >
    <!-- loader -->
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

    <!-- no data  -->
    <v-subheader v-show="!loading && patientRecords.length === 0 && !error">
      <span>
        This patient does not have any records
      </span>
    </v-subheader>

    <!-- record list  -->

    <v-list-group
      v-for="(record, recordIndex) in records"
      :key="record.id"
      v-model="record.active"
      no-action
      three-line
    >
      <template #activator>
        <v-list-item-content>
          <v-list-item-title>
            Records ID:
            {{ record.id }} ({{ record.sourceType }})
          </v-list-item-title>
          <v-list-item-subtitle>
            {{ record.status }} - {{ record.message }}
          </v-list-item-subtitle>
          <v-list-item-subtitle>
            {{ record.modifiedDate | localTime }}
          </v-list-item-subtitle>
        </v-list-item-content>
      </template>

      <!-- actions  -->
      <v-subheader class="ml-2">
        Actions
      </v-subheader>

      <v-list-item>
        <v-list-item-content>
          <v-list-item-title class="py-0">
            <!-- edit files uploaded  -->
            <Upload
              :is-new-upload="false"
              :user-id="record.userId"
              :source-type="record.sourceType"
              :files="record.files"
              :record="record"
              @finishEditRecord="finishEditRecord"
            />

            <v-dialog
              v-model="dialog"
              max-width="700"
            >
              <template #activator="{ on }">
                <v-btn
                  @click="viewLogs(record.logs&&record.logs.url)"
                  v-on="on"
                  :disabled="!record.logs"
                  class="mr-2"
                  color="info"
                >
                  View logs
                </v-btn>
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

            <v-btn
              color="error"
              :disabled="record.status==='PROCESSING'"
              @click="deleteRecord({recordId:record.id, revision:record.revision, recordIndex})"
            >
              Delete record
            </v-btn>
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>

      <v-divider />
      <!-- end of actions  -->

      <!-- file list  -->
      <v-subheader class="ml-2">
        Files
      </v-subheader>

      <v-list-item
        v-for="(file,fileIndex) in record.files"
        :key="fileIndex"
        three-line
      >
        <v-list-item-content>
          <v-list-item-title v-text="file.fileName||file.name" />
          <v-list-item-subtitle>Size: {{ file.size | toMB }} </v-list-item-subtitle>
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
    </v-list-group>
  </v-list>
</template>


<script>
import fileAPI from '@/axios/file';
import Upload from '@/components/Upload';

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
  components: {
    Upload,
  },
  data() {
    return {
      recordLogs: '',
      dialog: false,
      loadingLog: false,
      records: this.patientRecords.map(record => ({ active: false, ...record })),
    };
  },
  methods: {
    async viewLogs(url) {
      this.loadingLog = true;
      this.recordLogs = await fileAPI.getRecordLog(url).catch(() => {
        this.$error('Cannot download logs, please try again later');
        this.dialog = false;
        return '';
      });
      this.loadingLog = false;
    },
    deleteRecord({ recordId, revision, recordIndex }) {
      fileAPI.deleteRecord({
        recordId,
        revision,
      });
      this.records.splice(recordIndex, 1);
    },
    finishEditRecord({ record }) {
      const recordIndex = this.records.findIndex(re => re.id === record.id);
      this.records.splice(recordIndex, 1, record);
    },
  },
};
</script>
