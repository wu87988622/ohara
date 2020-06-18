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
import { min } from 'lodash';
import LinearProgress from '@material-ui/core/LinearProgress';

const Progress = (props) => {
  const { activeIndex, activities } = props.state.context;

  const steps = activities.filter((activity) => !activity.hidden).length;
  const step = activeIndex;

  const value = min([(step / steps) * 100, 100]);

  const valueBuffer = value + (100 / steps) * 0.9;

  return (
    <LinearProgress value={value} valueBuffer={valueBuffer} variant="buffer" />
  );
};

Progress.propTypes = {
  state: PropTypes.shape({
    context: PropTypes.shape({
      activeIndex: PropTypes.number,
      activities: PropTypes.array,
      error: PropTypes.object,
    }).isRequired,
  }).isRequired,
};

export default Progress;
