oharaManager.widget.menu = {
    clickBinding: ["#jobs~clickJobs", "#account~clickAccount", "#monitor~clickMonitor",
                   "#dishboard~clickDishBoard", "#topic~clickTopic", "#schema~clickSchema"],
    clickJobs: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("JOBS");
    },
    clickAccount: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("Account");
    },
    clickMonitor: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("Monitor");
    },
    clickDishBoard: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("Dishboard");
    },
    clickTopic: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("Topic");
    },
    clickSchema: function(e){
      this.contentPanel.clean();
      this.contentPanel.append("Schema");
    },
    buildItems: function(){
      var cloneToolTemplate = oharaManager.template.cloneTemplate("#template .tool");
      this.$baseEl.append(cloneToolTemplate({toolName: "Jobs", toolID: "jobs", icon: "fas fa-align-left"}))
                      .append(cloneToolTemplate({toolName: "Account", toolID: "account", icon: "fas fa-user"}))
                      .append(cloneToolTemplate({toolName: "Monitor", toolID: "monitor", icon: "fas fa-desktop"}))
                      .append(cloneToolTemplate({toolName: "DishBoard", toolID: "dashboard", icon: "fas fa-chart-bar"}))
                      .append(cloneToolTemplate({toolName: "Topic", toolID: "topic", icon: "fas fa-codepen"}))
                      .append(cloneToolTemplate({toolName: "Schema", toolID: "schema", icon: "fas fa-copy"}));
    }
};
