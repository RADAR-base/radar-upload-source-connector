# RADAR-base Data Uploader

RADAR-base Data Uploader is a web-application that enables uploading data to RADAR-Base. It has a Kafka Connect source connector that processes the uploaded data, parse it in appropriate format and send them to Kafka.

This can be used to 
1. Upload data from devices and other sources using a web-interface.
2. Monitor status of the records created

# Components 
The project contains three components:
1. A web-application where user can upload data [radar-upload-frontend](radar-upload-frontend)
2. A back-end webservice application that stores the uploaded data with metadata. [radar-upload-backend](radar-upload-backend)
3. A Kafka Connect source-connector that loads data from backend connector and sends it to Kafka. [kafka-connect-upload-source](kafka-connect-upload-source)


# Screenshots
![login using managementportal credentials](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/add-documentation/docs/assets/Selection_003.bmp)

![create and upload files](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/add-documentation/docs/assets/Selection_010.bmp)

![view records and status](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/add-documentation/docs/assets/Selection_011.bmp)

![view participants and records](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/add-documentation/docs/assets/Selection_012.bmp)

# Usage
If you are a user who would like to upload data to RADAR-base, you require an account on ManagementPortal application of your environment. Please request an account from your System administer if you do not have done.

1. Please login to the uploader application using your account credentials from ManagementPortal.
2. Authorize the uploading application to to perform mentioned operations by clicking on `"Approve"`.
3. Select the project you want to work with.
4. Click on the `"UPLOAD"` button to upload files.
5. Select the participant and type of the file data source, then click on `"CREATE RECORD"`.
6. Once the record is created, upload the relevant file(s), then click on `"UPLOAD"`.
7. Once you are done with uploading files, click on `"DONE"` to finalise the process.
8. You will see a new record created for the corresponding participant with `READY`
9. You can switch to `RECORD` tab and monitor the status of the record.

Please see the (step-by-step guide)[] with screenshots if you like more information.

# Installation

To install fully functional RADAR-base data uploader from source, please use the `docker-compose.yml` under the root directory

```bash
docker-compose up -d --build
```


# Development

## Configuring RADAR-base Data Uploader

**Disclaimer:** This section does not cover how to install or configure the complete RADAR-Base stack. Please visit [RADAR-Docker](https://github.com/RADAR-base/RADAR-Docker) or [RADAR-Kubernetes](https://github.com/RADAR-base/RADAR-Kubernetes) for complete installation guidelines. 

**Note:** Some values of the configurations are specified under the assumption that the deployments will have the same name of docker container services.

Configuring Data Uploader involves configuring the three components mentioned above.

### 1. Configuring radar-upload-backend
Please copy `etc/upload.yml.template` to `etc/upload.yml` and modify the database credentials and the oauth client credentials. The following tables shows the possible properties and explanation.

```yaml

baseUri: "http://0.0.0.0:8085/upload/api/"
managementPortalUrl: "http://managementportal-app:8080/managementportal/"
jdbcDriver: "org.postgresql.Driver"
jdbcUrl: "jdbc:postgresql://radarbase-postgresql:5432/uploadconnector"
jdbcUser: "username" # change this to your database username
jdbcPassword: "password" # change this to your database password
additionalPersistenceConfig:
  "hibernate.dialect": "org.hibernate.dialect.PostgreSQL95Dialect"
enableCors: yes # if you want to enable cors filter to this component specify yes
clientId: "radar_upload_backend" # configure your oauth client id
clientSecret: "secret" # configure the client-secret
sourceTypes: # these are the data source types that are supported to upload data
  - name: "phone-acceleration"
    topics:
      - "android_phone_acceleration"
    contentTypes:
      - "text/csv"
    timeRequired: false
    sourceIdRequired: false
    configuration:
      "setting1": "value1"
      "setting2": "value2"
  - name: "acceleration-zip"
    topics:
      - "android_phone_acceleration"
    contentTypes:
      - "application/zip"
    timeRequired: false
    sourceIdRequired: false
  - name: "altoida-zip"
    topics:
      - "connect_upload_altoida_acceleration"
    contentTypes:
      - "application/zip"
    timeRequired: false
    sourceIdRequired: false

```

#### Adding support to new device type
To add support to additional device types, add a new entry of sourceType to the `sourceTypes` list.
A single sourceType entry is defined as below.

```yaml

- name: "acceleration-zip" # unique identifier of the data source or device (*required)
    topics: # list of topics to send data
      - "android_phone_acceleration" 
    contentTypes: # content types of the data
      - "application/zip"
    timeRequired: false # if the data is large or if uploading data would require a lot of time mention this to true.
    sourceIdRequired: false # if source-id is compulsory to upload data, specify true.    
``` 

If the `upload.yml` file has been modified after starting the serve, restart the service

```bash
docker-compose restart radar-upload-backend
```

### 2. Configuring Kafka Source Connector

Please copy `etc/source-upload.properties.template` to `etc/source-upload.properties` and modify the oauth client credentials and supported converter classes. The following tables shows the possible properties and explanation.


<table class="data-table"><tbody>
<tr>
<th>Name</th>
<th>Description</th>
<th>Type</th>
<th>Default</th>
<th>Valid Values</th>
<th>Importance</th>
</tr>
<tr>
<td>upload.source.poll.interval.ms</td></td><td>How often to poll the records to process.</td></td><td>long</td></td><td>60000</td></td><td></td></td><td>low</td></td></tr>
<tr>
<td>upload.source.client.id</td></td><td>OAuth Client-id of the upload kafka connector.</td></td><td>string</td></td><td>radar-upload-connector-client</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.client.secret</td></td><td>OAuth client-secret of the upload kafka connector.</td></td><td>string</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.client.tokenUrl</td></td><td>Token URL of ManagementPortal to get access token.</td></td><td>string</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.backend.baseUrl</td></td><td>URL of the radar-upload-backend where uploaded files are stored.</td></td><td>string</td></td><td>http://radar-upload-connect-backend:8085/radar-upload/</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.record.converter.classes</td></td><td>List of classes to be used to convert a record.</td></td><td>list</td></td><td>org.radarbase.connect.upload.converter.phone.AccelerometerConverterFactory</td></td><td>Class extending org.radarbase.connect.upload.converter.ConverterFactory</td></td><td>high</td></td></tr>
</tbody></table>

#### Adding support to new device type
To add processing data from new device type, please implement a ConverterFactory that can process the data from corresponding device and add the name of the class to the list of `upload.source.record.converter.classes`.

## Add support to a new data source/device.


Please visit our developer documentation on how to add support for a new data source/device. 
