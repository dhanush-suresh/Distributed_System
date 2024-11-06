
## General Information:
1. The introducer should be up and running for any node to join the network.
2. The JAR files are located in the target sub folder of Introducer and swim.

## Running the Introducer
1. cd into the introducer folder
2. Run mvn package
3. Execute "java -jar target/introducer-1.0.jar <Introducer_port_number>"


## Starting the Node
1. cd into the swim folder
2. Run mvn package
3. Execute "java -jar target/swim-1.0.jar <INTRODUCER_IP> <INTRODUCER_PORT>"

## Commands
1. All the Nodes support the following commands:
- list_mem: display the membership list

- leave: voluntarily leave the group (different from a failure)

- list_self: display the node ID of the current machine

- status_sus: displays whether Suspicion is turned on or off.

- display_sus: displays the list of suspected nodes

2. Suspicion can be turned on/off by passing the commands sus_enable and sus_disable respectively at the introducer.

### Notes:
- Please update the grepclient/src/main/resources/machineConnection.properties with the appropriate VM values and file names.
