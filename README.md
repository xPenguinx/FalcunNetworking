# Falcun Networking

Starting the client test client:
````
java -jar client.jar <username> <uuid> <access-token>
````

Starting the test server:
```
java -jar server.jar
```

Starting the dev db:
```
docker-compose up -f ./docker/devDb.yml -D
```

Configuration:  
falcunnetworking.properties next to jar.  
Options: db.user, db.pass, db.name, db.host, db.port

The server will setup tables in the configured database using the `falcun` prefix.

## Docs

See [this file](DOCS.md)

## Status

- [x] setup of modules
- [x] initial netty bootstrap
- [x] initial packet bootstrap (ping pong example)
- [x] initial db boilerplate (user read/write)
- [x] encryption
- [x] chat
  - [x] friends
  - [x] groups
  - [x] broadcast
- [x] friend list
  - [x] persistence
  - [x] invites
  - [x] listing
  - [x] packets
- [x] authentication
  - [x] packets
  - [x] talking to mojang
- [x] groups
  - [x] persistence
  - [x] invites
  - [x] listing
  - [x] kicking
  - [x] packets
  - [x] persistence of chat messages

- [x] proper testing   
- [x] docs

- [x] console with commands, lol
