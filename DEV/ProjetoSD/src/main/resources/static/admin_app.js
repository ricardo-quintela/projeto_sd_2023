var stompClient = null;


function connect() {
    var socket = new SockJS('/admin');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/topic/info', function (message) {
            showMessage(message.body);
        });
    });
}


function disconnect(){
    if (stompClient != null){
        stompClient.disconnect();
    }
}


function showMessage(message) {

    var lines = message.split("\n");

    $("#info").html("");

    for (let i = 0; i < lines.length; i++){
        $("#info").append("<p>" + lines[i] + "</p>");
    }

}