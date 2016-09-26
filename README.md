# THIS PROJECT IS DEPRECATED
watershed is no longer maintained, and **this** repository will be removed from GitHub on or after 2017-03-26.

# Watershed

Archival-replay suite designed for AWS Kinesis and S3

###Watershed is a system of tools designed to:
* Launch an EMR Cluster with Apache Drill and Pump
* Configure Apache Drill and Hive to recognize S3 Buckets and Kinesis Streams
* Expose an HTTP REST service which allows someone to query against Drill
* Run SQL Query Results back into a Kinesis Stream

####Watershed includes the Pump REST Service:
* which supports key-value compaction so only the last record for a key is replayed.
* which supports bounded replay (one needn’t replay the full archive).
* which supports filtered replay (only replay records matching some criteria).
* which supports annotating records as they are replayed in order to alter
consumer behavior, such as to force overwrite.
* which, with consumer cooperation, provides some definition of eventual
consistency with respect to records that arrive on a stream concurrently with a
replay operation, without requiring this solution to mediate the flow of the stream.

###Prerequisites
* Python3
* Pip3
* Java 7 (for development of Pump)
    
###Wiki
Any information you need can be found in our [wiki](https://github.com/commercehub-oss/watershed/wiki).

####Getting Started
* [What is Watershed?](https://github.com/commercehub-oss/watershed/wiki/What-is-Watershed%3F)
* [Goals, Constraints, and Additional Information](https://github.com/commercehub-oss/watershed/wiki/Goals,-Constraints,-and-Additional-Information)
* [Installing](https://github.com/commercehub-oss/watershed/wiki/Installing)

####Using Watershed
* [How to use Watershed](https://github.com/commercehub-oss/watershed/wiki/How-to-use-Watershed)
  * [Configuration JSON](https://github.com/commercehub-oss/watershed/wiki/How-to-use-Watershed#configuration-file)
  * [Watershed CLI](https://github.com/commercehub-oss/watershed/wiki/How-to-use-Watershed#using-the-watershed-cli-tool)
  * [Pump Client](https://github.com/commercehub-oss/watershed/wiki/How-to-use-Watershed#using-pump-client)

####More Information
* [Expected Archival Data](https://github.com/commercehub-oss/watershed/wiki/Expected-Archival-Data)
