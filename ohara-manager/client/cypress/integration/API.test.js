import { makeRandomPort } from '../utils';

let brokerClusterName = '';
let jarID = '';

describe('Zookeeper Api test', () => {
  it('fetchZookeepers', () => {
    cy.fetchZookeepers().then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('clientPort', 'name', 'nodeNames');
      expect(data.result[0].clientPort).to.be.a('number');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
    });
  });
});

describe('Broker Api test', () => {
  it('fetchBrokers', () => {
    cy.fetchBrokers().then(res => {
      const data = res.data;
      brokerClusterName = data.result[0].name;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('clientPort', 'name', 'nodeNames');
      expect(data.result[0].clientPort).to.be.a('number');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
    });
  });
});

describe('Jar Api test', () => {
  const testJarName = 'ohara-it-source.jar';
  it('createJar', () => {
    cy.createJar(testJarName).then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
    });
  });
  it('fetchJars', () => {
    cy.fetchJars().then(res => {
      const data = res.data;
      jarID = data.result[0].id;
      expect(data.isSuccess).to.eq(true);
      expect(data.result[0]).to.include.keys('name', 'id');
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].id).to.be.a('string');
    });
  });
});

describe('Worker Api test', () => {
  const wkName = `wk${makeRandomPort()}`;
  it('createWorker', () => {
    const data = {
      name: wkName,
      jmxPort: makeRandomPort(),
      brokerClusterName: brokerClusterName,
      clientPort: makeRandomPort(),
      nodeNames: [Cypress.env('nodeHost')],
      plugins: [jarID],
    };
    cy.testCreateWorker(data).then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'name',
        'clientPort',
        'nodeNames',
        'sources',
        'sinks',
        'jarNames',
        'configTopicName',
        'offsetTopicName',
        'statusTopicName',
      );
      expect(data.result.name).to.be.a('string');
      expect(data.result.clientPort).to.be.a('number');
      expect(data.result.nodeNames).to.be.a('array');
      expect(data.result.sources).to.be.a('array');
      expect(data.result.sinks).to.be.a('array');
      expect(data.result.jarNames).to.be.a('array');
      expect(data.result.configTopicName).to.be.a('string');
      expect(data.result.offsetTopicName).to.be.a('string');
      expect(data.result.statusTopicName).to.be.a('string');
    });
  });
  it('fetchWorker', () => {
    cy.fetchWorker(wkName).then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result.name).to.be.a('string');
      expect(data.result.clientPort).to.be.a('number');
      expect(data.result.nodeNames).to.be.a('array');
      expect(data.result.sources).to.be.a('array');
      expect(data.result.sinks).to.be.a('array');
      expect(data.result.jarNames).to.be.a('array');
    });
  });
  it('fetchWorkers', () => {
    cy.fetchWorkers().then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'name',
        'nodeNames',
        'configTopicName',
        'offsetTopicName',
        'statusTopicName',
      );
      expect(data.result[0].name).to.be.a('string');
      expect(data.result[0].nodeNames).to.be.a('array');
      expect(data.result[0].configTopicName).to.be.a('string');
      expect(data.result[0].offsetTopicName).to.be.a('string');
      expect(data.result[0].statusTopicName).to.be.a('string');
    });
  });
});

describe('Topic Api test', () => {
  const tpName = `tp${makeRandomPort()}`;
  it('CreateTopic', () => {
    const data = {
      name: tpName,
      numberOfPartitions: 1,
      brokerClusterName: brokerClusterName,
      numberOfReplications: 1,
    };
    cy.testCreateTopic(data).then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.numberOfPartitions).to.be.a('number');
      expect(data.result.numberOfReplications).to.be.a('number');
    });
  });
  it('fetchTopic', () => {
    cy.fetchTopic(tpName).then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result.id).to.be.a('string');
      expect(data.result.numberOfPartitions).to.be.a('number');
      expect(data.result.numberOfReplications).to.be.a('number');
    });
  });
  it('fetchTopics', () => {
    cy.fetchTopics().then(res => {
      const data = res.data;
      expect(data.isSuccess).to.eq(true);
      expect(data.result).to.be.a('array');
      expect(data.result[0]).to.include.keys(
        'id',
        'numberOfPartitions',
        'numberOfReplications',
      );
      expect(data.result[0].id).to.be.a('string');
      expect(data.result[0].numberOfPartitions).to.be.a('number');
      expect(data.result[0].numberOfReplications).to.be.a('number');
    });
  });
});
