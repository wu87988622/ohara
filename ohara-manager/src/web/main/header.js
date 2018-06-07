oharaManager.widget.header = {
  clickBinding: ["#logout~clickLogout"],
  clickLogout: function(e) {
     window.location.href = "../login/index.html";
  },
  buildItems: function() {
     var cloneHeaderTemplate = oharaManager.template.cloneTemplate("#template .header");
     this.$baseEl.append(cloneHeaderTemplate);
  }
};
