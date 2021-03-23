/* eslint-disable no-undef */
import axios from 'axios';
import fileAPI from '../file';

jest.mock('axios');

function expected(expectedValue, service) {
  return fileAPI[service]()
    .then((data) => { expect(data).toEqual(expectedValue); });
}

describe('axios/file', () => {
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
    const file = { type: 'fileObject' };
    const fileType = 'fileType';
    const onUploadProgress = jest.fn();
    const headers = { 'Content-Type': fileType };
    axios.put.mockResolvedValue();
    return fileAPI.putRecords({
      id, fileName, file, fileType, onUploadProgress,
    })
      .then(() => {
        expect(axios.put).toBeCalledWith(`records/${id}/contents/${fileName}`, file, expect.objectContaining({
          headers,
        }));
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
        expect(axios.post).toBeCalledWith('records', payload);
      });
  });

  it('markRecord', () => {
    const recordId = 'id';
    const revision = 'revision';
    const body = {
      status: 'READY',
    };
    axios.post.mockResolvedValue();
    return fileAPI.markRecord({ recordId, revision }).then(() => {
      expect(axios.post).toBeCalledWith(`records/${recordId}/metadata`, { ...body, revision });
    });
  });

  it('fileterRecords', async () => {
    const params = {
      projectId: 12,
      userId: 12,
      status: 'Status',
      sourceType: 'sourceType',
      size: 'size',
      page: 10,
    };
    const response = {
      totalElements: 10,
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

    const expectedVal = [{
      ...response.records[0].metadata,
      files: response.records[0].data.contents
        .map((file) => ({ ...file, uploadFailed: false, uploading: false })),
      sourceType: response.records[0].sourceType,
      userId: response.records[0].data.userId,
      id: response.records[0].id,
    }];

    const endpoint = `/records?projectId=${params.projectId}&size=${params.size}&page=${params.page}&sourceType=${params.sourceType}&userId=${params.userId}&status=${params.status}`;

    axios.get.mockResolvedValue(response);

    const res = await fileAPI.filterRecords(params);
    expect(res).toEqual({ tableData: expectedVal, totalElements: 10 });
    expect(axios.get).toBeCalledWith(endpoint);
  });
});
