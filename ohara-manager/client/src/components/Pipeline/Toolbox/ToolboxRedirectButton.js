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

import React, { forwardRef } from 'react';
import PropTypes from 'prop-types';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import Typography from '@material-ui/core/Typography';

import { Label } from 'components/common/Form';

const ToolboxRedirectButton = forwardRef(({ onClick, buttonText }, ref) => {
  return (
    <div className="add-button" ref={ref}>
      <IconButton onClick={onClick}>
        <Label htmlFor="fileInput">
          <AddIcon />
        </Label>
      </IconButton>
      <Typography variant="subtitle2">{buttonText}</Typography>
    </div>
  );
});

ToolboxRedirectButton.propTypes = {
  buttonText: PropTypes.string.isRequired,
  onClick: PropTypes.func.isRequired,
};

export default ToolboxRedirectButton;
