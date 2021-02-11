#!/bin/bash         

echo "Start building Caerus UDF related projects, generating API documentations..."

echo "mvn clean package..."
mvn clean package

echo "mvn compile javadoc:javadoc javadoc:aggregate..."
mvn compile javadoc:javadoc javadoc:aggregate


echo -e "\n------------------------------------------------------------------------\n"
echo -e "Done building Caerus UDF related projects and generating API documentations\n"

echo -e "To check all API documents, use web browser to point to: file:///{CAERUS_HOME}/ndp/udf/target/site/apidocs/javadoc/index.html\n"







