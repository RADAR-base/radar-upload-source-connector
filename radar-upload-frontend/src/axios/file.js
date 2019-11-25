import axios from 'axios';
import uuidv1 from 'uuid/v1';

export default {
  /** response
    {
    "sourceTypes": [
      {
        "name": "Mp3Audio",
        "contentTypes": ["application/mp3", "audio/mp3"],
        "topics": [
          "high_quality_mp3_audio",
          "low_quality_mp3_audio"
        ],
        "timeRequired": true
      },
      {
        "name": "WrittenText",
        "contentTypes": ["text/csv", "text/json"],
        "topics": ["written_text"],
        "timeRequired": false
      }
    ]
  }
   */
  getSourceTypes() {
    return axios.get('source-types').then(res => res.sourceTypes.map(el => ({
      name: el.name,
      contentTypes: el.contentTypes,
    })));
  },
  // upload has 2 phases
  // Post /records with file info to get id then
  // PUT /records/{id}/contents/{fileName} to upload the file
  postRecords({
    projectId,
    userId,
    sourceType,
    timeZoneOffset = new Date().getTimezoneOffset(),
    time = new Date().toISOString(),
    sourceId = uuidv1(),
  }) {
    const payload = {
      data: {
        projectId,
        userId,
        sourceId,
        time,
        timeZoneOffset,
      },
      sourceType,
    };
    return axios.post('records', payload)
      .then(record => ({
        ...record.metadata,
        files: record.data.contents,
        id: record.id,
        sourceType: record.sourceType,
        userId: record.data.userId,
      }));
  },
  putRecords({
    id, fileName, file, onUploadProgress, fileType,
  }) {
    const headers = {
      'content-type': fileType,
    };
    // eslint-disable-next-line func-names
    return axios.put(`records/${id}/contents/${fileName}`,
      file,
      {
        headers,
        onUploadProgress: progressEvent => onUploadProgress(progressEvent),
      });
  },

  markRecord({ recordId, revision }) {
    return axios.post(`records/${recordId}/metadata`, {
      status: 'READY',
      revision,
    });
  },

  deleteRecord({ recordId, revision }) { // delete record only if its state != PROCESSING
    return axios.delete(`/records/${recordId}?revision=${revision}`);
  },

  deleteFile({ recordId, fileName }) { // delete file only if its records == INCOMPLETE
    return axios.delete(`/records/${recordId}/contents/${fileName}`);
  },


  async filterRecords({
    projectId, size, page, sourceType = '', status = '', userId = '',
  }) {
    let endpoint = `/records?projectId=${projectId}&size=${size}&page=${page}`;
    if (sourceType) endpoint += `&sourceType=${sourceType}`;
    if (userId) endpoint += `&userId=${userId}`;
    if (status) endpoint += `&status=${status}`;

    const res = await axios.get(endpoint).catch((error) => { throw new Error(error); });
    const tableData = res.records
      .map(record => ({
        ...record.metadata,
        files: record.data.contents
          .map(file => ({ ...file, uploadFailed: false, uploading: false })),
        id: record.id,
        sourceType: record.sourceType,
        userId: record.data.userId,
      }));
    const { totalElements } = res;
    return { totalElements, tableData };
  },

  async getRecordLog(url) {
    return axios.get(url);
  },
};
