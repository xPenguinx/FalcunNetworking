# Docs

## Tech Stack

This project is build upon netty. It uses nettys NioServerSocketChannel and a thread pool to communicate between server and client.
Optionally (after authentication), traffic is encrypted using a continuous AES cipher, in CFB-8 mode (for more info, see [this](https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Cipher_feedback_(CFB)))

The pipeline is setup as follows:
* decrypter (optionally): decrypts incoming traffic
* length decoder: splits incoming traffic into a continuous stream of packets
* decoder: decodes those packets
* < server/client logic >
* encoder: encodes the outgoing packets
* length encoder: prefixes the packets with their length
* encrypter (optionally): encrypts the packet stream

One the server side, jdbi is used as a thin abstraction above the database, hikaricp is used to pool database connections.

All database queries were written against a mysql database, they may or may not work against other sql databases. The queries are located in the Dao interfaces.
jdbi maps those queries against table classes, which mirror the database tables, or directly against api objects.

## Concept

`FalcunClient/Server` are the entry points. Both are blocking and thus should be started in their own threads. They setup everything needed.
This is also the location where you would pass dependencies into this project, so that they can be used to handle packets down the line.

Both client and server have an implementation of `FalcunHandler`. Those contain the packet processing and thus the logic (or should delegate the logic).

Both client and server have a `FalcunConsole`. This is a simple CLI which offers commands to test the project. 
They allow you to implement debug commands.  
They aren't really designed for production usage (the commands aren't user-friendly nor particularly failsafe), but could easily be expanded to do that.

Packets are registered in the `FalcunPacketRegistry`.
The order should be changed after initial deployment, as it would lead to an ID shift and thus a protocol break!
New packets need to be registered here, so that client and server can decode and encode them.

A `Property` util has been included to allow configuring certain aspects of this project. It will first search for system properties, then environment variables,
then entries into a `falcunnetworking.properties` files (the location can be overwritten using the system property `falcun.property.location`) and will fallback to default values.
It might be beneficial to hook that up into your own configuration system if applicable.

## Packets

The packets should contain constructors for all usecases. Below is a short rundown of all packets and their usage. For more info, refer to the implementation.

### Client -> Server

* LoginStart:   
    Send by the client to inform the server of its username, the server will respond with encryption request.
* EncryptionResponse:  
    Send by the client to after authenticating to mojang, enables encryption on the client. When the server receives this packet, it will
    also authenticate to mojang and then enable encryption.
* Chat:  
    Allow the client to send a chat message, to either a group or a user on the friend list.
* Action:  
    Allows the client to execute an action. Multiple constructors are available for the different types.  
    Actions can be sending or rejecting invites, creating groups, kicking users, etc.
* ListRequest:  
    Sends a request to the server, asking to receive the friend list, the list of groups, or different list of invites.
* Ping:  
    Simple ping packet with a payload, can be used to track latency or keep the connection alive.

### Server -> Client

* EncryptionRequest:  
    Send by the server after the client started the login process, informs the client of the servers public key.
* Disconnect:  
    Send by the server to terminate the connection. Will contain a reason why the connection was terminated.
* Chat:  
    Indicates to the client that it received a message. This can be from a friend, from a group, or a system wide message.
* InviteStatus:  
    Send by the server to inform the client that an invite changed its status, this can be for example a new invite or a revocation.
* ListResponse:  
    Completes a list request by sending the requested list.
* Response:  
    A generic response packet, that can inform the client of the status of an action it issued or inform the client of errors.
* Pong:  
    See Client -> Ping, server will answer the same payload back.
    

## Auth

The authentication flow works the exact same way a minecraft client and server use.  
For detailed into, see [this document](https://wiki.vg/Protocol_Encryption#Authentication)  
Basically, the client generates a server hash, that basically consists of a shared secret, encoded by the servers public key.  
That hash is send to mojangs session server, indicating that the user is attempting to log onto that server.  
The client then sends the shared secret to the server, which can then ask mojang if that user has tried to join the server with valid credentials,
 completing the login process and enabling encryption.  
(Additionally, a verification token is used to protect the integrity of the shared secret)
 
This way, the access token of the user never leaves the users to 3rd parties, only to mojangs session server, 
 but the server can still verify that the user attempting to login is authenticated against mojang

However, this makes testing hard, as you need valid access tokens. Therefore the `Constants.java` file contains a constant to enable an "offline" mode, 
 where both server and client dont attempt to authenticate against mojang. Do however note this the server with then generate a uuid from the name of the user, and not use the real uuid!
