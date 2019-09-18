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

import React, {
  useState,
  useEffect,
  useImperativeHandle,
  forwardRef,
} from 'react';
import PropTypes from 'prop-types';
import { isNull } from 'lodash';

import * as utils from './pipelineToolbarUtils';
import { Modal } from 'components/common/Modal';
import { ListLoader } from 'components/common/Loader';
import { TableWrapper, Table, Inner } from './styles';
import { Input, FormGroup } from 'components/common/Form';

const PipelineNewConnector = forwardRef((props, ref) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newConnectorName, setNewConnectorName] = useState('');

  const { activeConnector, connectors, enableAddButton } = props;

  useEffect(() => {
    enableAddButton(isNull(activeConnector));
  }, [activeConnector, enableAddButton]);

  useImperativeHandle(ref, () => ({
    update() {
      setIsModalOpen(true);
    },
  }));

  const handleChange = ({ target: { value } }) => {
    setNewConnectorName(value);
  };

  const handleConfirm = () => {
    const {
      updateGraph,
      activeConnector: connector,
      pipelineGroup,
      workerClusterName,
    } = props;

    utils.createConnector({
      updateGraph,
      connector,
      newConnectorName,
      workerClusterName,
      group: pipelineGroup,
    });

    props.handleClose();
  };

  return (
    <TableWrapper>
      {!activeConnector ? (
        <ListLoader />
      ) : (
        <Table headers={['connector name', 'version', 'revision']}>
          {connectors.map(({ className: name, version, revision }) => {
            const isActive =
              name === activeConnector.className ? 'is-active' : '';
            return (
              <tr
                className={isActive}
                key={name}
                onClick={() => props.onSelect(name)}
                data-testid="connector-list"
              >
                <td>{name}</td>
                <td>{version}</td>
                <td>{utils.trimString(revision)}</td>
              </tr>
            );
          })}
        </Table>
      )}
      <Modal
        isActive={isModalOpen}
        title="New Connector Name"
        width="370px"
        confirmBtnText="Add"
        handleConfirm={handleConfirm}
        handleCancel={() => setIsModalOpen(false)}
      >
        <Inner>
          <FormGroup data-testid="name">
            <Input
              name="name"
              width="100%"
              placeholder="Connector name"
              data-testid="name-input"
              value={newConnectorName}
              handleChange={handleChange}
            />
          </FormGroup>
        </Inner>
      </Modal>
    </TableWrapper>
  );
});

PipelineNewConnector.propTypes = {
  connectors: PropTypes.array.isRequired,
  onSelect: PropTypes.func.isRequired,
  updateGraph: PropTypes.func.isRequired,
  enableAddButton: PropTypes.func.isRequired,
  handleClose: PropTypes.func.isRequired,
  pipelineGroup: PropTypes.string.isRequired,
  workerClusterName: PropTypes.string.isRequired,
  activeConnector: PropTypes.object,
};

export default PipelineNewConnector;
