import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Button } from '../../common/Form';
import { H2 } from '../../common/Heading';
import { cancelButton, submitButton } from '../../../theme/buttonTheme';
import {
  lightGray,
  lighterGray,
  white,
  red,
  durationNormal,
  radiusNormal,
} from '../../../theme/variables';

const ModalWrapper = styled(ReactModal)`
  top: 15%;
  left: 50%;
  right: auto;
  bottom: auto;
  margin-right: -50%;
  transform: translate(-50%, 0);
  padding: 0;
  background-color: ${white};
  position: absolute;
  width: ${props => props.width};
  border-radius: ${radiusNormal};

  &:focus {
    outline: 0;
  }
`;

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${lighterGray};
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
  border-top: 1px solid ${lighterGray};
  justify-content: flex-end;
`;

const CancelButton = styled(Button)`
  margin-right: 10px;
`;

CancelButton.displayName = 'CancelButton';

const CloseButton = styled.div`
  position: absolute;
  right: 22px;
  top: 22px;
  cursor: pointer;
  color: ${lightGray};
  padding: 5px;
  transition: ${durationNormal} all;

  &:hover {
    transition: ${durationNormal} all;
    color: ${red};
  }
`;

CloseButton.displayName = 'CloseButton';

const Modal = ({
  isActive,
  title,
  handleConfirm,
  handleCancel,
  children,
  width = '300px',
  confirmButtonText = 'save',
  isConfirmDisabled = false,
}) => {
  return (
    <ModalWrapper
      isOpen={isActive}
      contentLabel="Modal"
      ariaHideApp={false}
      width={width}
      onRequestClose={handleCancel}
    >
      <H2Wrapper>{title}</H2Wrapper>
      <CloseButton onClick={handleCancel}>
        <i className="fas fa-times" />
      </CloseButton>
      {children}
      <Actions>
        <CancelButton
          text="Cancel"
          theme={cancelButton}
          handleClick={handleCancel}
          data-testid="modal-cancel-button"
        />
        <Button
          text={confirmButtonText}
          theme={submitButton}
          handleClick={handleConfirm}
          data-testid="modal-confirm-button"
          disabled={isConfirmDisabled}
        />
      </Actions>
    </ModalWrapper>
  );
};

Modal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  handleConfirm: PropTypes.func.isRequired,
  handleCancel: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  confirmButtonText: PropTypes.string,
  isConfirmDisabled: PropTypes.bool,
};

export default Modal;
