import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Input, Button } from '../../common/Form';
import { H2 } from '../../common/Heading';
import { cancelButton, submitButton } from '../../../theme/buttonTheme';
import {
  lightBlue,
  lightGray,
  lighterGray,
  red,
  durationNormal,
} from '../../../theme/variables';

const modalStyles = {
  content: {
    top: '15%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, 0)',
    padding: 0,
  },
};

const FormInner = styled.div`
  padding: 20px;
`;

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${lighterGray};
`;

const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
  border-top: 1px solid ${lighterGray};
  justify-content: flex-end;
`;

const Label = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
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
  handleChange,
  handleCreate,
  handleClose,
  topicName,
  partitions,
  replicationFactor,
  isCreateTopicWorking,
}) => {
  return (
    <ReactModal
      isOpen={isActive}
      contentLabel="Modal"
      ariaHideApp={false}
      style={modalStyles}
      onRequestClose={handleClose}
    >
      <H2Wrapper>New topic</H2Wrapper>
      <CloseButton onClick={handleClose}>
        <i className="fas fa-times" />
      </CloseButton>
      <form>
        <FormInner>
          <FormGroup>
            <Label>Cluster name</Label>
            <Input
              type="text"
              width="250px"
              id="topicName"
              placeholder="kafka-cluster"
              value={topicName}
              data-testid="modalClusterName"
              handleChange={handleChange}
            />
          </FormGroup>

          <FormGroup>
            <Label>Partitions</Label>
            <Input
              type="text"
              width="250px"
              id="partitions"
              placeholder="1"
              value={partitions}
              data-testid="modalPartitions"
              handleChange={handleChange}
            />
          </FormGroup>

          <FormGroup>
            <Label>Replication Factor</Label>
            <Input
              type="text"
              width="250px"
              id="replicationFactor"
              placeholder="5"
              value={replicationFactor}
              data-testid="modalReplicationFactor"
              handleChange={handleChange}
            />
          </FormGroup>
        </FormInner>
        <Actions>
          <CancelButton
            text="Cancel"
            theme={cancelButton}
            handleClick={handleClose}
            data-testid="modalCancelButton"
          />
          <Button
            text="Save"
            theme={submitButton}
            handleClick={handleCreate}
            isWorking={isCreateTopicWorking}
            data-testid="modalSubmitButton"
          />
        </Actions>
      </form>
    </ReactModal>
  );
};

Modal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  handleChange: PropTypes.func.isRequired,
  handleClose: PropTypes.func.isRequired,
};

export default Modal;
