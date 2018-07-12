import React from 'react';
import PropTypes from 'prop-types';

const Button = ({ children }) => {
  return <button className="btn btn-primary">{children}</button>;
};

Button.propTypes = {
  children: PropTypes.any,
};

export default Button;
