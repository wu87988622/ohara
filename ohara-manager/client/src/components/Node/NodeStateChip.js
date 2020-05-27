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
import { capitalize } from 'lodash';

import Chip from '@material-ui/core/Chip';
import CancelIcon from '@material-ui/icons/Cancel';
import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import HelpIcon from '@material-ui/icons/Help';
import Tooltip from '@material-ui/core/Tooltip';

import { NODE_STATE } from 'api/apiInterface/nodeInterface';

const NodeStateChip = ({ node }) => {
  if (node?.state === NODE_STATE.AVAILABLE) {
    return (
      <Chip
        variant="outlined"
        color="primary"
        icon={<CheckCircleIcon />}
        label={capitalize(node.state)}
        size="small"
      />
    );
  } else if (node?.state === NODE_STATE.UNAVAILABLE) {
    return (
      <Tooltip title={node?.error}>
        <Chip
          variant="outlined"
          color="secondary"
          icon={<CancelIcon />}
          label={capitalize(node.state)}
          size="small"
        />
      </Tooltip>
    );
  } else {
    return (
      <Chip
        variant="outlined"
        icon={<HelpIcon />}
        label="Unknown"
        size="small"
      />
    );
  }
};

NodeStateChip.propTypes = {
  node: PropTypes.shape({
    error: PropTypes.string,
    state: PropTypes.string,
  }).isRequired,
};

NodeStateChip.defaultProps = {
  node: null,
};

export default NodeStateChip;
