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
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';

import MetricsListItem from './MetricsListItem';
import {
  StyledH4,
  ExpandIcon,
  StyledExpansionPanel,
  MetricsList,
} from './styles';

const TopicMetrics = props => {
  const { topicName, meters } = props;

  const previewMeters = [];
  const remainingMeters = [];

  meters.forEach((meter, index) => {
    if (index < 2) {
      previewMeters.push(meter);
    } else {
      remainingMeters.push(meter);
    }
  });

  return (
    <StyledExpansionPanel>
      <ExpansionPanelSummary
        expandIcon={<ExpandIcon className="fas fa-chevron-down" />}
      >
        <StyledH4>
          <i className="fas fa-tachometer-alt"></i>
          {`Metrics (${topicName})`}
        </StyledH4>

        <MetricsList>
          {previewMeters.map(meter => (
            <MetricsListItem key={meter.document} meter={meter} />
          ))}
        </MetricsList>
      </ExpansionPanelSummary>

      <ExpansionPanelDetails>
        <MetricsList>
          {remainingMeters.map(meter => (
            <MetricsListItem key={meter.document} meter={meter} />
          ))}
        </MetricsList>
      </ExpansionPanelDetails>
    </StyledExpansionPanel>
  );
};

TopicMetrics.propTypes = {
  topicName: PropTypes.string.isRequired,
  meters: PropTypes.arrayOf(
    PropTypes.shape({
      document: PropTypes.string.isRequired,
      value: PropTypes.number.isRequired,
      unit: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
};

export default TopicMetrics;
