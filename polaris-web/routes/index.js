/*
 * GET home page.
 */

var thrift = require('thrift');
var ttypes = require('../gen-nodejs/search_types');
var service = require('../gen-nodejs/TCodeSearchService');

var connection = thrift.createConnection('localhost', 5000);

exports.index = function (req, res) {
    res.render('index', {
        title: 'Express'
    });
};

exports.partials = function (req, res) {
    res.render('partials/' + req.params.name);
};

exports.api = function (req, res) {
    // TODO: Reuse TCP connection.
    var client = thrift.createClient(service, connection);
    var method = req.params.method;
    var callback = function(error, rpcResp) {
        if (error != null) {
            console.log("error", error);
        }
        res.send(rpcResp);
        console.log("client = ", client);
        console.log("connection = ", connection);
    }
    if (method == "search") {
        client.search(new ttypes.TSearchRequest(req.body), callback);
    } else if (method == "complete") {
        client.complete(new ttypes.TCompleteRequest(req.body), callback);
    } else if (method == "source") {
        client.source(new ttypes.TSourceRequest(req.body), callback);
    } else {
        res.send('Method not found: ' + method, 404);
    }
}
