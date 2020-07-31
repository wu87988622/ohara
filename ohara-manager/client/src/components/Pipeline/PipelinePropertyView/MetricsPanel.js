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
import Typography from '@material-ui/core/Typography';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import Accordion from '@material-ui/core/Accordion';
import SignalCellularAltIcon from '@material-ui/icons/SignalCellularAlt';
import AccordionDetails from '@material-ui/core/AccordionDetails';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import { isEmpty } from 'lodash';

import PropertyField from './PipelinePropertyViewField';
import { Wrapper } from './MetricsPanelStyles';
import { PipelineStateContext } from '../Pipeline';

const MetricsPanel = (props) => {
  const { isMetricsOn, currentCellName } = props;
  const [isExpanded, setIsExpanded] = React.useState(false);
  const { objects } = React.useContext(PipelineStateContext);

  const findByCellName = ({ name }) => name === currentCellName;
  const metrics = objects.find(findByCellName)?.nodeMetrics || {};
  const hasMetrics = Object.keys(metrics).some(
    (key) => metrics[key].meters.length > 0,
  );

  if (!hasMetrics || !isMetricsOn) return null;

  return (
    <Wrapper>
      <Accordion
        data-testid="metrics-panel"
        defaultExpanded={true}
        expanded={isExpanded}
      >
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          onClick={() => setIsExpanded((prevState) => !prevState)}
        >
          <SignalCellularAltIcon fontSize="small" />
          <Typography className="section-title" variant="h5">
            Metrics
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          {Object.keys(metrics)
            .sort()
            .map((key) => {
              // Don't render the node title if it doesn't contain any metrics data
              if (isEmpty(metrics[key].meters)) return null;

              return (
                <React.Fragment key={key}>
                  <Typography variant="h6">{key}</Typography>
                  {metrics[key].meters.map((meter, index) => {
                    const { document, value, unit } = meter;
                    return (
                      <PropertyField
                        key={index}
                        label={document}
                        slot={
                          <Typography
                            className="metrics-unit"
                            component="span"
                            variant="body2"
                          >
                            {unit}
                          </Typography>
                        }
                        value={value}
                      />
                    );
                  })}
                </React.Fragment>
              );
            })}
        </AccordionDetails>
      </Accordion>
    </Wrapper>
  );
};

MetricsPanel.propTypes = {
  isMetricsOn: PropTypes.bool.isRequired,
  currentCellName: PropTypes.string.isRequired,
};

export default MetricsPanel;
