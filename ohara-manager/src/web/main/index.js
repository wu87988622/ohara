$(document).ready(function() {
   var headerPanel = $("#header").widget($.extend({"autoBinding": false}, oharaManager.widget.header));
   headerPanel.buildItems();
   headerPanel.bindClickEvent();

   var contentHeaderPanel = $("#content-header .h2").widget(oharaManager.widget.contentHeader);
   var contentPanel = $("#content").widget(oharaManager.widget.content);
   var cfg = $.extend({
       "contentHeaderPanel": contentHeaderPanel,
       "contentPanel": contentPanel,
       autoBinding: false
   }, oharaManager.widget.menu);
   var menu = $("#toolGroup1").widget(cfg);
   menu.buildItems();
   menu.bindClickEvent();
});
