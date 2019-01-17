/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Button } from 'common/Form';
import { H3 } from 'common/Headings';
import { cancelBtn, primaryBtn, dangerBtn } from 'theme/btnTheme';
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

const H3Wrapper = styled(H3)`
  margin: 0;
  padding: 20px;
`;

const Actions = styled.div`
  display: flex;
  padding: 20px;
  justify-content: flex-end;
`;

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

CancelBtn.displayName = 'CancelBtn';

const CloseBtn = styled.div`
  position: absolute;
  right: 20px;
  top: 20px;
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
  isDelete = false,
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
          theme={isDelete ? dangerBtn : primaryBtn}
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
  width: PropTypes.string,
  isDelete: PropTypes.bool,
};

export default ConfirmModal;
