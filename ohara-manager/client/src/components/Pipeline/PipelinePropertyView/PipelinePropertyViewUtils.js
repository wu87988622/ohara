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
import WavesIcon from '@material-ui/icons/Waves';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import StorageIcon from '@material-ui/icons/Storage';

import { AddSharedTopicIcon } from 'components/common/Icon';
import { KIND } from 'const';

export const renderIcon = ({ kind, isShared }) => {
  switch (kind) {
    case KIND.source:
      return <FlightTakeoffIcon color="action" />;
    case KIND.sink:
      return <FlightLandIcon color="action" />;
    case KIND.stream:
      return <WavesIcon color="action" />;
    case KIND.topic:
      if (isShared) {
        return <AddSharedTopicIcon width={20} height={20} />;
      } else {
        return <StorageIcon color="action" />;
      }
    default:
      break;
  }
};

export const getDisplayName = (key, defs) => {
  const setting = defs.find(def => def.key === key);
  return setting ? setting.displayName : key;
};
