import { makeRandomPort } from '../utils';

let brokerClusterName = '';

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

describe.only('Jar Api test', () => {
  it('createJar', () => {
    cy.createJar().then(res => {
      cy.log(res);
    });
  });
});

describe('Worker Api test', () => {
  it('createWorker', () => {
    const data = {
      name: 'wk05',
      jmxPort: makeRandomPort(),
      brokerClusterName: brokerClusterName,
      clientPort: makeRandomPort(),
      nodeNames: [Cypress.env('nodeHost')],
      jars: [],
    };

    cy.testCreateWorker(data).then(res => {
      const data = res.data;
      cy.log(data);
      expect(data.isSuccess).to.eq(true);
    });
  });
});
