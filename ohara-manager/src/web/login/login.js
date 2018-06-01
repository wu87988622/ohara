

oharaManager.widget.login = {
    clickBinding: ["span.btn~clickLogin"],
    getUsername: function(){
        return this.$val("#username");
    },
    getPassword: function() {
        return this.$val("#password");
    },
    clickLogin: function(e){
        this.login(this.getUsername(), this.getPassword());
    },
    onSuccess: function(username, islogin) {
        if (islogin) {
          alert(username + " login success.");
        } else {
          alert(username + " login failed.");
        }
    },
    onFail: function(errorMessage) {
        console.log(errorMessage)
    },
    login: function(username, password) {
        console.log( username, "/", password);
        oharaManager.api.login(username, password, this.onSuccess, this.onFail);
    }
};


$(document).ready(function() {

    var loginWidget = $(".form-login").widget(oharaManager.widget.login);

    //$("#form-login").oharaWidget();
});