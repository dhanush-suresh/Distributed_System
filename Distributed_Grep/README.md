## General Information:
- The input should be of the format grep options "phrase"

## Running the Server
1. cd into the grepserver folder
2. Run mvn package
3. Execute "java -jar target/grepserver-1.0.jar"
### Notes
- Machine Log File Placement:
    - Please place your machine log files in a directory titled "log_files"
    - In grepserver/App.java replace "logFileDir" in line 23 with the parent directory of "log_files" directory

## Running the Client
1. cd into the grepclient folder
2. Run mvn package
3. Execute "java -jar target/grepclient-1.0.jar"
### Notes:
- Please update the grepclient/src/main/resources/machineConnection.properties with the appropriate VM values and file names.
