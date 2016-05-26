# Contrast Maven Plugin

Repository for the Contrast Maven plugin. This plugin will download and install the Contrast Java agent during the initialize lifecycle phase.
Then it will verify no new vulnerabilities were found before you call the verify goal.

## Goals

* `install`: installs a Contrast Java agent to your local project
* `verify`: checks for new vulnerabilities in your web application


## Configuration Options

| Parameter   | Required | Default | Description |
|-------------|----------|---------|-------------|
| username    | True     |         |             |
| serviceKey  | True     |         |             |
| apiKey      | True     |         |             |
| orgUuid     | True     |         |             |
| appId       | True     |         |             |
| apiUrl      | True     |         |             |
| serverName  | True     |         |             |
| minSeverity | False    | Medium  |             |
| jarPath     | False    |         |             |

## Example Configurations

```xml
<plugin>
     <groupId>com.aspectsecurity.contrast</groupId>
     <artifactId>contrast-maven-plugin</artifactId>
     <executions>
         <execution>
             <goals>
                <goal>install</goal>
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
         <appId>QWER-ASDF-ZXCV-ERTY</appId>
         <serverName>jenkins.slave1</serverName>
         <minSeverity>High</minSeverity>
     </configuration>
</plugin>
```