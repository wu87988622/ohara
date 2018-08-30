// Success messages
export const LOGIN_SUCCESS = 'You are now logged in!';
export const LOGOUT_SUCCESS = 'You are now logged out!';

export const SCHEMA_CREATION_SUCCESS = 'Schema successfully created!';
export const TOPIC_CREATION_SUCCESS = 'Topic successfully created!';
export const PIPELINE_DELETION_SUCCESS = 'Pipeline successfully deleted!';
export const CONFIG_SAVE_SUCCESS = 'Configuration successfully saved!';
export const TEST_SUCCESS = 'Test has passed!';

// Error messages
export const EMPTY_COLUMN_NAME_ERROR = 'Column Name is a required field!';
export const EMPTY_SCHEMA_NAME_ERROR = 'Schema name is a required field!';
export const EMPTY_SCHEMAS_COLUMNS_ERROR =
  'Please supply at least a Schema column name!';
export const DUPLICATED_COLUMN_NAME_ERROR = 'Column Name cannot be repeated';
export const ONLY_NUMBER_ALLOW_ERROR =
  'partition or replication only accept numeric values';
export const TOPIC_ID_REQUIRED_ERROR =
  'You need to select a topi before creating a new pipeline!';
export const NO_TOPICS_FOUND_ERROR = `You don't have any topics!`;
export const INVALID_TOPIC_ID = `The selected topic doesn't exist!`;

export const GENERIC_ERROR = 'Oops, something went wrong 😱 😱 😱';

// Warning
export const LEAVE_WITHOUT_SAVE =
  'You have unsaved changes or pending requests, are you sure you want to leave?';
