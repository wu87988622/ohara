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
import { get } from 'lodash';
import clx from 'classnames';
import Link from '@material-ui/core/Link';
import { getDateFromTimestamp } from 'utils/date';

import Row from './EventLogRowStyles';

const EventLogRow = ({ onClick, rowData: log, style }) => {
  const title = get(log, 'title');
  const isError = get(log, 'type') === 'error';

  return (
    <Row style={style} className={clx({ error: isError })}>
      {(isError && (
        <Link color="error" href="#" onClick={onClick}>
          {title}
        </Link>
      )) || <div>{title}</div>}
      <div className="date">{getDateFromTimestamp(get(log, 'createAt'))}</div>
    </Row>
  );
};

EventLogRow.propTypes = {
  onClick: PropTypes.func,
  rowData: PropTypes.object.isRequired,
  style: PropTypes.object.isRequired,
};

export default EventLogRow;
