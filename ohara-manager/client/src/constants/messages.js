/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/******************************
  Success messages -- green 
*******************************/

// Login
export const LOGIN_SUCCESS = 'You are now logged in!';
export const LOGOUT_SUCCESS = 'You are now logged out!';

// Schema
export const SCHEMA_CREATION_SUCCESS = 'Schema successfully created!';

// Topic
export const TOPIC_CREATION_SUCCESS = 'Topic successfully created!';
export const TOPIC_DELETION_SUCCESS = 'Successfully deleted the topic:';

// Node
export const NODE_CREATION_SUCCESS = 'Node successfully created!';
export const NODE_SAVE_SUCCESS = 'Node successfully saved!';

// Service
export const SERVICE_CREATION_SUCCESS = 'Service successfully created!';

// Pipeline
export const PIPELINE_CREATION_SUCCESS = 'Pipeline successfully created!';
export const PIPELINE_DELETION_SUCCESS = 'Successfully deleted the pipeline:';
export const START_CONNECTOR_SUCCESS = 'Connector successfully started!';
export const CONNECTOR_DELETION_SUCCESS = 'Successfully deleted the connector:';

// Configuration
export const CONFIG_DELETE_SUCCESS = 'Successfully deleted the configuration:';
export const CONFIG_SAVE_SUCCESS = 'Configuration successfully saved!';
export const CONFIG_DELETE_CHECK = 'Please select a connection to delete!';
export const TEST_SUCCESS = 'Test has passed!';

// Stream app
export const STREAM_APP_UPLOAD_SUCCESS = 'Stream app successfully uploaded!';
export const STREAM_APP_RENAME_SUCCESS = 'Stream app successfully renamed!';
export const STREAM_APP_DELETE_SUCCESS = 'Successfully deleted the stream app!';
export const STREAM_APP_START_SUCCESS = 'Stream app successfully started!';
export const STREAM_APP_STOP_SUCCESS = 'Stream app successfully stopped!';
export const PLUGIN_UPLOAD_SUCCESS = 'Plugin successfully uploaded!';
export const STREAM_APP_DELETION_SUCCESS =
  'Successfully deleted the stream app';

/******************************
  Error messages -- red
*******************************/

// Error messages
export const EMPTY_NAME_ERROR = 'Name is a required field!';
export const EMPTY_CONN_URL_ERROR = 'Connection URL is a required field!';
export const EMPTY_HOSTNAME_ERROR = 'Hostname is a required field!';
export const EMPTY_PORT_ERROR = 'Port is a required field!';
export const EMPTY_USER_ERROR = 'User is a required field!';
export const EMPTY_PASSWORD_ERROR = 'Password is a required field!';
export const EMPTY_COLUMN_NAME_ERROR = 'Column Name is a required field!';
export const EMPTY_SCHEMA_NAME_ERROR = 'Schema name is a required field!';
export const EMPTY_SCHEMAS_COLUMNS_ERROR =
  'Please supply at least a Schema column name!';
export const DUPLICATED_COLUMN_NAME_ERROR = 'Column Name cannot be repeated';
export const ONLY_NUMBER_ALLOW_ERROR =
  'partition or replication only accept numeric values';
export const INVALID_TOPIC_ID = `The selected topic doesn't exist!`;
export const TEST_FAILED_ERROR = `Didn't pass the test!`;

// Pipelines
export const PIPELINE_DELETION_ERROR =
  'Oops, something went wrong, we cannot delete the selected pipeline:';
export const CANNOT_START_PIPELINE_ERROR =
  'Failed to start the pipeline, please check your pipeline settings';
export const NO_WORKER_CLUSTER_FOUND_ERROR =
  'Please create a cluster before creating pipelines!';
export const NO_TOPIC_IS_SUPPLIED = 'Please select a topic!';

// Connectors
export const CANNOT_START_STREAM_APP_ERROR =
  'Cannot complete your action, please check your stream app settings';

export const NO_CONFIGURATION_FOUND_ERROR = `You don't have any HDFS connections set up yet, please create one before you can proceed`;

export const CANNOT_DELETE_TOPIC_ERROR = `You cannot delete the topic while it has any connection`;

export const EMPTY_PIPELINE_TITLE_ERROR = 'Pipeline title cannot be empty!';
export const CANNOT_UPDATE_WHILE_RUNNING_ERROR = `You cannot update the pipeline while it's running`;

export const GENERIC_ERROR = 'Oops, something went wrong 😱 😱 😱';

/******************************
  Warning messages -- orange
*******************************/
export const LEAVE_WITHOUT_SAVE =
  'You have unsaved changes or pending requests, are you sure you want to leave?';
