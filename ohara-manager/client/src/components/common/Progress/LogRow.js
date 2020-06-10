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
import cx from 'classnames';
import Link from '@material-ui/core/Link';
import PropTypes from 'prop-types';
import WarningIcon from '@material-ui/icons/Warning';
import CancelIcon from '@material-ui/icons/Cancel';

import { LOG_LEVEL } from 'const';
import Row from './LogRowStyles';

const LogRow = ({ onClick, rowData: log, style }) => {
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
    </Row>
  );
};

LogRow.propTypes = {
  rowData: PropTypes.object.isRequired,
  style: PropTypes.object.isRequired,
  onClick: PropTypes.func,
};

export default LogRow;
