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
import _ from 'lodash';
import clx from 'classnames';
import PropTypes from 'prop-types';

import { LOG_LEVEL } from 'const';
import Row from './LogRowStyles';

const LogRow = ({ rowData: log, style }) => {
  const title = _.get(log, 'title');
  const isError = _.get(log, 'type') === LOG_LEVEL.error;

  return (
    <Row style={style} className={clx({ error: isError })}>
      <div>{title}</div>
    </Row>
  );
};

LogRow.propTypes = {
  rowData: PropTypes.object.isRequired,
  style: PropTypes.object.isRequired,
};

export default LogRow;
