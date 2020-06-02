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
import styled, { css } from 'styled-components';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import CardActionArea from '@material-ui/core/CardActionArea';
import Typography from '@material-ui/core/Typography';

import PowerIcon from '@material-ui/icons/Power';
import Dropzone from 'react-dropzone';

const FileCard = (props) => {
  const { handelDrop, sm = false, title, content, values } = props;
  const StyledPluginsCard = styled(Card)(
    ({ theme }) => css`
      display: flex;
      align-items: center;
      justify-content: center;
      height: ${sm ? theme.spacing(25) : 240}px;

      .MuiCardContent-root {
        display: flex;
      }

      .action-icon {
        font-size: 50px;
        margin-right: ${theme.spacing(2)}px;
      }

      .title {
        margin-bottom: ${theme.spacing(1)}px;
      }

      .action-description {
        width: 145px;
      }
    `,
  );

  const StyledCardActionArea = styled(CardActionArea)(
    ({ theme }) => css`
      width: ${sm && theme.spacing(33)}px;
      height: ${sm && theme.spacing(25)}px;
      float: ${sm && 'left'};
      margin: ${sm && theme.spacing(1.5)}px;
    `,
  );

  return (
    <StyledCardActionArea>
      <Dropzone onDrop={(file) => handelDrop(file, values)}>
        {({ getRootProps, getInputProps }) => (
          <section>
            <div {...getRootProps()}>
              <input {...getInputProps()} />
              <StyledPluginsCard>
                <CardContent>
                  <PowerIcon className="action-icon" color="action" />
                  <div className="action-description">
                    <Typography className="title" variant="h5">
                      {title}
                    </Typography>
                    <Typography variant="body2">{content}</Typography>
                  </div>
                </CardContent>
              </StyledPluginsCard>
            </div>
          </section>
        )}
      </Dropzone>
    </StyledCardActionArea>
  );
};

FileCard.propTypes = {
  handelDrop: PropTypes.func.isRequired,
  sm: PropTypes.bool,
  title: PropTypes.string.isRequired,
  content: PropTypes.string.isRequired,
  values: PropTypes.object,
};

export default FileCard;
