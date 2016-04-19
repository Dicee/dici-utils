function ajaxRestApiCall(url, callback, onerror) {
    ajaxCall(url, callback, function(response) { return JSON.parse(response); }, onerror);
}

function ajaxCall(url, callback, parser, onerror) {
    parser  = parser || defaultParser();
    onerror = onerror || defaultOnerror();

    var xhttp = new XMLHttpRequest();
    xhttp.open("GET", url, true);
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState == 4 && xhttp.status == 200) {
            callback(parser(xhttp.responseText));
        } else if (xhttp.status == 202) {
            setTimeout(function() { ajaxCall(url, callback, parser, onerror); }, 50)
        } else if (xhttp.readyState == 4) {
            onerror(parser(xhttp.responseText));
        }
    };
    xhttp.send();
}

function defaultOnerror(response) { console.error(error); }
function defaultParser (response) { return response     ; }