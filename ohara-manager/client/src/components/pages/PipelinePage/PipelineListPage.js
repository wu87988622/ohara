import React from 'react';
import PropTypes from 'prop-types';
import DocumentTitle from 'react-document-title';
import styled from 'styled-components';
import toastr from 'toastr';
import { Link } from 'react-router-dom';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import { ConfirmModal } from 'common/Modal';
import { DataTable } from 'common/Table';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { Button } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';
import { PIPELINE } from 'constants/documentTitles';
import { lightBlue, blue, red, trBgColor } from 'theme/variables';
import { addPipelineStatus, getEditUrl } from 'utils/pipelineListPageUtils';
import {
  fetchPipelines,
  createPipeline,
  deletePipeline,
} from 'apis/pipelinesApis';

const Wrapper = styled.div`
  padding-top: 75px;
  max-width: 1200px;
  width: calc(100% - 100px);
  margin: auto;
`;

const TopWrapper = styled.div`
  margin-bottom: 20px;
  display: flex;
  align-items: center;
`;

const NewPipelineBtn = styled(Button)`
  margin-left: auto;
`;

NewPipelineBtn.displayName = 'NewPipelineBtn';

const Table = styled(DataTable)`
  text-align: center;

  .is-running {
    background: ${trBgColor};
  }
`;

Table.displayName = 'Table';

const LinkIcon = styled(Link)`
  color: ${lightBlue};

  &:hover {
    color: ${blue};
  }
`;

LinkIcon.displayName = 'LinkIcon';

const DeleteIcon = styled.button`
  color: ${lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;

  &:hover {
    color: ${red};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

class PipelineListPage extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  headers = ['#', 'name', 'status', 'edit', 'delete'];
  state = {
    isSelectTopicModalActive: false,
    isDeletePipelineModalActive: false,
    pipelines: [],
    currentTopic: {},
  };

  componentDidMount() {
    this.fetchPipelines();
  }

  fetchPipelines = async () => {
    const res = await fetchPipelines();
    const result = _.get(res, 'data.result', null);

    if (!result) return;

    const pipelines = addPipelineStatus(result);
    this.setState({ pipelines });
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { id } = target.options[selectedIdx].dataset;

    this.setState({
      currentTopic: {
        name: target.value,
        id,
      },
    });
  };

  handleNewPipeline = async () => {
    const { history, match } = this.props;
    const params = { name: 'Untitled pipeline', rules: {} };
    const res = await createPipeline(params);

    const pipelineId = _.get(res, 'data.result.id', null);

    if (pipelineId) {
      toastr.success(MESSAGES.PIPELINE_CREATION_SUCCESS);
      history.push(`${match.url}/new/${pipelineId}`);
    }
  };

  handleDeletePipelineModalOpen = id => {
    this.setState({
      isDeletePipelineModalActive: true,
      deletePipelineId: id,
    });
  };

  handleDeletePipelineModalClose = () => {
    this.setState({
      isDeletePipelineModalActive: false,
      deletePipelineId: '',
    });
  };

  handleDeletePipelineConfirm = async () => {
    const { deletePipelineId: id } = this.state;
    const res = await deletePipeline(id);
    const deletedId = _.get(res, 'data.result.id', null);
    const deletedPipeline = _.get(res, 'data.result.name', null);

    if (deletedId) {
      this.setState(({ pipelines }) => {
        const _pipelines = pipelines.filter(p => p.id !== deletedId);
        return {
          pipelines: _pipelines,
          isDeletePipelineModalActive: false,
          deletePipelineId: '',
        };
      });
      toastr.success(
        `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${deletedPipeline}`,
      );
    } else {
      toastr.error(`${MESSAGES.PIPELINE_DELETION_ERROR} ${deletedPipeline}`);
    }
  };

  render() {
    const { match } = this.props;
    const { isDeletePipelineModalActive, pipelines } = this.state;

    return (
      <DocumentTitle title={PIPELINE}>
        <React.Fragment>
          <ConfirmModal
            isActive={isDeletePipelineModalActive}
            title="Delete pipeline?"
            confirmBtnText="Yes, Delete this pipeline"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeletePipelineModalClose}
            handleConfirm={this.handleDeletePipelineConfirm}
            message="Are you sure you want to delete this pipeline? This action cannot be redo!"
            isDelete
          />

          <Wrapper>
            <TopWrapper>
              <H2>Pipelines</H2>
              <NewPipelineBtn
                theme={primaryBtn}
                text="New pipeline"
                data-testid="new-pipeline"
                handleClick={this.handleNewPipeline}
              />
            </TopWrapper>
            <Box>
              <Table headers={this.headers}>
                {pipelines.map((pipeline, idx) => {
                  const { id, name, status } = pipeline;
                  const isRunning = status === 'Running' ? true : false;

                  const trCls = isRunning ? 'is-running' : '';
                  const editUrl = getEditUrl(pipeline, match);

                  return (
                    <tr key={id} className={trCls}>
                      <td>{idx}</td>
                      <td>{name}</td>
                      <td>{status}</td>
                      <td className="has-icon">
                        <LinkIcon to={editUrl}>
                          <i className="far fa-edit" />
                        </LinkIcon>
                      </td>
                      <td data-testid="delete-pipeline" className="has-icon">
                        <DeleteIcon
                          onClick={() => this.handleDeletePipelineModalOpen(id)}
                        >
                          <i className="far fa-trash-alt" />
                        </DeleteIcon>
                      </td>
                    </tr>
                  );
                })}
              </Table>
            </Box>
          </Wrapper>
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default PipelineListPage;
