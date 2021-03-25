# RADAR-base Upload Source connector
This sub module contains upload source connector of RADAR-base platform. This application periodically pools data stored in [radar_upload_backend](../radar-upload-backend) for processing then uploading it to Kafka. The processing of the data depends on the metadata of each record read from the database and eventually converted into to avro records and published to Kafka.

Some files such as wearable camera data (images) and raw binary data from Physilog5 are directly uploaded to the target destination.

Each record of data belongs to a specific source-type and each source-type should have a matching `ConverterFactory` implemented in `radar-upload-source-connector`.

Currently supported ConverterFactory classes are listed below:
- org.radarbase.connect.upload.converter.altoida.AltoidaConverterFactory
- org.radarbase.connect.upload.converter.axivity.AxivityConverterFactory
- org.radarbase.connect.upload.converter.oxford.WearableCameraConverterFactory
- org.radarbase.connect.upload.converter.gaitup.Physilog5ConverterFactory

To add support to process data from new source-types, a compatible `ConverterFactory` should be implemented according to the data structure of the data.


### Installation

In addition to Zookeeper and Kafka brokers, [radar-upload-backend] and [managment-portal] applications should be running to use `radar-upload-source-connector`

To run a all dependent services of radar-upload-source-connect in your localhost, please refer to the [docker-compose](../docker-compose.yml)
 
### Usage

First, create OAuth Clients for `radar-upload-source-connector` and `radar-upload-backend` in `Management Portal`


Copy `etc/source-upload.properties.template` to `docker/source-upload.properties` and enter
the client ID and client secret of `radar-upload-source-connector`.
 
By default all `ConverterFactory` classes are enabled. You can enable a subset of them by specifying a list of `ConverterFactory` classes  in comma-separated-value for property `upload.source.record.converter.classes`

The following tables shows the all properties.

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

<td>upload.source.client.id</td></td><td>OAuth Client id of radar-upload-source-connector in Management Portal.</td></td><td>string</td></td><td>radar-upload-connector-client</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.client.secret</td></td><td>OAuth Client secret of radar-upload-source-connector in Management Portal.</td></td><td>string</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.client.tokenUrl</td></td><td>OAuth Token url of Managment Portal.</td></td><td>string</td></td><td>http://managementportal-app:8080/managementportal/oauth/token</td></td><td>Resolvable URL of Managmemnt portal plus /oauth/token</td></td><td>low</td></td></tr>
<tr>
<td>upload.source.backend.baseUrl</td></td><td>Base URL of radar-upload-backend</td></td><td>string</td></td><td>http://radar-upload-connect-backend:8085/radar-upload/</td></td><td></td></td><td>low</td></td></tr>
<tr>
<td>upload.source.poll.interval.ms</td></td><td>How often the connector should poll new data from radar-upload-backend to process in millseconds </td></td><td>long</td></td><td>60000</td></td><td></td></td><td>high</td></td></tr>
<tr>
<td>upload.source.queue.size</td></td><td>Capacity of the records queue.</td></td><td>int</td></td><td>1000</td></td><td></td></td><td>high</td></tr>
<tr>
<td>upload.source.record.converter.classes</td></td><td>List of `ConverterFactory` classes to be enabled, separated by commas.</td></td><td>list</td></td><td></td></td><td></td></td><td>high</td></td></tr>
<tr>
</tbody></table>

**If any of the `ConverterFactory` with direct file upload is enabled, the following properties must be configured.** 
<table class="data-table"><tbody>
<tr>
<th>Name</th>
<th>Description</th>
<th>s3</th>
<th>sftp</th>
<th>local</th>
</tr>
<tr>
<td>upload.source.file.uploader.type</td></td><td>What type of file uploader should be used for direct file uploads. Specify a value from `s3`, `sftp` or `local`. </td></td><td>s3</td></td><td>sftp</td></td>local</tr>
<tr>
<td>upload.source.file.uploader.target.endpoint</td></td><td>Advertised URL Endpoint of the file upload target. For sftp, specify advertised url in sftp://host:port format. If port is not specified default port 22 will be used.</td></td><td>http://minio:9000/</td></td><td>sftp://hostname:port/ </td></td><td>file://</td></td></tr>
<tr>
<td>upload.source.file.uploader.target.root.directory</td></td><td>Target root directory or a s3 bucket name where files should be uploaded to.</td></td><td>radar-output-storage</td></td><td>/output</td></td>/output</tr>
<tr>
<td>upload.source.file.uploader.username</td></td><td>Username to upload files to the target. Specify the access-key if s3 is chosen.</td></td><td>access-key</td></td><td>sftp username</td></td><td></td></td></tr>
<tr>
<td>upload.source.file.uploader.password</td></td><td>Username to upload files to the target. Specify the secret-key if s3 is chosen.</td></td><td>secret-key</td></td><td>sftp password</td></td><td></td></td></tr>
<tr>
<td>upload.source.file.uploader.sftp.private.key.file</td></td><td>Path of private-key file if using private key for uploading files using sftp.</td></td><td></td></td><td>/etc/upload-upload-source-connect/ssh-privatekey</td></td><td></td></tr>
<tr>
<td>upload.source.file.uploader.sftp.passphrase</td></td><td>Passphrase of the private-key file if using private key for uploading files using sftp.</td></td><td></td></td><td>passphrase for private-key</td></td><td></td></tr>
<tr>
</tbody></table>


## Contributing

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html).
If you want to contribute a feature or fix browse our [issues](https://github.com/RADAR-base/radar-upload-source-connector/issues), and please make a pull request.
