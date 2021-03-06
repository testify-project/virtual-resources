# Change Log
All notable changes to this project will be documented in this file. This project
adheres to [Semantic Versioning](http://semver.org/). The change log file consists
of sections listing each version and the date they were released along with what
was added, changed, deprecated, removed, fix and security fixes.

- Added - Lists new features
- Changed - Lists changes in existing functionality
- Deprecated -  Lists once-stable features that will be removed in upcoming releases
- Removed - Lists deprecated features removed in this release
- Fixed - Lists any bug fixes
- Security - Lists security fixes to security vulnerabilities

## [Unreleased]

## [1.0.2] - 2018-01-03
### Changed
- Updated Testify Version

## [1.0.1] - 2017-12-19
### Fixed
- Insured that `virtual-resources-bom` dependencies are test scoped.

## [1.0.0] - 2017-12-18
### Added
- Added virtual resources BOM module

### Changed
- Updated Testify Dependency to version 1.0.0

## [0.9.5] - 2017-09-15
### Changed
Updated versions of Testify and Build-Tools dependencies

## [0.9.4] - 2017-08-20
### Fixed
- Fixed issue with containers not being stopped and removed

## [0.9.3] - 2017-07-29
### Added
- Support for:
 - Clustering virtual resources
 - Linking virtual resources
 - Passing environmental variables to virtual resources
 - Waiting for specific ports

### Changed 
- Updated Testify API to version 0.9.7

## [0.9.2] - 2017-07-16
### Changed 
- Updated fully qualified of VirtualResourceInstance from just image name to image name plus image tag (i.e. `postgres:9.4`)

## [0.9.1] - 2017-07-16
### Changed 
- Updated Testify API to version 0.9.6
- Added VirtualResourceInstace parameter to VirtualResourceProvider#stop method

## [0.9.0] - 2017-06-06
### Added
- Virtual Resources parent project
- Docker based virtual resource provider implementation