import axios from 'axios';

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
    return axios.get('/sourceTypes').then(res => res.sourceTypes.map(el => ({
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
        sourceType,
        time: new Date().toISOString(),
        timeZoneOffset: new Date().getTimezoneOffset(),
      },
      sourceType,
    };
    return axios.post('/records', payload)
      .then(res => ({ id: res.id, createdDate: res.metadata.createdDate }));
  },
  putRecords({ id, fileName, file }) {
    return axios.put(`/records/${id}/contents/${fileName}`, { file });
  },
};
