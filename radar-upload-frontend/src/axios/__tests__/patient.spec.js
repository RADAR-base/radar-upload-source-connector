/* eslint-disable no-undef */
import axios from 'axios';
import patientAPI from '../patient';

jest.mock('axios');

describe('axios/patient', () => {
  it('filteredPatients', () => {
    const resp = {
      users: [{
        id: 'abcdef',
        projectId: 'req.params.projectId',
        externalId: 'AB10',
        status: 'ACTIVATED',
      }],
    };
    axios.get.mockResolvedValue(resp);

    const expectedData = resp.users.map((el, index) => ({
      sequence: index + 1,
      patientName: el.externalId,
      status: el.status,
      patientId: el.id,
    }));

    return patientAPI.filteredPatients().then((data) => {
      expect(data).toEqual(expectedData);
    });
  });
});
