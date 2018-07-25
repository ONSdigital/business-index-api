#!/bin/sh

# This script will create an ElasticSearch index using passed in command line
# arguments for the host, port, index name and path to JSON file which includes
# the index definition.

SCRIPT_NAME=$0
REQUIRED_NUM_ARGS=4

# Fail fast if we get any errors
set -e

function usage {
    echo "usage: ${SCRIPT_NAME} host port index_name path_to_index_json"
    echo "  host                    elasticsearch host"
    echo "  port                    elasticsearch port"
    echo "  index_name              name of index to create"
    echo "  path_to_index_json      path to json file with index definition"
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
PATH_TO_INDEX_DEFINITION=$4

# Make the curl PUT request to create the index, passing in our index definition JSON
echo 'Creating ElasticSearch index...'
curl --fail -XPUT "${HOST}:${PORT}/${INDEX_NAME}" -d @"${PATH_TO_INDEX_DEFINITION}"
echo

echo "Successfully created ElasticSearch index [${INDEX_NAME}] with the following settings:"
echo "$(cat ${PATH_TO_INDEX_DEFINITION})"