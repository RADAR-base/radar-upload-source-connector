<template>
  <v-data-table
    :headers="headers"
    :items="items"
    single-expand
    show-expand
    :expanded.sync="expanded"
    item-key="sequence"
  >
    <template #item.updatedAt="{item}">
      <td class="pl-0">
        {{ item.updatedAt | moment("YYYY/MM/DD") }}
      </td>
    </template>


    <template #item.uploadButton="{item}">
      <!-- {{ item }} -->
      <td class="pl-0">
        <UploadButton />
      </td>
    </template>


    <template #expanded-item="{item}">
      <td
        :colspan="12"
        class="py-4"
      >
        <FileList :patient-files="patientFiles" />
      </td>
    </template>
  </v-data-table>
</template>

<script>
import UploadButton from './UploadButton';
import FileList from './FileList';

export default {
  components: {
    UploadButton,
    FileList,
  },
  data() {
    return {
      expanded: [],
      headers: [
        { text: '#', value: 'sequence' },
        { text: 'Patient name', value: 'patientName' },
        { text: 'Updated at', value: 'updatedAt' },
        { text: 'Upload', value: 'uploadButton', sortable: false },
      ],
      items: [
        {
          sequence: 1,
          patientName: 'Patient 1',
          updatedAt: Date.now(),
        },
        {
          sequence: 2,
          patientName: 'Patient 2',
          updatedAt: Date.now(),
        },
      ],
      patientFiles: [
        {
          sequence: 1,
          fileName: 'Audio1',
          fileType: 'mp3',
          status: 'Incomplete',
          uploadedAt: Date.now(),
        },
        {
          sequence: 2,
          fileName: 'Checklist',
          fileType: 'text',
          status: 'Completed',
          uploadedAt: Date.now(),
        },
        {
          sequence: 1,
          fileName: 'Audio1',
          fileType: 'mp3',
          status: 'Incomplete',
          uploadedAt: Date.now(),
        },
        {
          sequence: 2,
          fileName: 'Checklist',
          fileType: 'text',
          status: 'Completed',
          uploadedAt: Date.now(),
        },
      ],
    };
  },
};
</script>

<style>

</style>
