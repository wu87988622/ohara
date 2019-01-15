import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Button } from 'common/Form';
import { H2 } from 'common/Headings';
import { cancelBtn, primaryBtn } from 'theme/btnTheme';
import {
  lightGray,
  lighterGray,
  white,
  red,
  durationNormal,
  radiusNormal,
} from 'theme/variables';

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

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

CancelBtn.displayName = 'CancelBtn';

const CloseBtn = styled.div`
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

CloseBtn.displayName = 'CloseBtn';

const Modal = ({
  isActive,
  title,
  handleConfirm,
  handleCancel,
  children,
  width = '300px',
  confirmBtnText = 'Save',
  cancelBtnText = 'Cancel',
  isConfirmDisabled = false,
  isConfirmWorking = false,
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
      <CloseBtn onClick={handleCancel}>
        <i className="fas fa-times" />
      </CloseBtn>
      {children}
      <Actions>
        <CancelBtn
          text={cancelBtnText}
          theme={cancelBtn}
          handleClick={handleCancel}
          data-testid="modal-cancel-btn"
        />
        <Button
          text={confirmBtnText}
          theme={primaryBtn}
          handleClick={handleConfirm}
          data-testid="modal-confirm-btn"
          disabled={isConfirmDisabled}
          isWorking={isConfirmWorking}
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
  children: PropTypes.any.isRequired,
  confirmBtnText: PropTypes.string,
  cancelBtnText: PropTypes.string,
  isConfirmDisabled: PropTypes.bool,
  isConfirmWorking: PropTypes.bool,
  width: PropTypes.string,
};

export default Modal;
