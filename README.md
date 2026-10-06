<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/docker-proxy-rule"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

[![build status](https://circleci.com/gh/palantir/docker-proxy-rule.svg?syle=shield)](https://circleci.com/gh/palantir/docker-proxy-rule)

Docker Proxy JUnit Rule
=======================

This is a small library for executing JUnit tests that interact with Docker containers. It supports the following:

 - Hitting the docker containers according the their hostnames when using interfaces that are not backed by Java NIO
 - Auto-mapping the hostnames when using docker-compose-rule
 - Auto-mapping the hostnames when specifying the name of the network they are on

## 🐳 Containerization Support

This project now includes full containerization support with Docker and Docker Compose for development, testing, and production environments.

### Quick Start with Docker

```bash
# Build the Docker image
docker build -t palantir/docker-proxy-rule .

# Run with Docker Compose (includes monitoring stack)
docker-compose up -d

# Access services
# - Proxy: localhost:1080
# - Test Web Server: localhost:8081
# - Prometheus: localhost:9090
# - Grafana: localhost:3000 (admin/admin)
```

### Development Environment

The Docker Compose setup includes:
- **Main Application**: Docker Proxy Rule with SOCKS proxy on port 1080
- **Test Services**: Nginx web server, PostgreSQL database, Redis cache
- **Monitoring Stack**: Prometheus metrics collection and Grafana dashboards
- **Health Checks**: Automated health monitoring for all services

Why should I use this
---------------------

This code allows you to avoid having to map internal docker ports to external ports so you don't have to map them to ports that may be in-use, or map them to random ports then have logic to construct clients based on which random port is being used.

Simple Use
----------

Add a dependency to your project. For example, in gradle:

```groovy
repositories {
    mavenCentral() // docker-proxy-rule is published on maven central
}
dependencies {
    testImplementation 'com.palantir.docker.proxy:docker-proxy-rule:<latest-tag>'
}
```

For the most basic use (with [docker-compose-rule](https://github.com/palantir/docker-compose-rule)), simply add an `@ClassRule` as follows:

```java
public class MyIntegrationTest {
    private static DockerComposeRule docker = ...;
    private static DockerProxyRule proxy = DockerProxyRule.fromProjectName(docker.projectName());

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(docker)
            .around(proxy);
}
```

You can then communicate with the hosts within your tests. For example:
```java
URLConnection urlConnection = new URL(TARGET).openConnection();
urlConnection.connect();
```

## 🚀 Container Features

### Multi-stage Build
- **Build Stage**: Uses Gradle with JDK 17 for compilation
- **Runtime Stage**: Uses Google Distroless for minimal attack surface
- **Optimization**: Layered caching for faster builds

### Security Features
- **Non-root User**: Runs as `nonroot:nonroot` user
- **Distroless Base**: Minimal runtime environment
- **Security Scanning**: Ready for vulnerability scanning tools

### Monitoring & Observability
- **Prometheus Metrics**: Application and JVM metrics
- **Grafana Dashboards**: Pre-configured monitoring dashboards
- **Health Checks**: Kubernetes-ready health endpoints
- **Structured Logging**: JSON-formatted logs for centralized logging

## 📁 Project Structure

```
├── Dockerfile                 # Multi-stage container build
├── docker-compose.yml         # Complete development stack
├── .dockerignore              # Docker build optimization
├── monitoring/                # Monitoring configuration
│   ├── prometheus.yml         # Metrics collection config
│   └── grafana/              # Dashboard configuration
└── test-data/                # Test environment setup
    ├── nginx.conf            # Test web server config
    ├── init.sql              # Database initialization
    └── html/                 # Test web content
```

## 🛠️ Development

### Prerequisites
- Docker and Docker Compose
- Java 17+ (for local development)
- Gradle 8+ (for local builds)

### Local Development
```bash
# Clone the repository
git clone https://github.com/palantir/docker-proxy-rule.git
cd docker-proxy-rule

# Create feature branch
git checkout -b feature/your-feature-name

# Start development environment
docker-compose up -d

# Run tests
./gradlew test

# Build application
./gradlew build
```

### Testing the Proxy
```bash
# Test SOCKS proxy connection
curl --socks5 localhost:1080 http://test-web-server/health

# Check application health
curl http://localhost:8080/health

# View metrics
curl http://localhost:8080/actuator/prometheus
```

## 📊 Monitoring

Access the monitoring stack:
- **Prometheus**: http://localhost:9090 - Metrics collection and querying
- **Grafana**: http://localhost:3000 - Dashboards and visualization (admin/admin)

## 🤝 Contributing

1. Create a feature branch from `develop`
2. Make your changes with appropriate tests
3. Ensure all containers build and run successfully
4. Update documentation as needed
5. Submit a pull request

## 📝 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
