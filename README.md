# RADAR Manual data upload connector

The manual data upload Kafka connector will have the following components:

- Data upload frontend
- Data upload backend
- Kafka source connector

The frontend reads project settings from the ManagementPortal and uses the ManagementPortal permission system to manage projects. It can upload files with given file types to the backend and can report the status of the files. The backend stores files and file metadata, and manages updates. Finally, Kafka source connector reads file records from the data upload backend and processes them with custom converters.

## Data upload backend

The backend should have the following API calls

**Get converter types**
GET /sourceTypes

```json
{
  "sourceTypes": [
    {
      "name": "Mp3Audio",
      "contentTypes": ["application/mp3", "audio/mp3"],
      "topics": [
        "high_quality_mp3_audio",
        "low_quality_mp3_audio"
      ],
      "requiresTime": true
    },
    {
      "name": "WrittenText",
      "contentTypes": ["text/csv", "text/json"],
      "topics": ["written_text"],
      "requiresTime": false
    }
  ]
}
```

**Upload a new data point**
POST /records

```
X-Progress-ID: <random-UUID>
Content-Type: multipart/form-data

------Boundary12345
projectId=radar-test&userId=testUser&sourceType=Mp3Audio

------Boundary12345
Content-Disposition: form-data; name="fileUpload1[]"; filename="Gibson.mp3"
Content-Type: audio/mp3

01011101101101...
```

Returns

HTTP 201 Created<br>
Location: /records/{id}

```json
{
  "id": 12,
  "projectId": "radar-test",
  "userId": "testUser",
  "name": "Gibson.mp3",
  "sourceType": "Mp3Audio", 
  "dateTime": "2019-03-04T00:00:00",
  "timeZoneOffset": 0,
  "dateTimeAdded": "2019-03-04T01:23:45Z",
  "dateTimeUploaded": null,
  "status": "READY",
  "logs": null,
  "revision": 1,
}
```

**Get progress for a record** GET /progress
X-Progress-ID: <random-UUID>
See https://www.nginx.com/resources/wiki/modules/upload_progress/ for result values.

**Get the logs**
GET /records/{id}/logs
```
...
```

**Get records for given filters**<br>
GET /records<br>
GET /records?projectId=radar-test&userId=testUser&status=ready&limit=10&lastId=11

```
{
  "limit": 10,
  "records": [
    {
      "id": 12,
      "projectId": "radar-test",
      "userId": "testUser",
      "name": "Gibson.mp3",
      "converter": "Mp3Audio",
      "dateTime": "2019-03-04T00:00:00",
      "timeZoneOffset": 0,
      "dateTimeUploaded": "2019-03-04T01:23:45Z",
      "dateTimeCommitted": null,
      "status": "READY",
      "logs": "/records/12/logs"
    }
  ]
}
```


**For getting queued data**
GET /records?limit=10&status=ready

Returns

```json
{
  "limit": 10,
  "records": [
    {
      "id": 12,
      "projectId": "radar-test",
      "userId": "testUser",
      "name": "Gibson.mp3",
      "converter": "Mp3Audio",
      "dateTime": "2019-03-04T00:00:00",
      "timeZoneOffset": 0,
      "dateTimeUploaded": "2019-03-04T01:23:45Z",
      "dateTimeCommitted": null,
      "status": "READY",
      "revision": 1,
    }
  ]
}
```

GET /records/{fileId}/contents
Content-Type: "application/mp3"

**Start transaction**
POST /records/{fileId}

```json
{
  "revision": 1,
  "status": "PROCESSING"
}
```

returns
HTTP 200

```json
{
  "id": 12,
  "projectId": "radar-test",
  "userId": "testUser",
  "name": "Gibson.mp3",
  "converter": "Mp3Audio",
  "dateTime": "2019-03-04T00:00:00",
  "timeZoneOffset": 0,
  "dateTimeUploaded": "2019-03-04T01:23:45Z",
  "dateTimeCommitted": null,
  "status": "PROCESSING",
  "revision": 2
}
```

or HTTP 409 Conflict if the revision does not match (i.e. another process is processing this file.)

**Finalize transaction**
POST /records/{fileId}

```json
{
  "revision": 2,
  "status": "FAILED | SUCCEEDED",
  "logs": "..."
}
```

Returns

```json
{
  "id": 12,
  "projectId": "radar-test",
  "userId": "testUser",
  "name": "Gibson.mp3",
  "converter": "Mp3Audio",
  "dateTime": "2019-03-04T00:00:00",
  "timeZoneOffset": 0,
  "dateTimeUploaded": "2019-03-04T01:23:45Z",
  "dateTimeCommitted": null,
  "status": "SUCCEEDED",
  "revision": 3
}
```

or HTTP 409 Conflict if the revision does not match (i.e. another process is processing this file.)
