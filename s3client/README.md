# Caerus S3 CLI (with UDF Support)

A CLI built based on AWS S3 SDK that can support standard storage operations by using standard AWS S3 protocols, PUT, GET, DELETE, COPY and LIST with UDF support. 
The major difference of this CLI comparing with other similar product is that we have the ability to process UDF request as part of storage requests for direct invocation of UDFs.

# Getting Started
1. Build the project:
```
mvn clean package
```
2. Run the project

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.1 top level help:
```
        root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/s3client# java -jar target/s3client-0.0.1-SNAPSHOT.jar --help
        Unknown option: --help
        Usage: caerus_s3cli [COMMAND]
        Commands:
          list    list objects within a S3 bucket.
          put     Upload an object/file to a S3 bucket while provide option to invoke
                    UDF on the object/file.
          delete  Remove an object/file from anS3 bucket.
          copy    Creates a copy of an object that is already stored in S3.
          get     Retrieve object from S3.

```
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.2 subcommand "put" command help:
```
        root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/s3client# java -jar target/s3client-0.0.1-SNAPSHOT.jar put --help
        Usage: caerus_s3cli put [-h] -b=<bucketName> -f=<uploadFileName>
                                [-i=<udfInputParameters>] [-k=<keyName>] [-u=<udfName>]
        Upload an object/file to a S3 bucket while provide option to invoke UDF on the
        object/file.
          -b, --bucketName=<bucketName>
                                    S3 bucket name
          -f, --uploadFileName=<uploadFileName>
                                    Full path of the file will be uploaded
          -h, --help                display this message
          -i, --udfInputParameters=<udfInputParameters>
                                    UDF input metadata
          -k, --keyName=<keyName>   new object key name in s3, use the same file name as key
                                      if not supplied
          -u, --udfName=<udfName>   UDF function name
        root@ubuntu1804:/home/ubuntu/caerus/caerus/ndp/udf/s3client#
```
