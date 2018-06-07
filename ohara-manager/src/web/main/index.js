$(document).ready(function() {
   var headerPanel = $("#header").widget($.extend({"autoBinding": false}, oharaManager.widget.header));
   headerPanel.buildItems();
   headerPanel.bindClickEvent();

   var contentPanel = $("#content-wrap .h2").widget(oharaManager.widget.content);
   var cfg = $.extend({
       "contentPanel": contentPanel,
       autoBinding: false
   }, oharaManager.widget.menu);
   var menu = $("#toolGroup1").widget(cfg);
   menu.buildItems();
   menu.bindClickEvent();
});
