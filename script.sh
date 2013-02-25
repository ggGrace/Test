check_status()
{
  status=$1
  if [[ "$status" != "0" ]]; then
    echo "WARN: COMMAND FAILED WITH STATUS = $status"
    exit $status
  fi
  return $status
}

check_status_onlyLog_on_error()
{
  status=$1
  if [[ "$status" != "0" ]]; then
    echo "ERROR: COMMAND FAILED WITH STATUS = $status, BUT IGNORING."
  fi
  return 0
}


OUTPUT_FOLDER=/srv/grid-tmp/etl/mysqldata
TEMP_DIR=$OUTPUT_FOLDER/dimensions/`date +%s`

# set-up
mkdir -p $TEMP_DIR
check_status $?


TABLES=(server_id_to_host adx_verticals page_categories)

for TBL in ${TABLES[@]}; do
  mkdir -p $TEMP_DIR/$TBL
  check_status $?
done

# call ServerIdToHost code in java
$JAVA_HOME/bin/java ServerIdToHost $TEMP_DIR/server_id_to_host/server_id_to_host.txt
check_status_onlyLog_on_error $?


# load data into hive
for TBL in ${TABLES[@]}; do
  echo "INFO: PUSHING EXTRACTED DIM DATA FOR TABLE '$TBL' TO HIVE"
  for FILES in $(ls $TEMP_DIR/$TBL); do
    sed -i -e 's/^[ ]*NULL[ ]*\t/\\N\t/g' -e 's/\t[ ]*NULL[ ]*\t/\t\\N\t/g' -e 's/\\N\t[ ]*NULL[ ]*\t/\\N\t\\N\t/g' -e  's/\t[ ]*NULL[ ]*$/\t\\N/g' -e 's/\x0D//g' $TEMP_DIR/$TBL/$FILES
    hive -e "LOAD DATA LOCAL INPATH '$TEMP_DIR/$TBL/$FILES' OVERWRITE INTO TABLE $TBL;"
    check_status $?
  done
done


# clean-up
rm -rf $TEMP_DIR

exit 0

