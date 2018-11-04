# EventInserter

The application parses given file, calculates events and saves them to the file HSQLDB. The application is built to handle large files in the fastest way possible. For this purpose, JMS And Ehcache are used.

###### 1. JMS

The application is designed around producer/consumer pattern therefore it utilizes Spring's JMS implementation and uses ActiveMQ as the broker. The reason for this design choice is to seperate persistence and bussiness logic therefore increase the throughput.

###### 2. Ehcache

The application utilizes Ehcache which is a caching framework that provides functionality to overflow data. The application is expected to handle large files. In order to meet with this requirement, the application firstly reads the file line by line. Next, as the event item pairs needs to be stored in the memory, we need to apply precautions to prevent out of memory error. Ehcache takes role on this case. By arranging the overflow functionality of it, we can use disk to continue processing when the defined memory limits are reached.

## How to run

The applications expects the file to be provided as a program argument.

Example command:
./gradlew bootRun -Pargs=C:\\Users\\test_data.txt
