import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Button } from '../../common/Form';
import { H3 } from '../../common/Headings';
import { cancelBtn, primaryBtn } from '../../../theme/btnTheme';
import {
  lightGray,
  lightBlue,
  white,
  red,
  durationNormal,
  radiusNormal,
  lightYellow,
  lightOrange,
  radiusCompact,
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

const H3Wrapper = styled(H3)`
  margin: 0;
  padding: 20px;
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
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

const IconWrapper = styled.i`
  margin: 3px 10px 0 0;
  padding: 5px 10px;
  background-color: ${lightYellow};
  display: inline-block;
  color: ${lightOrange};
  font-size: 12px;
  border-radius: ${radiusCompact};
  align-self: flex-start;
`;

const Warning = styled.p`
  display: flex;
  margin: 20px 25px;
  font-size: 15px;
  line-height: 1.5;
  color: ${lightBlue};
`;

const ConfirmModal = ({
  isActive,
  title,
  handleConfirm,
  handleCancel,
  message,
  width = '400px',
  confirmBtnText = 'Delete',
  cancelBtnText = 'Cancel',
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
      <H3Wrapper>{title}</H3Wrapper>
      <CloseBtn onClick={handleCancel}>
        <i className="fas fa-times" />
      </CloseBtn>
      <Warning>
        <IconWrapper className="fas fa-exclamation" />
        <span>{message}</span>
      </Warning>
      <Actions>
        <CancelBtn
          text={cancelBtnText}
          theme={cancelBtn}
          handleClick={handleCancel}
          data-testid="confirm-modal-cancel-btn"
        />
        <Button
          text={confirmBtnText}
          theme={primaryBtn}
          handleClick={handleConfirm}
          data-testid="confirm-modal-confirm-btn"
          disabled={isConfirmDisabled}
        />
      </Actions>
    </ModalWrapper>
  );
};

ConfirmModal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  handleConfirm: PropTypes.func.isRequired,
  handleCancel: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  message: PropTypes.any.isRequired,
  confirmBtnText: PropTypes.string,
  cancelBtnText: PropTypes.string,
  isConfirmDisabled: PropTypes.bool,
};

export default ConfirmModal;
