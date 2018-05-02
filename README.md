# Contrast Maven Plugin

This Maven plugin can be used to allow Contrast to discover vulnerabilities in your application during your integration or verification tests. 

The "install" goal of the plugin is used to download the agent to the /target directory. In order to use the agent, you should add the -javaagent flag to the JVM options of the application that will be monitored by Contrast in the testing lifecycle phases. 

The flag should look something like this, but you should consult the documentation of the application launcher for how to add arbitrary JVM options:

 -javaagent:/git/project/target/contrast.jar

In the "verify" phase, the plugin will check if any new vulnerabilities were discovered during the test phases. The build will fail if any serious vulnerabilities are discovered.

## Goals

* `install`: installs a Contrast Java agent to your local project
* `verify`: checks for new vulnerabilities in your web application


## Configuration Options

| Parameter   | Required | Default | Description                                                                  |
|-------------|----------|---------|------------------------------------------------------------------------------|
| username    | True     |         | Username in TeamServer                                                       |
| serviceKey  | True     |         | Service Key found in Organization Settings page                              |
| apiKey      | True     |         | API Key found in Organization Settings page                                  |
| orgUuid     | True     |         | Organization UUID found in Organization Settings page                        |
| appName     | True     |         | Name of the application as seen in the Contrast site                         |
| apiUrl      | True     |         | API URL to your TeamServer instance                                          |
| serverName  | True     |         | Name of the server you set with -Dcontrast.server                            |
| minSeverity | False    | Medium  | Minimum severity level to verify (can be Note, Low, Medium, High or Critical |
| jarPath     | False    |         | Path to contrast.jar if you already have one downloaded                      |

## Example Configuration

The following is a typical example.

```xml
<plugin>
     <groupId>com.contrast</groupId>
     <artifactId>contrast-maven-plugin</artifactId>
     <executions>
         <execution>
             <id>install-contrast-jar</id>
             <goals>
                <goal>install</goal>
             </goals>
         </execution>
         <execution>
            <id>verify-with-contrast</id>
            <phase>post-integration-test</phase>
            <goals>
                <goal>verify</goal>
            </goals>
         </execution>
     </executions>
     <configuration>
         <username>contrast_user</username>
         <apiKey>API_KEY_HERE</apiKey>
         <serviceKey>SERVICE_KEY_HERE</serviceKey>
         <apiUrl>http://app.contrastsecurity.com/api</apiUrl>
         <orgUuid>ORG_UID_HERE</orgUuid>
         <appName>Test Application</appName>
         <serverName>jenkins.slave1</serverName>
         <minSeverity>High</minSeverity>
     </configuration>
</plugin>
```
