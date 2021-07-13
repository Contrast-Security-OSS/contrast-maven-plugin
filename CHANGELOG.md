# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)

## [Unreleased]
### Added
- Contrast Scan support

### Removed
- `profile` configuration which the Contrast server has not supported since before 3.7.7


## [2.12] - 2021-03-09
### Changed
- Builds with JDK 8, 11, and 15
- Targets JRE 7
- Maven version > 3.6.1 (Released April 2019) is required to build the plugin


## [2.0] - 2018-05-15
### Added
- Vulnerabilities now reconciled using an app version instead of a timestamp
- App version can be generated using `$TRAVIS_BUILD_NUMBER` or `$CIRCLE_BUILD_NUM`
- Source packaging changed to `com.contrastsecurity.maven.plugin`