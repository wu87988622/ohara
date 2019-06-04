describe('API', () => {
  it('Zookeeper', () => {
    cy.fetchZookeepers()
      .its('data')
      .then(data => {
        expect(data.isSuccess).to.eq(true);
        expect(data.result[0]).to.include.keys(
          'clientPort',
          'name',
          'nodeNames',
        );
        expect(data.result[0].clientPort).to.be.a('number');
        expect(data.result[0].name).to.be.a('string');
        expect(data.result[0].nodeNames).to.be.a('array');
      });
  });

  it('Broker', () => {
    cy.fetchBrokers()
      .its('data')
      .then(data => {
        expect(data.isSuccess).to.eq(true);
        expect(data.result[0]).to.include.keys(
          'clientPort',
          'name',
          'nodeNames',
        );
        expect(data.result[0].clientPort).to.be.a('number');
        expect(data.result[0].name).to.be.a('string');
        expect(data.result[0].nodeNames).to.be.a('array');
      });
  });
});
