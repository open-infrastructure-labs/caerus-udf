# Caerus HDFS CLI (with UDF Support)

A CLI built based on top of WEBHDFS REST API that can support standard storage operations, PUT, GET, DELETE, APPEND, OPEN and LIST with UDF support. 
The major difference of this CLI comparing with other similar product is that we have the ability to process UDF request as part of storage requests for direct invocation of UDFs.

# Getting Started
1. Build the project:
```
mvn clean package
```
2. Run the project

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.1 top level help:
```
    root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/hdfsclient# java -jar target/hdfsclient-0.0.1-SNAPSHOT.jar --help
    Unknown option: --help
    Usage: caerus_hdfscli [COMMAND]
    Commands:
      list    list objects within a HDFS directory.
      put     Upload an object/file to HDFS while provide option to invoke UDF on the object/file.
      delete  Remove an object/file from HDFS.
      copy    Creates a copy of an object that is already stored in HDFS.
      get     Retrieve/Download object from HDFS.
    root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/hdfsclient#

```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.2 subcommand "put" command help:
```
    root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/hdfsclient# java -jar target/hdfsclient-0.0.1-SNAPSHOT.jar put --help
    Usage: caerus_hdfscli put [-h] [-i=<udfInputParameters>]
                              [-o=<optionalOperationParameters>] -s=<localFileName>
                              -t=<hdfsFileName> [-u=<udfName>]
    Upload an object/file to HDFS while provide option to invoke UDF on the
    object/file.
      -h, --help                display this message
      -i, --udfInputParameters=<udfInputParameters>
                                UDF input metadata
      -o, --optionalOperationParameters=<optionalOperationParameters>
                                Optional HDFS upload parameters
      -s, --sourceFileName=<localFileName>
                                Source local full path of the file will be uploaded
      -t, --hdfsFileName=<hdfsFileName>
                                Target HDFS full path of the file
      -u, --udfName=<udfName>   UDF function name
    root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/hdfsclient#
```
