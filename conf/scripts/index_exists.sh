#!/bin/sh

# This script will fail if the provided index name already exists within
# ElasticSearch, as we do not want to delete it.

SCRIPT_NAME=$0
REQUIRED_NUM_ARGS=3

# Fail fast if we get any errors
set -e

function usage {
    echo "usage: ${SCRIPT_NAME} host port index_name"
    echo "  host                    elasticsearch host"
    echo "  port                    elasticsearch port"
    echo "  index_name              name of index to check exists"
    exit 1
}

# Fail the script if we recieve an incorrect number of arguments
if [[ $# -ne REQUIRED_NUM_ARGS ]] ; then
    echo 'Error, you need to provide the correct number of arguments.'
    usage
fi

HOST=$1
PORT=$2
INDEX_NAME=$3

echo "Checking if index [${INDEX_NAME}] already exists within ElasticSearch"
http_code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null "${HOST}:${PORT}/${INDEX_NAME}")

echo "HTTP Code returned from checking if the index [$INDEX_NAME] exists: $http_code"
if [[ $http_code == '200' ]] ; then
    echo 'Index already exists, please use an index name that does not already exist.'
    exit 1
else
    echo 'Index does not exist, proceeding to Create Index step.'
    exit 0
fi