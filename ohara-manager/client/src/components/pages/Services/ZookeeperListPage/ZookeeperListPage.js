import React from 'react';
import { isEmpty, join, map, get } from 'lodash';

import * as zookeeperApis from 'apis/zookeeperApis';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import TableLoader from 'common/Loader';
import { FormGroup, Input, Label } from 'common/Form';

class ZookeeperListPage extends React.Component {
  state = {
    isLoading: true,
    zookeeper: [],
  };

  componentDidMount() {
    this.fetchZookeepers();
  }

  fetchZookeepers = async () => {
    const res = await zookeeperApis.fetchZookeepers();
    const zookeepers = get(res, 'data.result', []);
    if (!isEmpty(zookeepers)) {
      this.setState({ zookeeper: zookeepers[0] });
    }
    this.setState(() => ({ isLoading: false }));
  };

  getNodeList = () => {
    const { zookeeper } = this.state;
    if (!zookeeper) return [];

    return map(zookeeper.nodeNames, nodeName => {
      return `${nodeName}:${zookeeper.clientPort}`;
    });
  };

  render() {
    const { isLoading } = this.state;
    return (
      <React.Fragment>
        <Box>
          <FormGroup isInline>
            <H2>Services > Zookeeper</H2>
          </FormGroup>
          {isLoading ? (
            <TableLoader />
          ) : (
            <FormGroup isInline>
              <Label style={{ marginRight: '2rem' }}>Zookeeper list</Label>
              <Input
                width="30rem"
                value={join(this.getNodeList(), ', ')}
                data-testid="zookeeper-list"
                disabled
              />
            </FormGroup>
          )}
        </Box>
      </React.Fragment>
    );
  }
}

export default ZookeeperListPage;
