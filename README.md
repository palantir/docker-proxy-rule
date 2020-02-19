<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/docker-proxy-rule"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

[![build status](https://circleci.com/gh/palantir/docker-proxy-rule.svg?syle=shield)](https://circleci.com/gh/palantir/docker-proxy-rule) [![Download](https://api.bintray.com/packages/palantir/releases/docker-proxy-rule/images/download.svg) ](https://bintray.com/palantir/releases/docker-proxy-rule/_latestVersion)

Docker Proxy JUnit Rule
=======================

This is a small library for executing JUnit tests that interact with Docker containers. It supports the following:

 - Hitting the docker containers according the their hostnames when using interfaces that are not backed by Java NIO
 - Auto-mapping the hostnames when using docker-compose-rule
 - Auto-mapping the hostnames when specifying the name of the network they are on

Why should I use this
---------------------

This code allows you to avoid having to map internal docker ports to external ports so you don't have to map them to ports that may be in-use, or map them to random ports then have logic to construct clients based on which random port is being used.

Simple Use
----------

Add a dependency to your project. For example, in gradle:

```groovy
repositories {
    maven {
        url 'https://dl.bintray.com/palantir/releases' // docker-proxy-rule is published on bintray
    }
}
dependencies {
    compile 'com.palantir.docker.proxy:docker-proxy-rule:<latest-tag-from-bintray>'
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
URLConnection urlConnection = new URL("http://webserver").openConnection();
urlConnection.connect();
```
