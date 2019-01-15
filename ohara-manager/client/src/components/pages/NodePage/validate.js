const REQUIRED_FIELD = 'Required field';

const validate = values => {
  const errors = {};

  if (!values.name) {
    errors.name = REQUIRED_FIELD;
  }

  if (!values.port) {
    errors.port = REQUIRED_FIELD;
  }

  if (!values.user) {
    errors.user = REQUIRED_FIELD;
  }

  return errors;
};

export default validate;
