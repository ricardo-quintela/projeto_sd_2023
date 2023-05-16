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

function showMessage(message) {

    var lines = message.split("\n");

    $("#barrels").html("");

    for (let i = 0; i < lines.length; i++){
        $("#barrels").append("<tr><td>" + lines[i] + "</td></tr>");
    }

}