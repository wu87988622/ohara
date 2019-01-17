import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { find, join } from 'lodash';

import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import { H2 } from 'common/Headings';

const Text = styled(Label)`
  margin-left: 0.5rem;
`;

class WorkerDetailPage extends React.Component {
  static propTypes = {
    workers: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        clientPort: PropTypes.number.isRequired,
        nodeNames: PropTypes.arrayOf(PropTypes.string).isRequired,
        jarNames: PropTypes.arrayOf(PropTypes.string),
      }),
    ).isRequired,
    name: PropTypes.string.isRequired,
  };

  render() {
    const { workers, name } = this.props;
    const worker = find(workers, { name });
    if (!worker) return null;

    return (
      <React.Fragment>
        <Box shadow={false}>
          <FormGroup>
            <H2>Services > {worker.name}</H2>
          </FormGroup>
          <FormGroup isInline>
            <Label>Port:</Label>
            <Text>{worker.clientPort}</Text>
          </FormGroup>
          <FormGroup isInline>
            <Label>Node List:</Label>
            <Text>{join(worker.nodeNames, ', ')}</Text>
          </FormGroup>
          <FormGroup isInline>
            <Label>Plugin List:</Label>
            <Text>{join(worker.jarNames, ', ')}</Text>
          </FormGroup>
        </Box>
      </React.Fragment>
    );
  }
}

export default WorkerDetailPage;
