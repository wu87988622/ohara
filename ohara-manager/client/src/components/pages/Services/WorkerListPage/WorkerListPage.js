import React from 'react';
import PropTypes from 'prop-types';
import { join } from 'lodash';

import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { FormGroup } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import TableLoader from 'common/Loader';

import WorkerNewModal from '../WorkerNewModal';
import * as s from './Styles';

class WorkerListPage extends React.Component {
  static propTypes = {
    workers: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        nodeNames: PropTypes.arrayOf(PropTypes.string).isRequired,
        statusTopicName: PropTypes.string.isRequired,
        configTopicName: PropTypes.string.isRequired,
        offsetTopicName: PropTypes.string.isRequired,
      }),
    ).isRequired,
    newWorkerSuccess: PropTypes.func.isRequired,
    isLoading: PropTypes.bool,
  };

  headers = ['SERVICES', 'NODES', 'TOPICS'];

  state = {
    isModalOpen: false,
  };

  render() {
    const { workers, newWorkerSuccess, isLoading } = this.props;
    const { isModalOpen } = this.state;
    return (
      <React.Fragment>
        <Box>
          <FormGroup isInline>
            <H2>Services > Connect</H2>
            <s.NewNodeBtn
              theme={primaryBtn}
              text="New Cluster"
              data-testid="new-cluster"
              handleClick={() => {
                this.setState({ isModalOpen: true });
              }}
            />
          </FormGroup>
          {isLoading ? (
            <TableLoader />
          ) : (
            <s.Table headers={this.headers}>
              {workers.map(worker => (
                <tr key={worker.name}>
                  <td>
                    <s.Link to={`/services/workers/${worker.name}`}>
                      {worker.name || ''}
                    </s.Link>
                  </td>
                  <td>{join(worker.nodeNames, ', ')}</td>
                  <td>
                    {worker.statusTopicName && (
                      <div>status-topic: {worker.statusTopicName}</div>
                    )}
                    {worker.configTopicName && (
                      <div>config-topic: {worker.configTopicName}</div>
                    )}
                    {worker.offsetTopicName && (
                      <div>offset-topic: {worker.offsetTopicName}</div>
                    )}
                  </td>
                </tr>
              ))}
            </s.Table>
          )}
        </Box>
        <WorkerNewModal
          isActive={isModalOpen}
          onClose={() => {
            this.setState({ isModalOpen: false });
          }}
          onConfirm={newWorkerSuccess}
        />
      </React.Fragment>
    );
  }
}

export default WorkerListPage;
