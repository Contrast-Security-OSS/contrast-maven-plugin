# Contrast Maven Plugin

## Version 2.0

This document refers to version 2.0 of the Contrast Maven Plugin. Behavior has changed a bit since the latest 1.X release.

New in 2.X:

* Vulnerabilities now reconciled using an app version instead of a timestamp
* App version can be generated using $TRAVIS_BUILD_NUMBER or $CIRCLE_BUILD_NUM
* Source packaging changed to com.contrastsecurity.maven.plugin

## Documentation

Always refer to Contrast's Open Docs site for the most up to date documentation: https://docs.contrastsecurity.com/tools-build.html#maven

## Usage

This Maven plugin can be used to allow Contrast to discover vulnerabilities in your application during your integration or verification tests. 

The `install` goal of the plugin is used to download the agent to the /target directory.

The plugin will edit maven's `argLine` property to launch the JVM with the Contrast agent.

In the `verify` phase, the plugin will check if any new vulnerabilities were discovered during the test phases. The build will fail if any serious vulnerabilities are discovered.

## Goals

* `install`: installs a Contrast Java agent to your local project
* `verify`: checks for new vulnerabilities in your web application

## Configuration Options

| Parameter   | Required | Default    | Description                                                                       |
|-------------|----------|------------|-----------------------------------------------------------------------------------|
| username    | True     |            | Username in TeamServer                                                            |
| serviceKey  | True     |            | Service Key found in Organization Settings page                                   |
| apiKey      | True     |            | API Key found in Organization Settings page                                       |
| orgUuid     | True     |            | Organization UUID found in Organization Settings page                             |
| appName     | True     |            | Name of the application as seen in the Contrast site                              |
| appVersion  | False    | See below  | The appversion to report to TeamServer. See explanation below.                    |
| apiUrl      | True     |            | API URL to your TeamServer instance                                               |
| serverName  | True     |            | Name of the server you set with -Dcontrast.server                                 |
| minSeverity | False    | Medium     | Minimum severity level to verify (can be Note, Low, Medium, High or Critical      |
| jarPath     | False    |            | Path to contrast.jar if you already have one downloaded                           |
| skipArgLine | False    | False      | If this is "true", the plugin will not alter the Maven argLine property in any way|

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

## appVersion

When your app's integration tests are run, the Contrast agent can add an app version to its metadata so that vulnerabilites can be compared between app versions, CI builds, etc...

We generate this app version as follows and in this order:

* If you specify an appVersion in the properties, we'll use that without modification
* If your build is running in TravisCI, we'll use appName-$TRAVIS_BUILD_NUMBER
* If your build is running in CircleCI, we'll use appName-$CIRCLE_BUILD_NUM
* If no appVersion is specified, we'll generate one in the following format: appName-yyyyMMddHHmmss
