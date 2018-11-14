import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';

import * as _ from 'utils/commonUtils';
import { Input } from 'common/Form';

class EditableLabel extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    labelClassName: PropTypes.string,
    handleFocusOut: PropTypes.func,
    handleChange: PropTypes.func.isRequired,
  };

  state = {
    isEditing: false,
  };

  handleFocus = (isUpdate = true) => {
    const { handleFocusOut, title } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      if (_.isEmpty(title)) {
        return toastr.error('Pipeline title cannot be empty!');
      }
      handleFocusOut(isUpdate);
    }

    this.setState({ isEditing: !isEditing });
  };

  handleKeyDown = e => {
    const enterKey = 13;
    const escKey = 27;

    if (e.keyCode === enterKey) {
      return this.handleFocus();
    }

    if (e.keyCode === escKey) {
      return this.handleFocus(false);
    }
  };

  render() {
    const { handleChange, title } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      return (
        <Input
          value={title}
          onChange={handleChange}
          onKeyDown={this.handleKeyDown}
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
