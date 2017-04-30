# Status
![Build Status](https://codeship.com/projects/fa99f2c0-c34d-0133-4e04-26a4c37f4e5a/status?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mtakaki/CredentialStorageService/badge.svg?branch=master)](https://coveralls.io/github/mtakaki/CredentialStorageService?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/5d417f9c2dfc45b69ee4cd33552f4c5a)](https://www.codacy.com/app/mitsuotakaki/CredentialStorageService)
[![Download](https://maven-badges.herokuapp.com/maven-central/com.github.mtakaki/credential-storage/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.mtakaki/credential-storage)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.mtakaki/credential-storage/badge.svg)](http://www.javadoc.io/doc/com.github.mtakaki/credential-storage)

CredentialStorageService
========================
A RESTful Java micro-service that can be used to store resource credentials in a central location. It uses Java 8, [dropwizard](http://www.dropwizard.io), [lombok](https://projectlombok.org/), and RSA/AES encryption algorithms.

Its main purpose is to remove the need to store credential in code or configuration files and, instead, have the clients retrieve the credentials it needs from a centralized server. It also facilitates credential rotation, as clients can periodically retrieve the credentials from the server without downtime.

The data is encrypted using a random AES symmetric key and this key is encrypted using the client's RSA public key. So, the data can only be retrieved using the private key, which resides in the client. The server doesn't have the ability to decrypt its own information, so even if the data is stolen, it can't be decrypted without the private key.

There's a test sandbox server running the service available at: [https://credential-service.herokuapp.com/swagger](https://credential-service.herokuapp.com/swagger) Please keep in mind this is a test server and it may not be up all the time and it may not be secured.

Integrations:

- [Dropwizard hibernate bundle](https://github.com/mtakaki/CredentialStorageService-dw-hibernate)
    - Automatically retrieves database credentials and polls for credentials changes. Allows database credentials rotation in production without downtime.

# Setting up

The server uses Redis as it's storage backend. It's recommended to enable password to your Redis instance, to guarantee it's not easily accessible.

It is recommended enabling TLS, so all traffic is encrypted. You can use [let's encrypt](https://letsencrypt.org/) to create free certificates.

The client will require RSA certificates, but only the public key will be sent to the server. The public key should be converted into DER format. The private key should never be sent to the server.

As the service relies on HTTP protocol, it can be easily load-balanced (through HAProxy, for e.g.) to provide high-availability.

## Converting RSA key to DER

```
$ openssl rsa -in id_rsa -out public_key.der -outform DER -pubout
```

## Building the project

In order to build this project and have it running you will need:

1. JDK 8
1. JCE (in case you want to use AES with key size of 256)
1. Maven

After installing those dependencies:

```
$ mvn package
```

You will find the following files in the `target` folder:

```
$ ls target/
archive-tmp                                      credential-storage-0.0.3-SNAPSHOT.jar            maven-archiver                                   surefire-reports
bundled-credential-storage-0.0.3-SNAPSHOT.tar.gz generated-sources                                maven-status                                     test-classes
classes                                          generated-test-sources                           original-credential-storage-0.0.3-SNAPSHOT.jar
```

The `credential-storage-0.0.3-SNAPSHOT.jar` is a runnable JAR file. The `bundled-credential-storage-0.0.3-SNAPSHOT.tar.gz` is a tarball with the runnable JAR and the `config.yml` available at `src/main/resources`.

# Starting the server

Using the runnable jar file:

```
$ java -jar credential-storage-0.0.3-SNAPSHOT.jar server config.yml

INFO  [2017-04-23 04:06:27,197] org.eclipse.jetty.util.log: Logging initialized @3666ms to org.eclipse.jetty.util.log.Slf4jLog
INFO  [2017-04-23 04:06:27,525] io.dropwizard.server.DefaultServerFactory: Registering jersey handler with root path prefix: /
INFO  [2017-04-23 04:06:27,530] io.dropwizard.server.DefaultServerFactory: Registering admin handler with root path prefix: /
INFO  [2017-04-23 04:06:29,083] io.dropwizard.assets.AssetsBundle: Registering AssetBundle with name: swagger-assets for path /swagger-static/*
INFO  [2017-04-23 04:06:29,159] io.dropwizard.assets.AssetsBundle: Registering AssetBundle with name: swagger-oauth2-connect for path /o2c.html/*
INFO  [2017-04-23 04:06:29,423] org.reflections.Reflections: Reflections took 145 ms to scan 1 urls, producing 36 keys and 42 values
INFO  [2017-04-23 04:06:29,730] io.dropwizard.server.DefaultServerFactory: Registering jersey handler with root path prefix: /
INFO  [2017-04-23 04:06:29,730] io.dropwizard.server.DefaultServerFactory: Registering admin handler with root path prefix: /
INFO  [2017-04-23 04:06:29,744] io.dropwizard.server.ServerFactory: Starting credential-storage-service
   ___             _            _   _       _   __                 _          
  / __\ __ ___  __| | ___ _ __ | |_(_) __ _| | / _\ ___ _ ____   _(_) ___ ___
 / / | '__/ _ \/ _` |/ _ \ '_ \| __| |/ _` | | \ \ / _ \ '__\ \ / / |/ __/ _ \
/ /__| | |  __/ (_| |  __/ | | | |_| | (_| | | _\ \  __/ |   \ V /| | (_|  __/
\____/_|  \___|\__,_|\___|_| |_|\__|_|\__,_|_| \__/\___|_|    \_/ |_|\___\___|


INFO  [2017-04-23 04:06:29,993] org.eclipse.jetty.setuid.SetUIDListener: Opened application@46e03c31{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
INFO  [2017-04-23 04:06:29,993] org.eclipse.jetty.setuid.SetUIDListener: Opened admin@24cf1abf{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [2017-04-23 04:06:29,996] org.eclipse.jetty.server.Server: jetty-9.4.2.v20170220
INFO  [2017-04-23 04:06:30,979] io.dropwizard.jersey.DropwizardResourceConfig: The following paths were found for the configured resources:

    DELETE  /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    GET     /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    POST    /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    PUT     /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    GET     /swagger (io.federecio.dropwizard.swagger.SwaggerResource)
    GET     /swagger.{type:json|yaml} (io.swagger.jaxrs.listing.ApiListingResource)

INFO  [2017-04-23 04:06:31,003] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@25af233d{/,null,AVAILABLE}
INFO  [2017-04-23 04:06:31,013] io.dropwizard.setup.AdminEnvironment: tasks =

    POST    /tasks/log-level (io.dropwizard.servlets.tasks.LogConfigurationTask)
    POST    /tasks/gc (io.dropwizard.servlets.tasks.GarbageCollectionTask)

INFO  [2017-04-23 04:06:31,206] io.dropwizard.jersey.DropwizardResourceConfig: Registering admin resources
The following paths were found for the configured resources:

    GET     /audit (com.github.mtakaki.credentialstorage.resources.admin.AuditResource)
    GET     /audit/last_accessed (com.github.mtakaki.credentialstorage.resources.admin.AuditResource)

INFO  [2017-04-23 04:06:31,207] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@7fcbb24b{/,null,AVAILABLE}
INFO  [2017-04-23 04:06:31,234] org.eclipse.jetty.server.AbstractConnector: Started application@46e03c31{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
INFO  [2017-04-23 04:06:31,238] org.eclipse.jetty.server.AbstractConnector: Started admin@24cf1abf{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [2017-04-23 04:06:31,239] org.eclipse.jetty.server.Server: Started @7709ms
```

# APIs

## Data model

There is only one data representation of the credential pair:

```json
{
  "symmetric_key": "base64key",
  "primary": "encrypted using symmetric_key",
  "secondary": "encrypted using symmetric_key",
  "description": "optional description",
  "last_access": "2017-04-22T06:57:52.843",
  "created_at": "2017-04-21T03:39:05.190",
  "updated_at": "2017-04-21T08:48:35.845"
}
```

The `symmetric_key` is an AES symmetric key used to encrypt the `primary` and `secondary` attributes. The symmetric key is encrypted using the client's public key and, in order to decrypt the credentials, you will need to do the following operations in this order:

1. Decode the symmetric key from base64.
1. Use the client's private key to decrypt the symmetric key (RSA).
1. Use the decrypted symmetric key to decrypt the credentials (AES).

## Operations

All APIs require the client's public key (encoded in base64 format) in a custom header `X-Auth-RSA`. The key is an unique key and it's used to retrieve the credentials from the underlying database. As the public key is used as the key, there can only be one credential pair stored per public key.

The service includes the 4 basic CRUD operations.

### Get
**Request**

```
GET /credential
X-Auth-RSA: base64 encoded RSA public key
```

**Response**

```
200 OK
Content-Type: application/json
Payload:
{
  "symmetric_key": "encrypted key",
  "primary": "encrypted",
  "secondary": "encrypted"
}
```

### Post

This operation stores a new credential pair or completely overrides an existing one.

**Request**

```
POST /credential
X-Auth-RSA: base64 encoded RSA public key
Content-Type: application/json
Payload:
{
  "primary": "encrypted",
  "secondary": "encrypted",
  "description": "this entry description"
}
```

**Response**

```
201 Created
```

### Put
An idempotent operation, that will save a new credential pair or update an existing one.

**Request**

```
PUT /credential
X-Auth-RSA: base64 encoded RSA public key
Content-Type: application/json
Payload:
{
  "symmetric_key": "key",
  "primary": "encrypted",
  "secondary": "encrypted"
}
```

**Response**

```
200 OK
```

### Delete
**Request**

```
DELETE /credential
X-Auth-RSA: base64 encoded RSA public key
```

**Response**

```
200 OK
```

# Performance

These performance metrics were calculated using a high latency redis service, using 30 concurrent clients, and 2000 requests in total. It was running on eclipse on a small laptop, which doesn't provide the best output, and a remote redis service that added extra milliseconds to the requests. The numbers shows it's capable of handling more than **100 requests per second** on the `GET` operation. The credential update is not performed as often as a retrieval so its performance is not as important as the retrieval.

## Retrieving credentials
```
Document Path:          /credential
Document Length:        780 bytes

Concurrency Level:      30
Time taken for tests:   14.659 seconds
Complete requests:      2000
Failed requests:        0
Total transferred:      1824000 bytes
HTML transferred:       1560000 bytes
Requests per second:    136.43 [#/sec] (mean)
Time per request:       219.891 [ms] (mean)
Time per request:       7.330 [ms] (mean, across all concurrent requests)
Transfer rate:          121.51 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.3      0       4
Processing:    63  218  35.7    214     499
Waiting:       63  217  35.7    214     498
Total:         63  218  35.7    215     499

Percentage of the requests served within a certain time (ms)
  50%    215
  66%    221
  75%    226
  80%    230
  90%    245
  95%    265
  98%    335
  99%    352
 100%    499 (longest request)
```

## Uploading credentials
```
Document Path:          /credential
Document Length:        0 bytes

Concurrency Level:      30
Time taken for tests:   22.517 seconds
Complete requests:      2000
Failed requests:        0
Total transferred:      1684000 bytes
Total body sent:        1952000
HTML transferred:       0 bytes
Requests per second:    88.82 [#/sec] (mean)
Time per request:       337.748 [ms] (mean)
Time per request:       11.258 [ms] (mean, across all concurrent requests)
Transfer rate:          73.04 [Kbytes/sec] received
                        84.66 kb/s sent
                        157.70 kb/s total

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.4      0       5
Processing:   104  336  86.4    324     959
Waiting:      104  335  86.4    324     959
Total:        104  336  86.7    324     961

Percentage of the requests served within a certain time (ms)
  50%    324
  66%    334
  75%    342
  80%    349
  90%    385
  95%    446
  98%    522
  99%    877
 100%    961 (longest request)
```

# Next steps

1. Create more integrations.
1. Add more audit APIs.
1. Create administration UI.

# Docker

This service is also available at docker: https://hub.docker.com/r/mtakaki/credential-storage-service/
