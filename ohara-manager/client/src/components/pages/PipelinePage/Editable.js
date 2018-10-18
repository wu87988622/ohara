import React from 'react';
import PropTypes from 'prop-types';

import * as _ from 'utils/helpers';
import { Input } from 'common/Form';

class EditableLabel extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    labelClassName: PropTypes.string,
    onFocus: PropTypes.func,
    handleFocusOut: PropTypes.func,
    handleChange: PropTypes.func.isRequired,
  };

  state = {
    isEditing: false,
  };

  handleFocus = () => {
    const { onFocus, handleFocusOut } = this.props;
    const { isEditing, title } = this.state;

    if (isEditing) {
      if (_.isFunction(handleFocusOut)) {
        handleFocusOut(title);

        console.log('out right now!');
      }
    } else {
      if (_.isFunction(onFocus)) {
        onFocus(title);
      }
    }

    this.setState(({ isEditing }) => ({ isEditing: !isEditing }));
  };

  render() {
    const { handleChange, title } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      return (
        <Input
          value={title}
          onChange={handleChange}
          onBlur={this.handleFocus}
          width="400px"
          autoFocus
        />
      );
    }

    return (
      <div>
        <label onClick={this.handleFocus}>{title}</label>
      </div>
    );
  }
}

export default EditableLabel;
