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

    const lines = message.split('\n');
    const barrels = [];
    const downloaders = [];
    const pesquisas = [];

    let currentArray = null;

    for (const line of lines) {
        if (line.startsWith('BARRELS:')) {
            currentArray = barrels;
        } else if (line.startsWith('DOWNLOADERS:')) {
            currentArray = downloaders;
        } else if (line.startsWith('PESQUISAS:')) {
            currentArray = pesquisas;
        } else if (currentArray !== null) {
            currentArray.push(line.trim());
        }
    }

    $("#info").html("");

    $("#info").append("<p> BARRELS </p>");
    for (let i = 0; i < barrels.length; i++) {
        if (barrels[i][barrels[i].length - 1] % 2 === 0){
            $("#info").append("<p>" + barrels[i] + "- Partição: A-M</p>");
        } else {
            $("#info").append("<p>" + barrels[i] + "- Partição: N-Z</p>");
        }
        // Faça o que desejar com cada elemento do array 'barrels'
    }
    $("#info").append("<p> DOWNLOADERS </p>");
    for (let i = 0; i < downloaders.length; i++) {
        $("#info").append("<p>" + downloaders[i] + "</p>");
        // Faça o que desejar com cada elemento do array 'barrels'
    }
    $("#info").append("<p> PESQUISAS </p>");
    for (let i = 0; i < pesquisas.length; i++) {
        $("#info").append("<p>" + pesquisas[i] + "</p>");
        // Faça o que desejar com cada elemento do array 'barrels'
    }
}