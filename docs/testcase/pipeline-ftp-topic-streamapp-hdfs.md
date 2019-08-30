# Test case: FTP to Topic to Streamapp to HDFS

- [Create a new workspace with three nodes](#create-a-new-workspace-with-three-nodes)
- [Create two topic and upload a stream app jar into the workspace](#create-two-topic-and-upload-a-stream-app-jar-into-the-workspace)
- [Create a new pipeline, add some connectors, topics and a stream app](#create-a-new-pipeline--add-some-connectors--topics-and-a-stream-app)
- [連接 FTP source -> Topic -> StreamApp -> Topic -> HDFS sink](#連接-ftp-source---topic---streamapp---topic---hdfs-sink)
- [Prepare the required folders and test data on the FTP server](#prepare-the-required-folders-and-test-data-on-the-ftp-server)
- [Start all connectors and stream app](#start-all-connectors-and-stream-app)
- [Verify which test data was successfully dumped to HDFS](#verify-which-test-data-was-successfully-dumped-to-hdfs)

## Create a new workspace with three nodes

**Add three nodes:**

1. On the Nodes page, click the <kbd>NEW NODE</kbd> button.
2. Enter `<ohara_node_host>` in the **Node** field.
3. Enter `<ohara_node_port>` in the **Port** field.
4. Enter `<node_user>` in the **User** field.
5. Enter `<node_password>` in the **Password** field.
6. Click <kbd>TEST CONNECTION</kbd>.
7. If the connection has been successful, click <kbd>SAVE</kbd>.
8. Repeat the above steps as many times **until you have added three nodes**. 

**Create a new workspace:**

1. On the Workspaces page, click the <kbd>NEW WORKSPACE</kbd> button.
2. Enter “wk00” in the **Name** field.
3. Select nodes:
    1. Click the <kbd>Add node</kbd> button.
    2. Click the checkbox to select all nodes, and then click <kbd>Add</kbd>.
4. Click <kbd>Add</kbd>.

## Create two topic and upload a stream app jar into the workspace

**Add topics into workspace:**

1. On the **Workspaces** > **wk00** > **TOPICS** tab, click the <kbd>NEW TOPIC</kbd> button.
2. Enter “T1” in the **Topic name** field and enter default value in other fields, click <kbd>Save</kbd>.
3. Enter “T2” in the **Topic name** field and enter default value in other fields, click <kbd>Save</kbd>.

**Add stream app into workspace:**

1. On the **Workspaces** > **wk00** > **STREAM APPS** tab, click the <kbd>NEW JAR</kbd> button.
2. Browse to the location of your jar file `ohara-streamapp.jar`, click the file, and click <kbd>Open</kbd>. [How to get ohara-streamapp.jar?](#how-to-get-ohara-streamappjar)

 
## Create a new pipeline, add some connectors, topics and a stream app

1. On the **Pipelines** page, click the <kbd>New pipeline</kbd> button.
2. Enter “first_pipeline” in the **Pipeline name** field, click <kbd>Add</kbd>.
3. Click the **Add a source connector icon**, select the **com.island.ohara.connector.ftp.FTPSource** item, click <kbd>Add</kbd>.
4. Enter “ftp_source” in the **Connector name** field, click <kbd>Add</kbd>.
5. Click the **Add a topic** icon, select the **T1** item, click <kbd>Add</kbd>.
6. Click the **Add a stream app** icon, select the **ohara-streamapp** item, click <kbd>Add</kbd>.
7. Enter “dumb” in the **StreamApp name** field, click <kbd>Add</kbd>.
8. Click the **Add a topic** icon, select the **T2** item, click <kbd>Add</kbd>.
9. Click the **Add a sink connector** icon, select the **com.island.ohara.connector.hdfs.HDFSSinkConnector** item, click <kbd>Add</kbd>.
10. Enter “hdfs_sink” in the **Connector name** field, click <kbd>Add</kbd>.

## 連接 FTP source -> Topic -> StreamApp -> Topic -> HDFS sink

**Setup the ftp_source connector:**

1. On the **first_pipeline** page, click the **ftp_source** object in pipeline graph.
2. Select the **COMMON** tab, enter the following in the fields.
    1. Enter “/demo/input” in the **input folder** field.
    2. Enter “/demo/completed” in the **completed folder** field.
    3. Enter “/demo/error” in the **error folder** field.
    4. Enter `<ftp_server_ip>` in the **hostname of ftp server** field.
    5. Enter `<ftp_server_port>` in the **port of ftp server** field.
    6. Enter `ftp_username` in the **user of ftp server** field.
    7. Enter `ftp_password` in the **password of ftp server** field.
3. Select the **CORE** tab, select the **T1** option in the **Topics** field.

**Setup the **dumb** stream app:**

1. Click the **dumb** object in pipeline graph.
2. Enter ohara1 to **filter value**
3. Click the **CORE** tab.
4. Select the **T1** option in the **From topic** field.
5. Select the **T2** option in the **To topic** field.

**Setup the hdfs_sink connector:**

1. Click the **hdfs_sink** object in pipeline graph.
2. Select the **COMMON** tab, enter “hdfs://`<hdfs_host>`:`<hdfs_port>`” in the **HDSF URL** field, use the default value in other fields.
3. Enter 5 to **Flush Size**
4. Select the **CORE** tab, select the **T2** option int the **Topics** field.

## Prepare the required folders and test data on the FTP server

1. Open a terminal, login to FTP server.
```
$ ftp `ftp_server_ip`
Name: `ftp_username`
Password: `ftp_password`
```
2. Create the following folders.
```
ftp> mkdir demo
ftp> cd demo
ftp> mkdir input
ftp> mkdir completed
ftp> mkdir error
ftp> bye
```
3. Copy the test file `demo.csv` to **demo/input** folder. [How to get demo.csv?](#how-to-get-democsv)

## Start all connectors and stream app

1. On the **first_pipeline** page.
2. Click the **Start pipeline** icon.

## Verify which test data was successfully dumped to HDFS

1. Open a terminal, ssh to HDFS server.
2. List all CSV files in **/data/defult-T2/partition0** folder. 
```
$ hdfs dfs -ls /data/default-T2/partition0
/data/default-T1/partition0/part-000000000-000000005.csv
```
3. View the content of **part-000000000-000000005.csv**.
```
$ hdfs dfs -cat /data/default-T2/partition0/part-000000000-000000005.csv
ID,NAME,CREATE_AT
1,ohara1,2019-03-01 00:00:01
```
## How to get demo.csv?
1. Create the txt copy the date save to demo.csv
```
ID,NAME,CREATE_AT
1,ohara1,2019-03-01 00:00:01
2,ohara2,2019-03-01 00:00:02
3,ohara3,2019-03-01 00:00:03
4,ohara4,2019-03-01 00:00:04
5,ohara5,2019-03-01 00:00:05
```
## How to get ohara-streamapp.jar?
1. Open a terminal, go to Ohara source folder.
```
cd ohara/
```
2. Go to StreamApp DumbStreamApp source folder.
```
cd ohara-it/src/main/scala/com/island/ohara/it/streamapp/
```
3. Edit DumbStreamApp.scala
```
vi DumbStreamApp.scala
```
4. Enter "I" in to the "INSERT" mode
```
-- INSERT --
```
4. Overwirte DumbStreamApp class
```
class DumbStreamApp extends StreamApp {

  override def start(ostream: OStream[Row], configs: StreamDefinitions): Unit = {

    // do nothing but only start streamApp and write exactly data to output topic
    ostream.start()
  }
}
```
To
```
import com.island.ohara.common.setting.SettingDef

class DumbStreamApp extends StreamApp {

  override def config(): StreamDefinitions = StreamDefinitions
    .create()
    .add(
      SettingDef
        .builder()
        .key("filterValue")
        .displayName("filter value")
        .documentation("filter the row that contains this value")
        .build())

  override def start(ostream: OStream[Row], configs: StreamDefinitions): Unit = {

    ostream
    // we filter row which the cell values contain the pre-defined "filterValue". Note:
    // 1) configs.string("filterValue") will try to get the value from env and it should NOT be null
    // 2) we ignore case for the value (i.e., "aa" = "AA")
      .filter(
        r =>
          r.cells()
            .stream()
            .anyMatch(c => {
              c.value().toString.equalsIgnoreCase(configs.string("filterValue"))
            }))
      .start()
  }
}
```
5. Save and exit, Enter "Esc" and :wq
```
:wq
```
6. Build streamapp jar
```
gradle clean :ohara-it:jar -PskipManager
```
7. Go to streamapp jar folder
```
cd ohara-it/build/libs/
ls
ohara-it-sink.jar ohara-it-source.jar ohara-streamapp.jar
```