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

import styled from 'styled-components';
import TableCell from '@material-ui/core/TableCell';
import Paper from '@material-ui/core/Paper';
import Icon from '@material-ui/core/Icon';

import { Typography } from 'components/common/Mui/Typography';

export const Wrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
`;

export const LeftColumn = styled.div`
  margin: ${props => props.theme.spacing(3)}px;
  margin-right: ${props => props.theme.spacing(2)}px;
  width: ${props => `calc(50% - ${props.theme.spacing(5)}px)`};
`;

export const RightColumn = styled.div`
  margin: ${props => props.theme.spacing(3)}px;
  margin-left: ${props => props.theme.spacing(2)}px;
  width: ${props => `calc(50% - ${props.theme.spacing(5)}px)`};
`;

export const Box = styled(Paper)`
  margin-bottom: ${props => props.theme.spacing(3)}px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const TabHeading = styled(Typography).attrs({
  variant: 'h3',
})`
  display: flex;
  font-size: 20px;
  color: ${props => props.theme.palette.text.primary};
  border-bottom: 1px solid ${props => props.theme.palette.divider};
  padding: ${props => props.theme.spacing(2)}px;
  margin-bottom: ${props => props.theme.spacing(1)}px;
`;

export const StyledIcon = styled(Icon)`
  font-size: ${props => (props.size ? props.size : '20')}px;
  min-width: 25px;
  margin-right: ${props =>
    props.marginright ? props.marginright : props.theme.spacing(1)}px;
`;

export const List = styled.ul`
  li {
    color: ${props => props.theme.palette.text.primary};
    font-size: 13px;
    padding: ${props => props.theme.spacing(1)}px;
  }
`;

export const StyledTableCell = styled(TableCell)`
  padding: ${props => props.theme.spacing(1)}px;
  border-bottom: none;
`;

export const TooltipBody = styled.ul`
  max-width: 250px;

  li {
    word-break: break-all;
  }
`;

export const StyledIconLink = styled(Icon)`
  margin-left: auto;
  padding: 0;
  cursor: pointer;
  color: ${props => props.theme.palette.text.secondary};
`;
