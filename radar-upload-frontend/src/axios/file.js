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
    return axios.get('/source-types').then(res => res.sourceTypes.map(el => ({
      name: el.name,
      contentTypes: el.contentTypes,
    })));
  },
  // upload has 2 phases
  // Post /records with file info to get id then
  // PUT /records/{id}/contents/{fileName} to upload the file
  postRecords({ projectId, userId, sourceType }) {
    const payload = {
      data: {
        projectId,
        userId,
        sourceId: uuidv1(),
        time: new Date().toISOString(),
        timeZoneOffset: new Date().getTimezoneOffset(),
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
    return axios.put(`/records/${id}/contents/${fileName}`, { file });
  },

  async filterRecords({
    projectId, status, userId, getFileOnly = false,
  }) {
    let endpoint = `/records?projectId=${projectId}`;
    endpoint = status ? endpoint += `&&status=${status}` : endpoint;
    endpoint = userId ? endpoint += `&&userId=${userId}` : endpoint;
    if (getFileOnly) {
      const data = await axios.get(endpoint)
        .then(res => res.records
          .map(el => el.data)
          .map(each => ({ contents: each.contents, userId: each.userId })));

      const files = data
        .map(each => each.contents)
        .reduce((preVal, currVal) => [...preVal, ...currVal], [])
        .map((file, index) => ({
          fileSize: `${file.size} kb`,
          patient: data[index].fileName || data[index].userId,
          fileName: file.fileName,
          fileType: file.contentType,
          uploadedAt: file.createdDate,
        }));
      return files;
    }

    return axios.get(endpoint)
      .then(res => res.records
        .map(record => ({
          ...record.metadata,
          files: record.data.contents,
          id: record.id,
          sourceType: record.sourceType,
          userId: record.data.userId,
        }))
        .reverse());
  },
};
