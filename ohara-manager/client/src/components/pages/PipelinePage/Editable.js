import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import styled from 'styled-components';

import * as _ from 'utils/commonUtils';
import { EMPTY_PIPELINE_TITLE_ERROR } from 'constants/messages';
import { Input } from 'common/Form';

const EditInput = styled(Input)`
  display: block;
  width: 100%;
`;

EditInput.displayName = 'Input';

const Icon = styled.i`
  margin: 0 8px 0 4px;
`;

const InputWrapper = styled.div`
  display: flex;
  align-items: center;
`;

class EditableLabel extends React.Component {
  static propTypes = {
    title: PropTypes.string.isRequired,
    handleFocusOut: PropTypes.func,
    handleChange: PropTypes.func.isRequired,
    showIcon: PropTypes.bool,
  };

  static defaultProps = {
    showIcon: true,
  };

  state = {
    isEditing: false,
  };

  handleFocus = (isUpdate = true) => {
    const { handleFocusOut, title } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      if (_.isEmpty(title)) {
        return toastr.error(EMPTY_PIPELINE_TITLE_ERROR);
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
      return this.handleFocus();
    }
  };

  render() {
    const { handleChange, title, showIcon } = this.props;
    const { isEditing } = this.state;

    if (isEditing) {
      return (
        <InputWrapper>
          {showIcon && <Icon className="fas fa-pencil-alt" />}
          <EditInput
            value={title}
            onChange={handleChange}
            onKeyDown={this.handleKeyDown}
            onBlur={this.handleFocus}
            data-testid="title-input"
            autoFocus
          />
        </InputWrapper>
      );
    }

    return (
      <React.Fragment>
        {showIcon && <Icon className="fas fa-pencil-alt" />}
        <label data-testid="title-label" onClick={this.handleFocus}>
          {title}
        </label>
      </React.Fragment>
    );
  }
}

export default EditableLabel;
