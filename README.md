# Contrast Maven Plugin

Repository for the Contrast Maven plugin. This plugin will download and install the Contrast Java agent pre-integration tests.
Then it will verify no new vulnerabilities were found in the post-integration test lifecycle phase.

Setting '-Djavaagent:contrast.jar' is required.

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
         <minSeverity>High</minSeverity>
         <jarPath>~/.contrast/contrast.jar</jarPath>
     </configuration>
</plugin>
```