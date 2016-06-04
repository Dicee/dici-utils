function parseQueryString() {
    var params = {};
    location.search.substr(1).split("&").forEach(function(item) { var keyValue = item.split("="); params[keyValue[0]] = keyValue[1]; });
    return params;
}

function debug(...args) { console.log(...args.map(function(x) { return x.debug !== undefined ? x.debug() : JSON.stringify(x); })); }