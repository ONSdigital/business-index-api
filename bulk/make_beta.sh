#!/bin/bash

# look for text similar to variable name and replace it with variable value defined in this script.

### script template
### it should be copied for each environment (at least variables part).

ENV_VAR="beta"
DATA_HOME_VAR="\/opt\/scratch\/business-index\/beta"
SFTP_USER_VAR="bi-dev-ss"
SFTP_HOST_VAR="***"
SFTP_PORT_VAR="22"
SFTP_PASS_VAR="****"
SFTP_IN_VAR="in\/clean\/in"
GATEWAY_URL_VAR="https:\/\/apigw***:9443"

ALL_VARS=(ENV_VAR DATA_HOME_VAR SFTP_USER_VAR SFTP_HOST_VAR SFTP_PORT_VAR SFTP_PASS_VAR SFTP_IN_VAR GATEWAY_URL_VAR)

### sample for BETA below

FILE="bi_beta_pipeline.json"

cp bi_pipeline_template.json $FILE

#### NOTE: ".bak" is backup extension for \sed\ on MAC, should not be there if it's Linux.


for var_name in ${ALL_VARS[@]}
do
    sed -i ".bak" "s/$var_name/${!var_name}/g" "$FILE"
done
