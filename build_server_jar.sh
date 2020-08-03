#!/bin/bash

mvn clean package && cp target/aws_test1-1.0.1-jar-with-dependencies.jar awstest.jar
