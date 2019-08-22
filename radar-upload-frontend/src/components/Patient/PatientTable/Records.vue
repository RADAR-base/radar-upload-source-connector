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
      <span v-show="!loading&&patientRecords.length>0">
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
        <v-list-item-avatar>
          <v-progress-circular
            v-if="record.status==='INCOMPLETE'"
            indeterminate
            color="primary"
          />
          <v-icon
            color="info"
            v-if="record.status==='READY'"
          >
            mdi-checkbox-marked-circle
          </v-icon>
        </v-list-item-avatar>

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

      <v-list-item
        v-for="(file,fileIndex) in record.files"
        :key="fileIndex"
      >
        <v-list-item-avatar>
          <v-icon v-show="!file.uploading">
            mdi-file
          </v-icon>

          <v-progress-circular
            class="mt-2"
            v-show="file.uploading"
            indeterminate
            color="primary"
          />
        </v-list-item-avatar>
        <v-list-item-content>
          <v-list-item-title v-text="file.fileName" />
          <v-list-item-subtitle>Size: {{ file.size }} Kb</v-list-item-subtitle>
          <v-list-item-subtitle>{{ file.createdDate | localTime }}</v-list-item-subtitle>
        </v-list-item-content>
      </v-list-item>
    </v-list-group>
  </v-list>
</template>

<script>
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
    };
  },
  computed: {
    records() {
      return this.patientRecords.map(record => ({ active: false, ...record }));
    },
  },
};
</script>