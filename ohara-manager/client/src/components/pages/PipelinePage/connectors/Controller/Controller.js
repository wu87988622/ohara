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
import ReactTooltip from 'react-tooltip';

import { ConfirmModal } from 'common/Modal';
import * as s from './Styles';

class Controller extends React.Component {
  static propTypes = {
    kind: PropTypes.string.isRequired,
    onStart: PropTypes.func.isRequired,
    onStop: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
  };

  state = {
    isDeleteModalActive: false,
  };

  handleDeleteModalOpen = e => {
    e.preventDefault();
    this.setState({ isDeleteModalActive: true });
  };

  handleDeleteModalClose = () => {
    this.setState({ isDeleteModalActive: false });
  };

  handleDeleteClick = e => {
    e.preventDefault();
    this.props.onDelete();
    this.handleDeleteModalClose();
  };

  render() {
    const { kind, onStart, onStop } = this.props;
    const { isDeleteModalActive } = this.state;
    return (
      <s.Controller>
        <s.ControlButton
          data-tip={`Start ${kind}`}
          onClick={onStart}
          data-testid="start-button"
        >
          <i className={`fa fa-play-circle`} />
        </s.ControlButton>
        <s.ControlButton
          data-tip={`Stop ${kind}`}
          onClick={onStop}
          data-testid="stop-button"
        >
          <i className={`fa fa-stop-circle`} />
        </s.ControlButton>
        <s.ControlButton
          data-tip={`Delete ${kind}`}
          onClick={e => {
            this.handleDeleteModalOpen(e);
          }}
          data-testid="trash-button"
        >
          <i className={`fa fa-trash-alt`} />
        </s.ControlButton>

        <ReactTooltip />
        <ConfirmModal
          isActive={isDeleteModalActive}
          title={`Delete ${kind}?`}
          confirmBtnText={`Yes, Delete this ${kind}`}
          cancelBtnText="No, Keep it"
          handleCancel={this.handleDeleteModalClose}
          handleConfirm={this.handleDeleteClick}
          message={`Are you sure you want to delete this ${kind}? This action cannot be redo!`}
          isDelete
        />
      </s.Controller>
    );
  }
}

export default Controller;
