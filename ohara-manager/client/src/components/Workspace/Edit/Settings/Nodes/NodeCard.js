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
import clsx from 'classnames';
import { map, reduce } from 'lodash';

import Button from '@material-ui/core/Button';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import Collapse from '@material-ui/core/Collapse';
import Grid from '@material-ui/core/Grid';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';

import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import MoreVertIcon from '@material-ui/icons/MoreVert';

import { useWorkspace } from 'context';
import ResourceItem from './ResourceItem';
import ServiceItem from './ServiceItem';
import ServiceSwitch from './ServiceSwitch';

import { Wrapper } from './NodeCardStyles';

const NodeCard = ({ node }) => {
  const { currentWorker, currentBroker, currentZookeeper } = useWorkspace();

  const [expanded, setExpanded] = React.useState(false);
  const handleExpandClick = () => {
    setExpanded(!expanded);
  };

  const flattenServices = services => {
    return reduce(
      services,
      (result, service) => {
        const { name: serviceName, clusterKeys } = service;
        return [
          ...result,
          ...map(clusterKeys, clusterKey => ({
            name: serviceName,
            clusterKey,
          })),
        ];
      },
      [],
    );
  };

  const services = flattenServices(node.services);

  return (
    <Wrapper>
      <Card>
        <CardHeader
          action={
            <IconButton aria-label="settings">
              <MoreVertIcon />
            </IconButton>
          }
          title={node.hostname}
        />
        <CardContent>
          {map(node.resources, resource => (
            <ResourceItem
              key={`${node.hostname}-${resource.name}`}
              {...resource}
            />
          ))}

          <Grid container justify="space-between" alignItems="center">
            <Grid item xs={6}>
              <Typography>Services</Typography>
            </Grid>
            <Grid item xs={6} className="services">
              <Typography align="right">
                <Button color="primary" onClick={handleExpandClick}>
                  {services.length}
                </Button>{' '}
                / {services.length}{' '}
                <IconButton
                  className={clsx('expand', {
                    'expand-open': expanded,
                  })}
                  onClick={handleExpandClick}
                >
                  <ExpandMoreIcon />
                </IconButton>
              </Typography>
            </Grid>
          </Grid>
          <Collapse in={expanded} className="services-detail">
            {map(services, service => (
              <ServiceItem
                key={`${node.hostname}-${service.name}-${service.clusterKey.name}`}
                {...service}
              />
            ))}
          </Collapse>
        </CardContent>
        <CardActions>
          <Grid container justify="center">
            <Grid item xs={4}>
              <ServiceSwitch
                cluster={currentZookeeper}
                nodeName={node.hostname}
              />
            </Grid>
            <Grid item xs={4}>
              <ServiceSwitch cluster={currentBroker} nodeName={node.hostname} />
            </Grid>
            <Grid item xs={4}>
              <ServiceSwitch cluster={currentWorker} nodeName={node.hostname} />
            </Grid>
          </Grid>
        </CardActions>
      </Card>
    </Wrapper>
  );
};

NodeCard.propTypes = {
  node: PropTypes.shape({
    hostname: PropTypes.string.isRequired,
    services: PropTypes.array,
    resources: PropTypes.array,
  }).isRequired,
};

export default NodeCard;
