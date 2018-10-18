import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';

import * as _ from 'utils/helpers';
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

  handleFocus = () => {
    const { handleFocusOut, title } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      if (_.isEmpty(title)) {
        return toastr.error('Pipeline title cannot be empty!');
      }
      handleFocusOut(title);
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
