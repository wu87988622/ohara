import React from 'react';
import PropTypes from 'prop-types';
import MuiB from '@material-ui/core/Button';

const MuiButton = ({ text, className, color, variant, onClick }) => {
  return (
    <MuiB
      className={className}
      color={color}
      variant={variant}
      onClick={onClick}
    >
      {text}
    </MuiB>
  );
};

MuiButton.propTypes = {
  text: PropTypes.string,
  className: PropTypes.string,
  color: PropTypes.string,
  variant: PropTypes.string,
  onClick: PropTypes.func,
};

export default MuiButton;
