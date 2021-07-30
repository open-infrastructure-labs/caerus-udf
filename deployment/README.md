# Caerus UDF Deployment Automation

Caerus UDF related services can be deployed as separate microservice as stated in the README of parent "caerus-udf" directory, for ease-of-use, here we dockerize the major components as follows:
  1. docker "caerus-ndp-udf-backend": it has major Caerus UDF services and their ports
        * 8000 - Ndp Service
        * 8002 - UDF Service
        * 8003 - Event Listener Service
        * 8004 - Registry Service
  2. docker "redis": for event notification and docker registry

Currently, the automation only supports public docker hub option, the private docker registry support can be added as needed in the future.
 
# Getting Started
The steps below show how to use this deployment automation:

## Step 1: Get the latest Caerus code and build udf projects
```
> git pull
> cd $CAERUS_UDF_HOME/
> root@ubuntu1804:/home/ubuntu/caerus-udf#./build.sh
```
## Step 2: Set up OPENFAAS and deploy needed serverless functions (e.g. thumbnail function)
Follow the below link: [faas](../faas)

## Step 3: Create environmental variable for OPENFAAS secret
```
root@ubuntu1804:/home/ubuntu/caerus-udf/deployment# chmod +x create_secrets.sh 

root@ubuntu1804:/home/ubuntu/caerus-udf/deployment# source ./create_secrets.sh 
```

## Step 4: Rest of the support, including different type of storage systems, other necessary application platforms like Redis, Nifi, etc. 
  1. [Object Storage Deployment Support](README_OBJECT_STORAGE_DEPLOYMENT.md)
  2. [HDFS Deployment Support](README_HDFS_DEPLOYMENT.md)
