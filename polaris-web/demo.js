var thrift = require('thrift');
var Search = require('./gen-nodejs/TCodeSearchService');
var ttypes = require('./gen-nodejs/search_types');

var connection = thrift.createConnection('localhost', 5000);
var client = thrift.createClient(Search, connection);

var request = new ttypes.TSearchRequest({
    query: 'com.fenbi.ape.data.Answer',
    rankFrom: 0,
    rankTo: 100
});

client.search(request, function (err, data) {
    if (err) {
        console.log("err:", err);
    } else {
        console.log("data:", data);
    }

    connection.end();
});
