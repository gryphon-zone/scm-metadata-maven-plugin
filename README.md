# scm-metadata-maven-plugin

[![License][documentation_badge]][documentation_url]
[![License][license_badge]][license_url]
[![Maven Central][central_badge]][central_link]
[![Build Status][build_badge]][build_link]

[documentation_badge]: https://img.shields.io/static/v1?label=documentation&message=Github%20Pages&color=informational
[documentation_url]: https://gryphon-zone.github.io/scm-metadata-maven-plugin/plugin-info

[build_badge]: https://jenkins.gryphon.zone/buildStatus/icon?job=gryphon-zone%2Fscm-metadata-maven-plugin%2Fmaster
[build_link]: https://jenkins.gryphon.zone/view/master%20builds/job/gryphon-zone/job/scm-metadata-maven-plugin/job/master/

[central_badge]: https://img.shields.io/maven-central/v/zone.gryphon.maven.plugins/scm-metadata-maven-plugin
[central_link]: https://search.maven.org/search?q=g:%22zone.gryphon.maven.plugins%22%20AND%20a:%22scm-metadata-maven-plugin%22

[license_badge]: https://img.shields.io/github/license/gryphon-zone/scm-metadata-maven-plugin
[license_url]: http://www.apache.org/licenses/LICENSE-2.0

Maven plugin to inject metadata about the SCM in use for the project into your build as Maven properties, for re-use by other plugins.

Similar in spirit to the [Buildnumber Maven Plugin](https://www.mojohaus.org/buildnumber-maven-plugin/), with the following key differences:
1. Focus specifically on SCM metadata
1. Richer set of metadata included
1. Greater control over injected properties

See the [documentation][documentation_url] for details.

## SCM support

Currently supported SCM implementations:
* `git`

PRs adding support for additional SCM providers are welcome,
the eventual goal is to support all of the most commonly used SCMs
[supported by Maven itself](https://maven.apache.org/scm/scms-overview.html)

## Developer Information

#### Project Requirements
Building the plugin requires a Java 9+ JDK (although the project is compiled against the Java 7 SDK).

#### Building

To build the project:
```shell script
mvn clean install
```

To build the plugin documentation:
```shell script
mvn clean site
```

For compatibility with `github-pages`, the documentation is deployed into the [docs](docs) folder.
To build the documentation and deploy it into this folder, run
```shell script
mvn clean site-deploy
```

#### IDE integration

This project utilizes [Project Lombok](https://projectlombok.org/), so ensure you add the appropriate plugins to your IDE.

## License
```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
