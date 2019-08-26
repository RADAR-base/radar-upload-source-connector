/* eslint-disable no-undef */
import axios from 'axios';
import flushPromises from 'flush-promises';
import fileAPI from '../file';

jest.mock('axios');

function expected(expectedValue, service) {
  return fileAPI[service]()
    .then((data) => { expect(data).toEqual(expectedValue); });
}

describe.only('axios/file', () => {
  afterEach(() => {
    axios.get.mockClear();
    axios.post.mockClear();
    axios.put.mockClear();
  });
  it('getSourceTypes', () => {
    const response = {
      sourceTypes: [
        {
          name: 'Mp3Audio',
          contentTypes: ['application/mp3', 'audio/mp3'],
          topics: [
            'high_quality_mp3_audio',
            'low_quality_mp3_audio',
          ],
          timeRequired: true,
        },
        {
          name: 'WrittenText',
          contentTypes: ['text/csv', 'text/json'],
          topics: ['written_text'],
          timeRequired: false,
        },
      ],
    };
    axios.get.mockResolvedValue(response);
    const expectedValue = [{
      name: 'Mp3Audio',
      contentTypes: ['application/mp3', 'audio/mp3'],
    },
    {
      name: 'WrittenText',
      contentTypes: ['text/csv', 'text/json'],
    }];
    return expected(expectedValue, 'getSourceTypes');
  });

  it('putRecords', () => {
    const id = 'id';
    const fileName = 'fileName';
    const file = 'fileObject';
    axios.put.mockResolvedValue();
    return fileAPI.putRecords({ id, fileName, file })
      .then(() => {
        expect(axios.put).toBeCalledWith(`/records/${id}/contents/${fileName}`, { file });
      });
  });

  it('postRecords', async () => {
    const params = {
      projectId: 12,
      userId: 12,
      sourceType: '12312',
      sourceId: 'sourceId',
      time: 'time',
      timeZoneOffset: 'timeZoneOffset',
    };
    const record = {
      id: 1,
      sourceType: 'sourceType',
      metadata: ['some metadata'],
      data: {
        userId: 'sourceType',
        contents: [],
      },
    };
    const expectedVal = {
      ...record.metadata,
      files: record.data.contents,
      id: record.id,
      sourceType: record.sourceType,
      userId: record.data.userId,
    };

    axios.post.mockResolvedValue(record);

    const payload = {
      data: {
        projectId: params.projectId,
        userId: params.userId,
        sourceId: params.sourceId,
        time: params.time,
        timeZoneOffset: params.timeZoneOffset,
      },
      sourceType: params.sourceType,
    };
    return fileAPI.postRecords(params)
      .then((data) => {
        expect(data).toEqual(expectedVal);
        expect(axios.post).toBeCalledWith('/records', payload);
      });
  });

  it('fileterRecords', async () => {
    const params1 = {
      projectId: 12,
      userId: 12,
      status: 'Status',
      getFileOnly: true,
    };
    const params2 = {
      projectId: 12,
      userId: 12,
      status: 'Status',
      getFileOnly: false,
    };
    const response = {
      records: [
        {
          id: 12,
          data: {
            projectId: 'radar-test',
            userId: 'testUser',
            sourceId: 'source',
            time: '2019-03-04T00:00:00',
            timeZoneOffset: 0,
            contents: [{
              fileName: 'Gibson.mp3',
              createdDate: 'createdDate',
              contentType: 'audio/mp3',
              size: 10,
              url: '/records/{id}/contents/{fileName}',
            }],
          },
          sourceType: 'Mp3Audio',
          metadata: {
            revision: 2,
            createdDate: 'createdDate',
            modifiedDate: 'modifiedDate',
            committedDate: null,
            status: 'READY',
            message: 'Data has succesfully been uploaded to the backend.',
            logs: null,
          },
        }],
    };
    const expectedVal1 = [{
      fileSize: `${response.records[0].data.contents[0].size} kb`,
      patient: response.records[0].data.userId,
      fileName: response.records[0].data.contents[0].fileName,
      fileType: response.records[0].data.contents[0].contentType,
      uploadedAt: response.records[0].data.contents[0].createdDate,
    }];

    const expectedVal2 = [{
      ...response.records[0].metadata,
      files: response.records[0].data.contents,
      sourceType: response.records[0].sourceType,
      userId: response.records[0].data.userId,
      id: response.records[0].id,
    }];

    let endpoint = `/records?projectId=${params1.projectId}`;
    endpoint = params1.status ? endpoint += `&&status=${params1.status}` : endpoint;
    endpoint = params1.userId ? endpoint += `&&userId=${params1.userId}` : endpoint;

    axios.get.mockResolvedValue(response);


    await fileAPI.filterRecords(params1)
      .then((data) => {
        expect(data).toEqual(expectedVal1);
        expect(axios.get).toBeCalledWith(endpoint);
      });

    await fileAPI.filterRecords(params2)
      .then((data) => {
        expect(data).toEqual(expectedVal2);
        // expect(axios.get).toBeCalledWith(endpoint);
      });
  });
});
