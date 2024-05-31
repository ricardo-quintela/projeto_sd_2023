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

    $("#barrels").html("<h1>Barrels</h1>");
    $("#downloaders").html("<h1>Downloaders</h1>");
    $("#search-results").html("<h1>Pesquisas mais Frequentes</h1>");

    for (let i = 0; i < barrels.length; i++) {
        const host = barrels[i].split('/');

        if (host[0][host[0].length - 1] % 2 === 0){
            $("#barrels").append("<p>" + barrels[i] + "- Partição: A-M</p>");
        } else {
            $("#barrels").append("<p>" + barrels[i] + "- Partição: N-Z</p>");
        }
        // Faça o que desejar com cada elemento do array 'barrels'
    }
    for (let i = 0; i < downloaders.length; i++) {
        $("#downloaders").append("<p>" + downloaders[i] + "</p>");
        // Faça o que desejar com cada elemento do array 'barrels'
    }
    for (let i = 0; i < pesquisas.length; i++) {
        $("#search-results").append("<p>" + pesquisas[i] + "</p>");
        // Faça o que desejar com cada elemento do array 'barrels'
    }
}