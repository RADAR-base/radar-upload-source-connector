# Getting started with RADAR-base Data Uploader - Step-by-step Guide

Requirements:
- Working browser
- A valid account on ManagmentPortal that has permission to upload-data (e.g. `PROJECT_ADMIN` or `SYS_ADMIN`)

1. Go to upload application interface of your deployment at `https://domain-name/upload`
2. Click on Login with ManagementPortal.
![login with MP](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_001.png)
3. You will be redirected to a login screen. Please enter your **credentials from Management Portal** to login.
![login using managementportal credentials](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_003.png)
4. Once you have successfully logged in, Please click on the **Approve** button to allow the web-interface to upload data.
![approve frontend](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_004.png)
5. Now you will see the home screen of the application. It will list all of the projects you have access to. 

   Please select on the project you want to work with.
![select project](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_005.png)
6. In the project view under the **PARTICIPANTS** tab, you will see all of the participants enrolled.
![view project](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_006.png)

7. Click on the **UPLOAD** button to upload data.
    7.1 Select the participant you want to upload data from. The participant's external id will be listed in the drop-down. 
    7.2 Select the type of device or data source of the data from the drop down list.
    7.3 Click on **CREATE RECORD**
![create record](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_007.png)
    
8. Once you have created the record,you will see the identifier and the status of the record. In this example, it is 542 and INCOMPLETE.

   Now you can start adding data to upload by drag and drop or selecting a file from your computer.
![add files](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_008.png)
 
9. Once you have created the record,you will see the identifier and the status of the record. In this example, it is 542 and INCOMPLETE.

   8.1 Now you can start adding data to upload by drag and drop or selecting a file from your computer.
   8.2 You can select one or more files to upload.
   8.3 Then click on **UPLOAD**.
![upload files](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_009.png)

10. Once you are done with uploading files for this record, please click on the **DONE** button. This will finialize the record creation process and mark it READY for post processing by the RADAR-base platform.
![mark it done](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_010.png)

11. Under the **RECORDS** tab, you can see a list of all records available under your project.
    11.1 Here you can **monitor the status** of the records. Description of the record status are mentioned below.
    
        INCOMPLETE: You have created a record entry, but it is not ready for processing. Until you upload files and click on the DONE button the record will be INCOMPLETE.
        READY: The record has all of the files uploaded and ready for processing.
        QUEUED: The record has been queued for automatic processing.
        PROCESSING: The record is being processed.
        FAILED: The automatic record processing has been failed. Possible causes could be the data is corrupted, or the data was not marked with the correct device-type/source-type, something else went wrong while processing. 
        SUCCEEDED: The record processing was successful.
    11.2 You can **edit** the record, if available.
    
    You can make changes to existing records if necessary except when it is in the PROCESSING status.
    e.g. You can edit this record that were FAILED, if there was a human error in record creation.
    
    11.3 Filter records based on available categories.
    
    11.4 Delete a record, if necessary. 
     
![record view](https://raw.githubusercontent.com/RADAR-base/radar-upload-source-connector/master/docs/assets/Selection_011.png)
