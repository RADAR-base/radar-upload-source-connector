<template>
  <div>
    <v-card>
      <v-toolbar
        flat
      >
        <!-- <v-app-bar-nav-icon /> -->


        <v-toolbar-title>Project 1 - Patients</v-toolbar-title>

        <v-spacer />

        <v-toolbar-title><FileFilter /></v-toolbar-title>

        <template #extension>
          <v-tabs
            v-model="tab"
          >
            <v-tabs-slider />
            <v-tab
              active-class
              class="ml-0"
            >
              <v-icon class="pr-2">
                mdi-account-box-multiple
              </v-icon>
              Patients
            </v-tab>

            <v-tab>
              <v-icon class="pr-2">
                mdi-file-multiple
              </v-icon>
              Files
            </v-tab>
          </v-tabs>
        </template>
      </v-toolbar>


      <!-- <v-card class="mt-4"> -->
      <!-- <v-tabs
      v-model="tab"
      centered
    > -->
      <!-- <v-tabs-slider />
        <v-tab
          active-class
          class="ml-0"
        >
          <v-icon class="pr-2">
            mdi-account-box-multiple
          </v-icon>
          Patients
        </v-tab>

        <v-tab>
          <v-icon class="pr-2">
            mdi-file-multiple
          </v-icon>
          Files
        </v-tab>
      </v-tabs> -->

      <v-tabs-items v-model="tab">
        <v-tab-item :value="0">
          <v-card
            flat
            class="mt-4 pa-2"
          >
            <PatientTable />
          </v-card>
        </v-tab-item>
        <v-tab-item
          :value="1"
        >
          <v-card
            flat
            class="mt-4 pa-2"
          >
            <FileTable />
          </v-card>
        </v-tab-item>
      </v-tabs-items>
      <!-- </v-card> -->
      <!-- </v-tabs> -->
    </v-card>
  </div>
</template>

<script>
import FileTable from '@/components/File/FileTable.vue';
import PatientTable from '@/components/PatientTable/index.vue';
import FileFilter from '@/components/File/FileFilter.vue';

export default {
  components: { FileTable, PatientTable, FileFilter },
  data() {
    return {
      tab: null,
    };
  },
};
</script>

<style>

</style>
