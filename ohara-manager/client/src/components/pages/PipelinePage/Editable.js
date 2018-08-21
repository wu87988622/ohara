import React from 'react';
import PropTypes from 'prop-types';

class EditableLabel extends React.Component {
  static propTypes = {
    text: PropTypes.string.isRequired,
    isEditing: PropTypes.bool,
    labelClassName: PropTypes.string,
    labelFontSize: PropTypes.string,
    labelFontWeight: PropTypes.string,
    inputMaxLength: PropTypes.string,
    inputPlaceHolder: PropTypes.string,
    inputTabIndex: PropTypes.number,
    inputWidth: PropTypes.string,
    inputHeight: PropTypes.string,
    inputFontSize: PropTypes.string,
    inputFontWeight: PropTypes.string,
    inputClassName: PropTypes.string,
    inputBorderWidth: PropTypes.string,
    onFocus: PropTypes.func,
    onFocusOut: PropTypes.func,
  };

  state = {
    isEditing: this.props.isEditing || false,
    text: this.props.text || '',
  };

  handleFocus = () => {
    const { onFocus, onFocusOut } = this.props;
    const { isEditing, text } = this.state;

    if (isEditing) {
      if (this.isFunction(onFocusOut)) {
        onFocusOut(text);
      }
    } else {
      if (this.isFunction(onFocus)) {
        onFocus(text);
      }
    }

    this.setState(({ isEditing }) => ({ isEditing: !isEditing }));
  };

  isFunction = val => {
    return typeof val === 'function';
  };

  handleChange = () => {
    this.setState(() => ({ text: this.textInput.value }));
  };

  render() {
    const {
      inputClassName,
      inputMaxLength,
      inputPlaceHolder,
      inputTabIndex,
      inputWidth,
      inputHeight,
      inputFontSize,
      inputFontWeight,
      inputBorderWidth,
      labelClassName,
      labelFontSize,
      labelFontWeight,
    } = this.props;
    const { isEditing, text } = this.state;

    if (isEditing) {
      return (
        <input
          type="text"
          className={inputClassName}
          ref={input => (this.textInput = input)}
          value={text}
          onChange={this.handleChange}
          onBlur={this.handleFocus}
          style={{
            width: inputWidth,
            height: inputHeight,
            fontSize: inputFontSize,
            fontWeight: inputFontWeight,
            borderWidth: inputBorderWidth,
          }}
          maxLength={inputMaxLength}
          placeholder={inputPlaceHolder}
          tabIndex={inputTabIndex}
          autoFocus
        />
      );
    }

    return (
      <div>
        <label
          className={labelClassName}
          onClick={this.handleFocus}
          style={{
            fontSize: labelFontSize,
            fontWeight: labelFontWeight,
          }}
        >
          {this.state.text}
        </label>
      </div>
    );
  }
}

export default EditableLabel;
