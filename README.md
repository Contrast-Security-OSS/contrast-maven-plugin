# Contrast Maven Plugin

Repository for the Contrast Maven plugin. This plugin will download and install the Contrast Java agent during the initialize lifecycle phase.
It will then verify no new vulnerabilities were found before you called the verify goal and after the agent was downloaded.

## Goals

* `install`: installs a Contrast Java agent to your local project
* `verify`: checks for new vulnerabilities in your web application


## Configuration Options

| Parameter   | Required | Default | Description                                             |
|-------------|----------|---------|---------------------------------------------------------|
| username    | True     |         | Username in TeamServer                                  |
| serviceKey  | True     |         | Service Key found in Organization Settings              |
| apiKey      | True     |         | Api Key found in Organization Settings                  |
| orgUuid     | True     |         | Organization Uuid found in Organization Settings        |
| appName     | True     |         | Name of application                                     |
| apiUrl      | True     |         | API Url to your TeamServer instance                     |
| serverName  | True     |         | Name of server you set with -Dcontrast.server           |
| minSeverity | False    | Medium  | Minimum severity level to verify                        |
| jarPath     | False    |         | Path to contrast.jar if you already have one downloaded |

## Example Configurations

```xml
<plugin>
     <groupId>com.aspectsecurity.contrast</groupId>
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
         <apiKey>ASDFONASFNASFIASNFLNAS</apiKey>
         <serviceKey>ADSFASFASF</serviceKey>
         <apiUrl>http://www.app.contrastsecurity.com/api</apiUrl>
         <orgUuid>QWER-ASDF-ZXCV-ERTY</orgUuid>
         <appName>Test Application</appName>
         <serverName>jenkins.slave1</serverName>
         <minSeverity>High</minSeverity>
     </configuration>
</plugin>
```
