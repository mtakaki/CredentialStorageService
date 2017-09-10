#Status
![Build Status](https://codeship.com/projects/f0c2a400-c5ae-0133-5e12-4e8753dd3f97/status?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mtakaki/CredentialStorageServiceClient/badge.svg?branch=master)](https://coveralls.io/github/mtakaki/CredentialStorageServiceClient?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/6d513ce5def644ebb24c3ac22700dcdf)](https://www.codacy.com/app/mitsuotakaki/CredentialStorageServiceClient)
[![Download](https://maven-badges.herokuapp.com/maven-central/com.github.mtakaki/credential-storage-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.mtakaki/credential-storage-client)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.github.mtakaki/credential-storage-client/badge.svg)](http://www.javadoc.io/doc/com.github.mtakaki/credential-storage-client)

# CredentialStorageServiceClient
Java client for [credential storage service](https://github.com/mtakaki/CredentialStorageService). This project can be used as a stand-alone shell client or as a client library.

## Setup

The client requires both private and public keys in a DER format.

```
$ openssl pkcs8 -topk8 -inform PEM -outform DER -in id_rsa -nocrypt > private_key.der
```

```
$ openssl rsa -in id_rsa -out public_key.der -outform DER -pubout
```

The private key is never sent to the server, only the public key. The private key is only used to decrypt the incoming data.

## Stand-alone client

To use the client as a shell stand-alone client, you will need to build the project and use the runnable jar file.

It takes the following arguments:

```
usage:   --delete | --get | --update | --upload  [-p <arg>] -priv <arg>
       -pub <arg> [-s <arg>] -u <arg>
Credential service client

    --delete             Delete credentials
    --get                Retrieves credentials
 -p,--primary <arg>      Primary credential
 -priv,--private <arg>   Private key file
 -pub,--public <arg>     Public key file
 -s,--secondary <arg>    Secondary credential
 -u,--url <arg>          Credential service URL
    --update             Update existing credentials
    --upload             Upload new credentials

Please report issues at
https://github.com/mtakaki/CredentialStorageService/issues
```

### Get credentials

```
$ java -jar target/credential-storage-client-0.0.1-SNAPSHOT.jar --get --priv src/test/resources/private_key.der --pub src/test/resources/public_key.der -u https://damp-sea-57022.herokuapp.com/ | jq .
{
  "primary": "test user",
  "secondary": "青"
}
```

### Upload credentials

```
$ java -jar target/credential-storage-client-0.0.1-SNAPSHOT.jar --priv src/test/resources/private_key.der --pub src/test/resources/public_key.der -u https://damp-sea-57022.herokuapp.com/ --upload --primary "test user" --secondary "青"
Credential successfully uploaded!
```

If upload is used with an existing credential it will completely override the existing one and regenerate a new symmetrical key.

### Update credentials

```
$ java -jar target/credential-storage-client-0.0.1-SNAPSHOT.jar --priv src/test/resources/private_key.der --pub src/test/resources/public_key.der -u https://damp-sea-57022.herokuapp.com/ --update --primary "user"
Credential successfully updated!
```

Update supports partial updates, so it preserves the existing information. The symmetrical key is regenerated nonetheless.

### Delete credentials

```
$ java -jar target/credential-storage-client-0.0.1-SNAPSHOT.jar --priv src/test/resources/private_key.der --pub src/test/resources/public_key.der -u https://damp-sea-57022.herokuapp.com/ --delete
Credential successfully deleted!
```