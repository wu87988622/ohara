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
import { Link } from 'react-router-dom';
import { get, truncate, isEmpty, isNull } from 'lodash';

import * as jarApi from 'api/jarApi';
import * as URLS from 'constants/urls';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import { ListLoader } from 'components/common/Loader';
import { Dialog } from 'components/common/Mui/Dialog';
import { createConnector } from './pipelineToolbarUtils';
import { LoaderWrapper } from './styles';
import { InputField } from 'components/common/Mui/Form';
import { Warning } from 'components/common/Messages';
import { Select } from 'components/common/Mui/Form';

const PipelineNewStream = forwardRef((props, ref) => {
  const [newStreamName, setNewStreamName] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [jars, setJars] = useState([]);
  const [currentJar, setCurrentJar] = useState('');

  const { showMessage } = useSnackbar();

  const {
    workerClusterName,
    enableAddButton,
    updateGraph,
    pipelineGroup,
    brokerClusterName,
  } = props;
  const jarGroup = workerClusterName;

  useEffect(() => {
    const isDisabled = currentJar === '' || currentJar === 'Please select...';
    enableAddButton(isDisabled);

    const fetchJars = async () => {
      const response = await jarApi.fetchJars(jarGroup);
      setIsLoading(false);
      const jars = get(response, 'data.result', []);

      if (!isEmpty(jars)) {
        setJars(jars);
      }
    };

    fetchJars();
  }, [currentJar, enableAddButton, jarGroup]);

  useImperativeHandle(ref, () => ({
    update() {
      setIsModalOpen(true);
    },
  }));

  const handleChange = ({ target: { value } }) => {
    const newStreamAppName = truncate(value.replace(/[^0-9a-z]/g, ''), {
      length: 20,
      omission: '',
    });
    setNewStreamName(newStreamAppName);
  };

  const handleConfirm = () => {
    const { group, name } = jars.find(jar => jar.name === currentJar);

    const connector = {
      jarKey: { group, name },
      className: 'stream',
      typeName: 'stream',
    };

    createConnector({
      updateGraph,
      connector,
      newStreamName,
      brokerClusterName,
      showMessage,
      group: pipelineGroup,
    });

    props.handleClose();
  };

  return (
    <div>
      {isLoading ? (
        <LoaderWrapper>
          <ListLoader />
        </LoaderWrapper>
      ) : (
        <DialogContent>
          {isEmpty(jars) || isNull(currentJar) ? (
            <Warning
              text={
                <>
                  {`You don't have any stream jars available in this workspace yet. But you can create one in `}
                  <Link
                    to={`${URLS.WORKSPACES}/${workerClusterName}/streamjars`}
                  >
                    here
                  </Link>
                </>
              }
            />
          ) : (
            <Select
              required
              autoFocus
              placeholder="mystream"
              input={{
                name: 'stream',
                onChange: event => setCurrentJar(event.target.value),
                value: currentJar,
              }}
              meta={{}}
              list={jars.map(jar => jar.name)}
              inputProps={{
                'data-testid': 'streamapp-select',
              }}
            />
          )}
        </DialogContent>
      )}

      <Dialog
        title="New stream app name"
        open={isModalOpen}
        confirmDisabled={newStreamName.length === 0}
        maxWidth="xs"
        handleConfirm={handleConfirm}
        handleClose={() => setIsModalOpen(false)}
        testId="new-steam-dialog"
      >
        <DialogContent>
          <InputField
            autoFocus
            placeholder="mystreamapp"
            input={{
              name: 'name',
              onChange: handleChange,
              value: newStreamName,
            }}
            inputProps={{ 'data-testid': 'name-input' }}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
});

PipelineNewStream.propTypes = {
  match: PropTypes.shape({
    params: PropTypes.object.isRequired,
  }).isRequired,
  activeConnector: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  updateGraph: PropTypes.func.isRequired,
  enableAddButton: PropTypes.func.isRequired,
  workerClusterName: PropTypes.string.isRequired,
  handleClose: PropTypes.func.isRequired,
  pipelineGroup: PropTypes.string.isRequired,
  brokerClusterName: PropTypes.string.isRequired,
};

export default PipelineNewStream;
