import axios from 'axios';
import uuidv1 from 'uuid/v1';
import { downLoadFile } from '@/helpers';
import { baseURL } from '@/app.config';

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
    return axios.get('/source-types').then(res => res.sourceTypes.map(el => ({
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
    return axios.post('/records', payload)
      .then(record => ({
        ...record.metadata,
        files: record.data.contents,
        id: record.id,
        sourceType: record.sourceType,
        userId: record.data.userId,
      }));
  },
  putRecords({ id, fileName, file }) {
    const headers = {
      'content-type': file.type,
    };
    // eslint-disable-next-line func-names
    return axios.put(`/records/${id}/contents/${fileName}`, file, { headers });
  },

  markRecord({ recordId, revision }) {
    return axios.post(`/records/${recordId}/metadata`, {
      status: 'READY',
      revision,
    });
  },

  async filterRecords({
    projectId, status, userId, getRecordOnly = false,
  }) {
    let endpoint = `/records?projectId=${projectId}`;
    endpoint = status ? endpoint += `&&status=${status}` : endpoint;
    endpoint = userId ? endpoint += `&&userId=${userId}` : endpoint;
    if (getRecordOnly) {
      return axios.get(endpoint)
        .then(res => res.records
          .map(record => ({
            ...record.metadata,
            id: record.id,
            sourceType: record.sourceType,
            userId: record.data.userId,
          }))
          .reverse());
    }

    return axios.get(endpoint)
      .then(res => res.records
        .map(record => ({
          ...record.metadata,
          files: record.data.contents
            .map(file => ({ ...file, uploadFailed: false, uploading: false })),
          id: record.id,
          sourceType: record.sourceType,
          userId: record.data.userId,
        }))
        .reverse());
  },

  async download({ recordId, fileName }) {
    const url = `${baseURL}/records/${recordId}/contents/${fileName}`;
    await axios.get(url);
    downLoadFile(fileName, url);
  },

  async getRecordLog(url) {
    const logs = await axios.get(url);
    return logs;
  },
};
