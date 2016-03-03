# Status
![Build Status](https://codeship.com/projects/fa99f2c0-c34d-0133-4e04-26a4c37f4e5a/status?branch=master)
[![Coverage Status](https://coveralls.io/repos/mtakaki/CredentialStorageService/badge.svg?branch=master&service=github)](https://coveralls.io/github/mtakaki/CredentialStorageService?branch=master)

CredentialStorageService
========================
A RESTful Java micro-service that can be used to store resource credentials in a central location. It uses Java 8, [dropwizard](http://www.dropwizard.io), and RSA/AES encryption algorithms.

Its main purpose is to remove the need to store credential in code or configuration files and, instead, have the clients retrieve the credentials it needs from a centralized server. It also facilitates credential rotation, as clients can periodically retrieve the credentials from the server without downtime.

The data is encrypted using a random AES symmetrical key and this key is encrypted using the client's RSA public key. So the data can only be retrieved using the private key, which resides in the client.

# Setting up

The server is currently setup to use an in-memory database, HSQLDB, and it can be setup to persist the data in the disk. If you choose to use it in-memory only, you will lose the data if the server crashes or when you restart it.

It is recommended enabling TLS, so all traffic is encrypted. You can use [let's encrypt](https://letsencrypt.org/) to create free certificates.

The client will require RSA certificates, but only the public key will be sent to the server. The public key should be converted into DER format. The private key should never be sent to the server.

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
archive-tmp                                      credential-storage-0.0.1-SNAPSHOT.jar            maven-archiver                                   surefire-reports
bundled-credential-storage-0.0.1-SNAPSHOT.tar.gz generated-sources                                maven-status                                     test-classes
classes                                          generated-test-sources                           original-credential-storage-0.0.1-SNAPSHOT.jar
```

The `credential-storage-0.0.1-SNAPSHOT.jar` is a runnable JAR file. The `bundled-credential-storage-0.0.1-SNAPSHOT.tar.gz` is a tarball with the runnable JAR and the `config.yml` available at `src/main/resources`.

# Starting the server

Using the runnable jar file:

```
$ java -jar credential-storage-0.0.1-SNAPSHOT.jar server config.yml

INFO  [2016-03-03 08:32:59,648] io.dropwizard.assets.AssetsBundle: Registering AssetBundle with name: swagger-assets for path /swagger-static/*
INFO  [2016-03-03 08:33:01,708] org.reflections.Reflections: Reflections took 1996 ms to scan 1 urls, producing 20416 keys and 38756 values
INFO  [2016-03-03 08:33:01,869] io.dropwizard.server.ServerFactory: Starting credential-storage-service
   ___             _            _   _       _   __                 _
  / __\ __ ___  __| | ___ _ __ | |_(_) __ _| | / _\ ___ _ ____   _(_) ___ ___
 / / | '__/ _ \/ _` |/ _ \ '_ \| __| |/ _` | | \ \ / _ \ '__\ \ / / |/ __/ _ \
/ /__| | |  __/ (_| |  __/ | | | |_| | (_| | | _\ \  __/ |   \ V /| | (_|  __/
\____/_|  \___|\__,_|\___|_| |_|\__|_|\__,_|_| \__/\___|_|    \_/ |_|\___\___|


INFO  [2016-03-03 08:33:01,879] io.dropwizard.server.DefaultServerFactory: Registering jersey handler with root path prefix: /
INFO  [2016-03-03 08:33:01,894] io.dropwizard.server.DefaultServerFactory: Registering admin handler with root path prefix: /
INFO  [2016-03-03 08:33:02,003] org.eclipse.jetty.setuid.SetUIDListener: Opened application@2dd828c{HTTP/1.1}{0.0.0.0:8080}
INFO  [2016-03-03 08:33:02,003] org.eclipse.jetty.setuid.SetUIDListener: Opened admin@68f68a1a{HTTP/1.1}{0.0.0.0:8081}
INFO  [2016-03-03 08:33:02,008] org.eclipse.jetty.server.Server: jetty-9.2.z-SNAPSHOT
INFO  [2016-03-03 08:33:02,646] io.dropwizard.jersey.DropwizardResourceConfig: The following paths were found for the configured resources:

    DELETE  /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    GET     /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    POST    /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    PUT     /credential (com.github.mtakaki.credentialstorage.resources.CredentialResource)
    GET     /swagger (io.swagger.jaxrs.listing.ApiListingResource)
    GET     /swagger.{type:json|yaml} (io.swagger.jaxrs.listing.ApiListingResource)

INFO  [2016-03-03 08:33:02,648] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@6788303d{/,null,AVAILABLE}
INFO  [2016-03-03 08:33:02,654] io.dropwizard.setup.AdminEnvironment: tasks =

    POST    /tasks/log-level (io.dropwizard.servlets.tasks.LogConfigurationTask)
    POST    /tasks/gc (io.dropwizard.servlets.tasks.GarbageCollectionTask)

INFO  [2016-03-03 08:33:02,657] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@3f17e012{/,null,AVAILABLE}
INFO  [2016-03-03 08:33:02,667] org.eclipse.jetty.server.ServerConnector: Started application@2dd828c{HTTP/1.1}{0.0.0.0:8080}
INFO  [2016-03-03 08:33:02,670] org.eclipse.jetty.server.ServerConnector: Started admin@68f68a1a{HTTP/1.1}{0.0.0.0:8081}
INFO  [2016-03-03 08:33:02,671] org.eclipse.jetty.server.Server: Started @6700ms
```

- The initial hibernate errors can be ignored. They are caused by HSQLDB.

# APIs

## Data model

There is only one data representation of the credential pair:

```json
{
  "symmetric_key": "base64key",
  "primary": "encrypted using symmetric_key",
  "secondary": "encrypted using symmetric_key"
}
```

The `symmetric_key` is an AES symmetric key used to encrypt the `primary` and `secondary` attributes. The symmetric key is encrypted using the client's public key and, in order to decrypt the credentials, you will need to do the following operations in this order:

1. Decode the symmetric key from base64.
1. Use the client's private key to decrypt the symmetric key.
1. Use the decrypted symmetric key to decrypt the credentials.

## Operations

All APIs require the client's public key (encoded in base64 format) in a custom header `X-Auth-RSA`. The key is a unique key and it's used to retrieve the credentials from the underlying database. The service includes the 4 basic CRUD operations.

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
  "symmetric_key": "key",
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
  "symmetric_key": "key",
  "primary": "encrypted",
  "secondary": "encrypted"
}
```

**Response**

```
201 Created
```

### Put
Updates an existing credential pair, if it exists.

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

These performance metrics were calculated using the in-memory database, using 30 concurrent clients, and 2000 requests in total. It was running on eclipse on a small laptop, which doesn't provide the best output. The numbers shows it's capable of handling more than **300 requests per second** on the `GET` operation. The credential update is not performed as often as a retrieval so its performance is not as important as the retrieval.

## Retrieving credentials
```
Document Path:          /credential
Document Length:        780 bytes

Concurrency Level:      30
Time taken for tests:   5.607 seconds
Complete requests:      2000
Failed requests:        0
Total transferred:      1824000 bytes
HTML transferred:       1560000 bytes
Requests per second:    356.67 [#/sec] (mean)
Time per request:       84.111 [ms] (mean)
Time per request:       2.804 [ms] (mean, across all concurrent requests)
Transfer rate:          317.66 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0   14  13.5     10      72
Processing:     5   68  65.6     43     496
Waiting:        0   59  62.3     33     496
Total:          6   82  63.5     61     497

Percentage of the requests served within a certain time (ms)
  50%     61
  66%     76
  75%     95
  80%    111
  90%    169
  95%    222
  98%    276
  99%    309
 100%    497 (longest request)
```

## Uploading credentials
```
Document Path:          /credential
Document Length:        0 bytes

Concurrency Level:      30
Time taken for tests:   6.852 seconds
Complete requests:      2000
Failed requests:        0
Total transferred:      1722000 bytes
Total body sent:        1930000
HTML transferred:       0 bytes
Requests per second:    291.88 [#/sec] (mean)
Time per request:       102.783 [ms] (mean)
Time per request:       3.426 [ms] (mean, across all concurrent requests)
Transfer rate:          245.42 [Kbytes/sec] received
                        275.06 kb/s sent
                        520.48 kb/s total

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    9  10.1      4      71
Processing:     5   93  89.7     57     506
Waiting:        5   87  89.3     51     497
Total:          5  102  88.8     67     524

Percentage of the requests served within a certain time (ms)
  50%     67
  66%     93
  75%    123
  80%    145
  90%    246
  95%    296
  98%    374
  99%    416
 100%    524 (longest request)
```

# Next steps

Create client integration for hibernate, to automatically retrieve the credentials from the server.