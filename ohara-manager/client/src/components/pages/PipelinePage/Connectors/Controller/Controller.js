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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import Tooltip from '@material-ui/core/Tooltip';
import { noop, includes } from 'lodash';

import { DeleteDialog } from 'components/common/Mui/Dialog';
import * as s from './styles';

const START = 'start';
const STOP = 'stop';
const DELETE = 'delete';

const Controller = props => {
  const [isDeleteModalActive, setIsDeleteModalActive] = useState(false);

  const {
    onStart = noop,
    onStop = noop,
    onDelete = noop,
    show = [START, STOP, DELETE],
    disable = [],
    kind,
    connectorName,
  } = props;

  const handleDeleteModalOpen = e => {
    e.preventDefault();
    setIsDeleteModalActive(true);
  };

  const handleDeleteModalClose = () => {
    setIsDeleteModalActive(false);
  };

  const handleDeleteClick = e => {
    e.preventDefault();
    onDelete();
    handleDeleteModalClose();
  };

  return (
    <s.Controller>
      {includes(show, START) && (
        <Tooltip title={`Start ${kind}`} enterDelay={1000}>
          <s.ControlButton
            onClick={onStart}
            data-testid="start-button"
            disabled={includes(disable, START)}
          >
            <i className="far fa-play-circle" />
          </s.ControlButton>
        </Tooltip>
      )}
      {includes(show, STOP) && (
        <Tooltip title={`Stop ${kind}`} enterDelay={1000}>
          <s.ControlButton
            onClick={onStop}
            data-testid="stop-button"
            disabled={includes(disable, STOP)}
            isDanger
          >
            <i className="far fa-stop-circle" />
          </s.ControlButton>
        </Tooltip>
      )}
      {includes(show, DELETE) && (
        <Tooltip title={`Delete ${kind}`} enterDelay={1000}>
          <s.ControlButton
            onClick={e => {
              handleDeleteModalOpen(e);
            }}
            data-testid="delete-button"
            disabled={includes(disable, DELETE)}
            isDanger
          >
            <i className="far fa-trash-alt" />
          </s.ControlButton>
        </Tooltip>
      )}

      <DeleteDialog
        title={`Remove ${kind}?`}
        content={`Are you sure you want to remove the ${kind}: ${connectorName} from the pipeline graph? This action cannot be undone!`}
        open={isDeleteModalActive}
        handleConfirm={handleDeleteClick}
        handleClose={handleDeleteModalClose}
      />
    </s.Controller>
  );
};

Controller.propTypes = {
  kind: PropTypes.string.isRequired,
  connectorName: PropTypes.string.isRequired,
  onStart: PropTypes.func,
  onStop: PropTypes.func,
  onDelete: PropTypes.func,
  show: PropTypes.arrayOf(PropTypes.oneOf([START, STOP, DELETE])),
  disable: PropTypes.arrayOf(PropTypes.oneOf([START, STOP, DELETE])),
};

export default Controller;
