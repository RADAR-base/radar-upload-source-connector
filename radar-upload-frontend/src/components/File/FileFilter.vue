<template>
  <div class="file-filter">
    <v-autocomplete
      v-model="autocomplete"
      class="mt-4"
      prepend-inner-icon="mdi-magnify"
      label="Search your patients"
      :items="items"
      deletable-chips
      chips
    >
      <!-- add filter button and chips-->
      <template #append-outer>
        <v-menu
          v-model="menu"
          :close-on-content-click="false"
        >
          <template #activator="{ on: menu }">
            <v-tooltip bottom>
              <template #activator="{ on: tooltip }">
                <v-btn
                  color="primary lighten-2"
                  v-on="{ ...tooltip, ...menu }"
                  class="white--text ml-0"
                  fab
                  x-small
                >
                  <v-icon dark>
                    mdi-filter
                  </v-icon>
                </v-btn>
              </template>
              <span>Click to add more filters</span>
            </v-tooltip>
          </template>


          <v-card>
            <v-list>
              <v-list-item>
                <v-select
                  label="Select file status"
                  v-model="fileStatus"
                  :items="fileStatusList"
                />
              </v-list-item>
              <v-list-item>
                <v-select
                  label="Select file type"
                  v-model="fileType"
                  :items="fileTypeList"
                />
              </v-list-item>
            </v-list>

            <v-card-actions>
              <v-spacer />

              <v-btn
                text
                @click="menu = false"
              >
                Cancel
              </v-btn>

              <v-btn
                color="primary"
                text
                @click="menu = false"
              >
                Search
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-menu>
        <div>
          <v-chip
            v-if="fileType"
            close
            @click:close="fileType=''"
          >
            <span>
              {{ fileType }}
            </span>
          </v-chip>

          <v-chip
            v-if="fileStatus"
            close
            @click:close="fileStatus=''"
          >
            <span> {{ fileStatus }}</span>
          </v-chip>
        </div>
      </template>
    </v-autocomplete>
  </div>
</template>

<script>
export default {
  data() {
    return {
      items: ['Audio 10', 'Audio 22', 'Audio 1', 'Audio 2', 'Audio 1', 'Audio 2', 'Audio 1', 'Audio 2', 'Audio 17', 'Audio 25', 'Audio 12', 'Audio 28', 'Audio 13', 'Audio 23', 'Audio 1q', 'Audio 2w'],
      fileStatus: '',
      fileStatusList: ['complete', 'incomplete'],
      fileType: '',
      fileTypeList: ['mp3', 'text'],
      menu: false,
      autocomplete: '',
    };
  },
};
</script>

<style lang="scss" scoped>
.v-list {
  padding: 0 !important;
}

</style>
