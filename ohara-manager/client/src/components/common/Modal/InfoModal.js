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
import { H2 } from 'common/Headings';

const ModalWrapper = styled(ReactModal)`
  top: 15%;
  left: 50%;
  right: auto;
  bottom: auto;
  margin-right: -50%;
  transform: translate(-50%, 0);
  padding: 0;
  background-color: ${props => props.theme.white};
  position: absolute;
  width: ${props => props.width};
  border-radius: ${props => props.theme.radiusNormal};

  &:focus {
    outline: 0;
  }
`;

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${props => props.theme.lighterGray};
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
  color: ${props => props.theme.lightGray};
  padding: 5px;
  transition: ${props => props.theme.durationNormal} all;

  &:hover {
    transition: ${props => props.theme.durationNormal} all;
    color: ${props => props.theme.red};
  }
`;

CloseBtn.displayName = 'CloseBtn';

const Modal = ({
  isActive,
  title,
  handleCancel,
  children,
  width = '300px',
  testId,
}) => {
  return (
    <ModalWrapper
      isOpen={isActive}
      contentLabel="Modal"
      ariaHideApp={false}
      width={width}
      onRequestClose={handleCancel}
      testId={testId}
    >
      <H2Wrapper>{title}</H2Wrapper>
      <CloseBtn data-testid="close-btn" onClick={handleCancel}>
        <i className="fas fa-times" />
      </CloseBtn>
      {children}
    </ModalWrapper>
  );
};

Modal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  handleCancel: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  children: PropTypes.any.isRequired,
  width: PropTypes.string,
  testId: PropTypes.string,
};

export default Modal;
