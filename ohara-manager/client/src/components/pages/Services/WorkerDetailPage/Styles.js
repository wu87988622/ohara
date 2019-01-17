import styled from 'styled-components';
import { FormGroup } from 'common/Form';
import * as CSS_VARS from 'theme/variables';

const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const Text = styled.div`
  width: ${props => props.width || '100%'};
  border: 1px solid ${CSS_VARS.lighterGray};
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  font-size: 12px;
  cursor: not-allowed;
`;

const List = styled.div`
  position: relative;
  width: ${props => props.width || '100%'};
  border: 1px solid ${CSS_VARS.lighterGray};
  padding: 0.5rem 1rem;
  border-radius: 0.25rem;
  min-height: 8rem;
  cursor: not-allowed;
`;

const ListItem = styled.div`
  color: ${CSS_VARS.lightBlue};
  margin: 0.25rem 0;
  font-size: 13px;
`;

const FormRow = styled(FormGroup).attrs({
  isInline: true,
})``;

const FormCol = styled(FormGroup)`
  width: ${props => props.width || '100%'};
`;

export { TopWrapper, List, ListItem, FormRow, FormCol, Text };
