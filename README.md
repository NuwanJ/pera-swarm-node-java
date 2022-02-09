# java-swarm-node

This is the Java Swarm Node Implementation for the project, *Pera-Swarm*

More details will be updated soon.

---

## Local Environment Setup

If you need to run this repository on your local environment,
please create a file named *'mqtt.properties'* in path, *'./src/resources/config/'*
as follows and provide your MQTT broker's configurations.

You can select any channel, as same as your simulation server runs on.

```
server=127.0.0.1
port=1883
username=user
password=pass
channel="v1"
```

### Build and release

Please refer following documents for the instructions:
- [How to Export a JAR from IntelliJ](https://lightrun.com/java/how-to-export-a-jar-from-intellij/)
- Use *Export a JAR Using the Build Artifact Method*
