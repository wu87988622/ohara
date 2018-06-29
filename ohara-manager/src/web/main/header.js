oharaManager.widget.header = {
  clickBinding: ["#logout~clickLogout"],
  clickLogout: function(e) {
     window.location.href = "../login/index.html";
  },
  buildItems: function() {
     var headerTemplate = this._template("#template .header");
     this.$baseEl.append(headerTemplate);
  }
};
