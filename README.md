# cc-java-sdk

The Java sdk for developing plugins for cloud compute. This library provides tools for interacting with S3 and managing and configuring plugins.

## Getting Started

### Requirements

- [Docker](https://docs.docker.com/get-docker/)
- Gradle
- AWS S3 (or minio running in local network)

### Building the System

This library is built with gradle, using the following command:

```
gradle build
```

This will create a `cc-java-sdk-0.0.50.jar` file inside the `build/libs` directory.

Optionally, this library can be opened for development inside a Docker dev container. The `devcontainer.json` specifies configuration for the development container. Currently, the dev container is set to build from the existing [`Dockerfile`](Dockerfile)

### Running the System

This library does not have a main class/method for itself, and is meant to be incorporated into other libraries that do. Ensure that the environmental variables inside the .env-example are present for this library.
