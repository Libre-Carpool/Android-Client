# Libre Carpool
Free, Open Source, and secure carpooling platform.
Free as in freedom and Open Source - given to you under the MIT license.
Free as in free beer - you don't pay for the software. You don't pay the driver.

## Libre Carpool Client
The front end of the [Libre Carpool](https://github.com/Libre-Carpool) platform - an Android application written in [Java](https://en.wikipedia.org/wiki/Java_(programming_language)).

Connects to the [Libre Carpool Server](https://github.com/Libre-Carpool/Server) for authentication, and exchanging information.

You can't register to the service - an existing user with the right priviliges must add you. This is part of the *security* - making sure the other person is trustworthy.

---
### Installation
* Clone / download this repository
* Set the [`URL` in `ServerConnection.java`](https://github.com/Libre-Carpool/Android-Client/blob/master/app/src/main/java/home/climax708/librecarpool/ServerConnection.java#L23) to your web servers URL
* Build & run!