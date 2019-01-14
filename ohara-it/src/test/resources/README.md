ItConnector.jar and ItConnector2.jar are used in testing. TestLoadCustomJarToWorkerCluster will create a worker cluster
with them and check whether worker cluster hava loaded the connector correctly. The dumb connector put in ItConnector.jar and ItConnector2.jar
are shown below:

1) com.island.ohara.it.ItConnector
2) com.island.ohara.it.ItConnector2

NOTED: ItConnector implements our RowSourceConnector and ItConnector2 implements SourceConnector. If the base connector
have updated in the future, both test-purposed connectors should be updated also. Their source code doesn't keep in git
repo so you have to write them again and then build ohara to get jar file.
