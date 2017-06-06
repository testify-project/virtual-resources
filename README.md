# Resources
[![Build Status](https://travis-ci.org/testify-project/virtual-resources.svg?branch=develop)](https://travis-ci.org/testify-project/virtual-resources)
[![Join the chat on Gitter](https://badges.gitter.im/testify-project/Lobby.svg)](https://gitter.im/testify-project/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![CodecovIO](https://codecov.io/github/testify-project/virtual-resources/coverage.svg?branch=develop)](https://codecov.io/github/testify-project/virtual-resources?branch=develop)
[![Maven Central](https://img.shields.io/maven-central/v/org.testifyproject.resources/parent.svg)]()
[![License](https://img.shields.io/github/license/testify-project/virtual-resources.svg)](LICENSE)

## Overview
A repository that contains reusable collection of virtual resource provider implementations.

## Issue Tracking
Report issues via the [Github Issues][github-issues]. Think you've found a bug?
Please consider submitting a reproduction project via the a new [Github Issue][github-issues-new].

## Building from Source
Testify uses a Maven-based build system. To build from source follow the bellow instructions:

### Prerequisites
- [Git 1.9.1](https://git-scm.com/downloads) or above
- [JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html) (be sure to set `JAVA_HOME`)
- [Maven 3.1.1](https://maven.apache.org/download.cgi) or above

### Check out sources
```
git clone git@github.com:testify-project/virtual-resources.git
```

or

```
git clone https://github.com/testify-project/virtual-resources.git
```

### Install all Testify jars into your local Maven cache
```
./mvnw install -Dmaven.test.skip
```

### Compile and test and build all jars
```
./mvnw clean install
```

## Staying in Touch
Hit us up on [Gitter][gitter].

## License
The Testify is released under [Apache Software License, Version 2.0](LICENSE).

Enjoy and keep on Testifying!

[github-issues]: https://github.com/testify-project/virtual-resources/issues
[github-issues-new]: https://github.com/testify-project/virtual-resources/issues/new
[gitter]: https://gitter.im/testify-project/Lobby