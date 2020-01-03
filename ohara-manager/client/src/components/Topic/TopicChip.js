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
import Chip from '@material-ui/core/Chip';
import LockIcon from '@material-ui/icons/LockOutlined';
import ShareIcon from '@material-ui/icons/Share';
import { Wrapper } from './TopicChipStyles';

function TopicChip({ isShared }) {
  return (
    <Wrapper>
      <Chip
        variant="outlined"
        size="small"
        icon={isShared ? <ShareIcon /> : <LockIcon />}
        label={isShared ? 'Shared' : 'Private'}
        color={isShared ? 'primary' : 'secondary'}
        className={isShared ? 'shared' : 'private'}
      />
    </Wrapper>
  );
}

TopicChip.propTypes = {
  isShared: PropTypes.bool,
};

TopicChip.defaultProps = {
  isShared: true,
};

export default TopicChip;
