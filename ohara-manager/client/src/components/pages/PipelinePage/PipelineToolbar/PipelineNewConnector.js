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
import DialogContent from '@material-ui/core/DialogContent';
import { isNull } from 'lodash';

import * as utils from './pipelineToolbarUtils';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import { ListLoader } from 'components/common/Loader';
import { Dialog } from 'components/common/Mui/Dialog';
import { Table } from './styles';
import { InputField } from 'components/common/Mui/Form';
import { graph as graphPropType } from 'propTypes/pipeline';

const PipelineNewConnector = forwardRef((props, ref) => {
  const { activeConnector, connectors, enableAddButton } = props;

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newConnectorName, setNewConnectorName] = useState('');

  const { showMessage } = useSnackbar();

  useEffect(() => {
    enableAddButton(isNull(activeConnector));
  }, [activeConnector, enableAddButton]);

  useImperativeHandle(ref, () => ({
    update() {
      setIsModalOpen(true);
    },
  }));

  const handleConfirm = () => {
    const {
      updateGraph,
      activeConnector: connector,
      pipelineGroup,
      workerClusterName,
      graph,
    } = props;

    utils.createConnector({
      updateGraph,
      connector,
      newConnectorName,
      workerClusterName,
      showMessage,
      graph,
      group: pipelineGroup,
    });

    props.handleClose();
  };

  return (
    <DialogContent>
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
      <Dialog
        title="New connector name"
        open={isModalOpen}
        confirmDisabled={newConnectorName.length === 0}
        width="xs"
        handleConfirm={handleConfirm}
        handleClose={() => setIsModalOpen(false)}
        testId="new-connector-dialog"
      >
        <DialogContent>
          <InputField
            autoFocus
            placeholder="myconnector"
            input={{
              name: 'name',
              onChange: event => setNewConnectorName(event.target.value),
              value: newConnectorName,
            }}
            inputProps={{ 'data-testid': 'name-input' }}
          />
        </DialogContent>
      </Dialog>
    </DialogContent>
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
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  activeConnector: PropTypes.object,
};

export default PipelineNewConnector;
