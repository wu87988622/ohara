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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { get, pick, reject } from 'lodash';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Grid from '@material-ui/core/Grid';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import MoreVertIcon from '@material-ui/icons/MoreVert';

import { useWorkspace, useWorkerActions } from 'context';
import { DeleteDialog } from 'components/common/Dialog';
import { Wrapper } from './PluginCardStyles';

const GridItem = ({ label, value, valueColor = 'initial' }) => (
  <>
    <Grid item xs={3}>
      <Typography>{label}</Typography>
    </Grid>
    <Grid item xs={9}>
      <Typography align="right" component="div" color={valueColor}>
        {value}
      </Typography>
    </Grid>
  </>
);

GridItem.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  valueColor: PropTypes.string,
};

function PluginCard({ plugin }) {
  const [anchorEl, setAnchorEl] = useState(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isDeleting] = useState(false);

  const { currentWorker } = useWorkspace();
  const { stageWorker } = useWorkerActions();

  const workerName = get(currentWorker, 'name');
  const pluginKeys = get(currentWorker, 'stagingSettings.pluginKeys');

  const displayTitle = plugin.name.substring(0, plugin.name.lastIndexOf('.'));

  const getDisplayStatus = status => {
    switch (status) {
      case 'same':
        return 'activated';
      case 'add':
        return 'will be added after restart';
      case 'remove':
        return 'will be removed after restart';
      default:
        return '';
    }
  };

  const getDisplayStatusColor = status => {
    switch (status) {
      case 'add':
        return 'secondary';
      case 'remove':
        return 'secondary';
      default:
        return 'initial';
    }
  };

  const handleMoreClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleMoreClose = () => {
    setAnchorEl(null);
  };

  const handleDeleteClick = async () => {
    const newPluginKeys = reject(pluginKeys, pick(plugin, ['group', 'name']));
    setIsConfirmOpen(false);
    await stageWorker({ name: workerName, pluginKeys: newPluginKeys });
  };

  return (
    <Wrapper>
      <Card>
        <CardHeader
          action={
            <>
              <IconButton onClick={handleMoreClick}>
                <MoreVertIcon />
              </IconButton>
              <Menu
                id="edit-workspace-more-actions-menu"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={handleMoreClose}
              >
                <MenuItem
                  onClick={() => {
                    setIsConfirmOpen(true);
                    handleMoreClose();
                  }}
                >
                  DELETE PLUGIN
                </MenuItem>
              </Menu>
              <DeleteDialog
                title="Delete plugin?"
                content={`Are you sure you want to delete the plugin "${displayTitle}"? This action cannot be undone!`}
                open={isConfirmOpen}
                handleClose={() => setIsConfirmOpen(false)}
                handleConfirm={handleDeleteClick}
                isWorking={isDeleting}
              />
            </>
          }
          title={displayTitle}
        />
        <CardContent>
          <Grid container justify="space-between" alignItems="center">
            <GridItem
              label="Status"
              value={getDisplayStatus(plugin.status)}
              valueColor={getDisplayStatusColor(plugin.status)}
            />
            <GridItem label="File" value={plugin.name} />
            <GridItem label="File size" value={plugin.size} />
          </Grid>
        </CardContent>
      </Card>
    </Wrapper>
  );
}

PluginCard.propTypes = {
  plugin: PropTypes.shape({
    name: PropTypes.string.isRequired,
    size: PropTypes.number.isRequired,
    status: PropTypes.string.isRequired,
  }),
};

export default PluginCard;
