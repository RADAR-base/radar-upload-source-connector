/* eslint-disable no-undef */
import axios from 'axios';
import flushPromises from 'flush-promises';
import { O_TRUNC } from 'constants';
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
    const response = {
      id: 1,
      metadata: { createdDate: 'date' },
    };
    const expectedVal = {
      id: response.id,
      createdDate: response.metadata.createdDate,
    };
    axios.post.mockResolvedValue(response);
    const params = { projectId: 12, userId: 12, sourceType: '12312' };
    const payload = {
      projectId: params.projectId,
      userId: params.userId,
      time: new Date().toISOString(),
      timeZoneOffset: new Date().getTimezoneOffset(),
    };
    return fileAPI.postRecords(params)
      .then((data) => {
        expect(data).toEqual(expectedVal);
        expect(axios.post).toBeCalledWith('/records', { data: payload, sourceType: params.sourceType });
      });
  });

  it('fileterRecords', async () => {
    const projectId = '123';
    const status = 'done';
    const userId = '123';
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
    const expectedVal = [{
      sequence: 1,
      fileName: 'Gibson.mp3',
      fileType: 'audio/mp3',
      status: 'Incomplete',
      uploadedAt: 'createdDate',
    }];
    let endpoint = `/records?projectId=${projectId}`;
    endpoint = status ? endpoint += `status=${status}` : endpoint;
    endpoint = userId ? endpoint += `userId=${userId}` : endpoint;

    axios.get.mockResolvedValue(response);
    await fileAPI.filterRecords({ projectId, status, userId })
      .then((data) => {
        expect(data).toEqual(expectedVal);
        expect(axios.get).toBeCalledWith(endpoint);
      });

    await fileAPI.filterRecords({ projectId })
      .then(() => {
        expect(axios.get).toBeCalledWith(`/records?projectId=${projectId}`);
      });
  });
});
