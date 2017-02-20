rouplex-benchmark-service
=======

# README #
Instrumentation service using rouplex-platform and for managing rouplex-platfom components (such as configuration or
starting/stopping of services) as well as orchestration of complex scenarios for needs of testing and/or benchmarking.

## What is this repository for? ##
This repo provides an J2EE web application (or service), which can be deployed in the various application containers
such as tomcat.

### Description ###
This service will be used for pushing the various configurations for testing and/or benchmarking of rouplex-platform
services and components.

### Versioning ###
We use semantic versioning, int its representation x.y.z, z stands for API, y for dependencies, and z for build number.

## How do I get set up? ##

### Build ###

1. Maven is required to build the project. Please download and install it. Make sure the installation is successful by
typing `mvn -version` in a shell window; the command output should be showing the installation folder.

1. Java is required to build the project.. Make sure the installation is successful by typing `java -version`;
the command output should show the version.

1. On a shell window, and from the folder containing this README.txt file, type `mvn clean install` and if
successful, you will have the built artifacts in appropriate 'target' folders located under the appropriate modules.
The same jars will be installed in your local maven repo.

### Test ###
`mvn test`

### Run ###
1. Tomcat or similar application container is required to run the project's webapp. Please download and install it.
1. Deploy the webapp by uploading it in tomcat
1. Use the browser to get to url (coming soon)

## Contribution guidelines ##

* Writing tests
* Code review
* Other guidelines

## Who do I talk to? ##

* Repo owner or admin
andimullaraj@gmail.com