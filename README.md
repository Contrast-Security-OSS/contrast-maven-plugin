# Contrast Maven Plugin

## Version 2

This document refers to version 2.X of the Contrast Maven Plugin. Behavior has changed a bit since the latest 1.X release.

New in 2.X:

* Vulnerabilities now reconciled using an app version instead of a timestamp
* App version can be generated using $TRAVIS_BUILD_NUMBER or $CIRCLE_BUILD_NUM
* Source packaging changed to com.contrastsecurity.maven.plugin

## An Example

We have a working example up on a Spring Boot application here: https://github.com/Contrast-Security-OSS/vulnerable-spring-boot-application

## Documentation

Always refer to Contrast's Open Docs site for the most up to date documentation: https://docs.contrastsecurity.com/tools-build.html#maven

## Usage

This Maven plugin can be used to allow Contrast to discover vulnerabilities in your application during your integration or verification tests. 

The `install` goal of the plugin is used to download the agent to the /target directory.

The plugin will edit maven's `argLine` property to launch the JVM with the Contrast agent. If the Spring Boot Maven Plugin is detected, the `run.jvmArguments` property will also be edited to load Contrast.

In the `verify` phase, the plugin will check if any new vulnerabilities were discovered during the test phases. The build will fail if any serious vulnerabilities are discovered.

## Goals

* `install`: installs a Contrast Java agent to your local project
* `verify`: checks for new vulnerabilities in your web application

## Configuration Options

| Parameter   | Required | Default    | Description                                                                       | Since |
|-------------|----------|------------|-----------------------------------------------------------------------------------|-------|
| username    | True     |            | Contrast username                                                            |       |
| serviceKey  | True     |            | Service Key found in Your Account => Profile page => Your keys => Personal Keys                                   |       |
| apiKey      | True     |            | Api Key found in Your Account => Profile page => Your keys => Organization Keys                                       |       |
| orgUuid     | True     |            | Organization Uuid found in Your Account => Profile page => Your keys => Organization Keys                             |       |
| appName     | False    |            | Name of the application as seen in the Contrast site                              |       |
| appId       | False    |            | Id of the application as seen in the Contrast site. Either appId or appName is required. If both are specified, we'll use appId and ignore appName | 2.5 |
| standalone  | False    | False      | Set this to true if this is a standalone app                                    |    2.2|
| appVersion  | False    | See below  | The appversion to report to Contrast. See explanation below.                    |       |
| applicationSessionMetadata | False | | Comma-separated name=value pairs for your application session metadata | 2.9 |
| applicationTags            | False | | Comma-separated values to tag the application | 2.9 |
| environment | False | "QA" | The name of the environment (Development/QA/Production | 2.9 |
| apiUrl      | True     |            | API URL to your Contrast instance found in Your Account => Profile page => Your keys => Organization Keys                                              |       |
| serverName  | True     |            | Name of the server you set with -Dcontrast.server                                 |       |
| serverPath  | False    |            | The server context path                                                           |    2.1|
| minSeverity | False    | Medium     | Minimum severity level to verify (can be Note, Low, Medium, High or Critical)     |       |
| jarPath     | False    |            | Path to contrast.jar if you already have one downloaded                           |       |
| skipArgLine | False    | False      | If this is "true", the plugin will not alter the Maven argLine or run.jvmArguments properties in any way|    2.0|
| profile     | False    |            | If this is set, the plugin will use the specified custom agent profile when downloading contrast.jar|    2.4|
| useProxy   | False    | False      | Override proxy settings from maven settings.xml | 2.8|
| proxyHost  | False    |            | Required if useProxy is true. The host of the proxy | 2.8|
| proxyPort  | False    |            | Required if useProxy is true. The port of the proxy | 2.8|


## Option Details

### serverPath

Multi-module Maven builds can appear as different servers in the Contrast UI. If you would like to discourage this behavior and would rather see all modules appear under the same server in Contrast, then please set the `serverPath` property.

**You are strongly encouraged to add a serverPath if this build will be run in a CI environment such as Travis CI or Circle CI.** This will help with keeping your servers tidy in the Contrast UI. 

### appVersion

When your app's integration tests are run, the Contrast agent can add an app version to its metadata so that vulnerabilites can be compared between app versions, CI builds, etc...

We generate the app version as follows and in this order:

* If you specify an appVersion in the properties, we'll use that without modification.
* If your build is running in TravisCI, we'll use appName-$TRAVIS_BUILD_NUMBER.
* If your build is running in CircleCI, we'll use appName-$CIRCLE_BUILD_NUM.
* If no appVersion is specified, we'll generate one in the following format: appName-yyyyMMddHHmmss.

## Example Configuration

```xml
<plugin>
     <groupId>com.contrastsecurity</groupId>
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
         <apiUrl>https://app.contrastsecurity.com/Contrast/api</apiUrl>
         <orgUuid>ORG_UID_HERE</orgUuid>
         <appName>Test Application</appName>
         <appId>bc3028e6-82ac-410f-b9c7-13573d33cb94</appId>
         <serverName>jenkins.server</serverName>
         <minSeverity>High</minSeverity>
         <useProxy>true</useProxy>
         <proxyHost>localhost</proxyHost>
         <proxyPort>8060</proxyPort>
         <environment>Development</environment>
         <applicationTags>${user.name},spring</applicationTags>
         <applicationSessionMetadata>version=${build.number}</applicationSessionMetadata>
     </configuration>
</plugin>
```
