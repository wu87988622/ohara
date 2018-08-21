import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';

const GraphLink = ({ to, children, ...rest }) => {
  return (
    <Link to={to} {...rest}>
      {children}
    </Link>
  );
};

GraphLink.propTypes = {
  to: PropTypes.string,
  children: PropTypes.any,
};

export default GraphLink;
