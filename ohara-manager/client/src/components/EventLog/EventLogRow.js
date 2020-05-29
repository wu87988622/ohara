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
import cx from 'classnames';
import Link from '@material-ui/core/Link';
import WarningIcon from '@material-ui/icons/Warning';
import CancelIcon from '@material-ui/icons/Cancel';

import Row from './EventLogRowStyles';
import { getDateFromTimestamp } from 'utils/date';
import { LOG_LEVEL } from 'const';

const EventLogRow = ({ onClick, rowData: log, style }) => {
  const title = log?.title;
  const message = log?.payload?.error?.message || '';
  const logType = log?.type;
  const isError = logType === LOG_LEVEL.error;
  const isWarning = logType === LOG_LEVEL.warning;
  const isInfo = logType === LOG_LEVEL.info;

  const classNames = cx({
    error: isError,
    warning: isWarning,
    info: isInfo,
  });

  return (
    <Row className={classNames} style={style}>
      {isError ? (
        <Link onClick={onClick}>
          <CancelIcon className="log-icon" />
          <span className="log-content">
            {title} {message && `--> ${message}`}
          </span>
        </Link>
      ) : isWarning ? (
        <>
          <WarningIcon className="log-icon" />
          <span className="log-content">
            {title} {message && `--> ${message}`}
          </span>
        </>
      ) : (
        <span className="log-content">{title}</span>
      )}
      <div className="date">{getDateFromTimestamp(log?.createAt)}</div>
    </Row>
  );
};

EventLogRow.propTypes = {
  onClick: PropTypes.func,
  rowData: PropTypes.object.isRequired,
  style: PropTypes.object.isRequired,
};

export default EventLogRow;
