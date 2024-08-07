# WORDLE-CLI
a simple CLI version of the popular game WORDLE in Java developed networking course for university
This project was done for the Networking and laboratory course at UNI
it makes use of
- GSON library
- java API's TCP and UDP

both the server and client have a config.properties file to configure the server Ip adress(for the client) and the UDP multicast address(for both)
The file necessary for the GSON library are already included in the repo
# usage
to start a client use the following command :  `java WORDLEClientMain <path to .properties file>`

to start a server use the following command :  `java -cp .:<path to gson linrary .jar file> WORDLEServerMain <path to .properties file> `
the game was written in italian
