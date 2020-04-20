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

import { Input } from '@material-ui/core';

const DeleteConfirmDialogContent = ({ workspaceName, setWorkspaceName }) => {
  return (
    <>
      <div>
        'This action cannot be undone. This will permanently delete the
        workspace1 zookeeper, broker, worker, and pipelines.'
      </div>
      <Input
        value={workspaceName}
        onChange={event => setWorkspaceName(event.target.value)}
      />
    </>
  );
};

DeleteConfirmDialogContent.propTypes = {
  workspaceName: PropTypes.string.isRequired,
  setWorkspaceName: PropTypes.func.isRequired,
};

export default DeleteConfirmDialogContent;
