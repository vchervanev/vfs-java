# vfs-java
Virtual File Server - network demo application

This implements basic Virtual File System which supports concurrent access and modification. The protocol is plain text over TCP/IP - e.g. telnet is suitable for testing it but sample client is added.

Every operation using atomic blocking propogated through "files" hierarchy - only operation which blocked the whole tree can be completed.

See [IFileSystem](/VFS/src/ru/chervanev/vfs/IFileSystem.java) for available commands.

# Class Diagram
![Class Diagram](/srv-class-scheme.png)
