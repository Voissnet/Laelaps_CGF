/**
 * Requires node.js (https://nodejs.org) and smpp (https://github.com/farhadi/node-smpp | npm install --save smpp )
 **/
"use strict";

var smpp = require('smpp');
var uuid = require('node-uuid');

function Client() {
	var session = new smpp.Session({host: process.env.npm_package_config_host, port: process.env.npm_package_config_port});
	var didConnect = false; 

	function connectSMPP() {
	  console.log('smpp reconnecting');
	  session.connect();
	}

	session.on('close', function(){
	  console.log('smpp disconnected')
	  if (didConnect) {
		connectSMPP();
	  }
	})

	session.on('error', function(error){
	  console.log('smpp error', error)
	  didConnect = false;
	})

	function lookupPDUStatusKey(pduCommandStatus) {
	  for (var k in smpp.errors) {
		if (smpp.errors[k] == pduCommandStatus) {
		  return k
		}
	  }
	}

	function sendSMS(from, to, text) {
	  // in this example, from & to are integers
	  // We need to convert them to String
	  // and add `+` before

	  from = '+' + from.toString();
	  to   = '+' + to.toString();

	  session.submit_sm({
		  source_addr:      from,
		  destination_addr: to,
		  short_message:    text
	  }, function(pdu) {
		console.log('sms pdu status', lookupPDUStatusKey(pdu.command_status));
		  if (pdu.command_status == 0) {
			  // Message successfully sent
			  var messageId = pdu.message_id;
			  console.log(messageId);
		  } else { 
			console.error('Something unexpected occurred, abandoning.');
			didConnect = false;
			session.close();
		  }
	  });
	}

	session.on('deliver_sm',  function(pdu){
		console.log('srv: session received a deliver_sm pdu', pdu.short_message);
	});

	session.on('unbind_resp', function(pdu) {
		console.log('srv: session received a unbind_resp pdu');
		didConnect = !(process.env.npm_package_config_reconnect==0);
		session.close();
	});

	session.on('connect', function(){
	  didConnect = true;

	  session.bind_transceiver({
		  system_id: 'pavel',
		  password: 'dfsew',
		  interface_version: 1,
		  system_type: '380666000600',
		  address_range: '+380666000600',
		  addr_ton: 1,
		  addr_npi: 1,
	  }, function(pdu) {
		console.log('pdu status', lookupPDUStatusKey(pdu.command_status));
		if (pdu.command_status == 0) {
			console.log('Successfully bound')
			var iterations = randomInt(10,20)//http://blog.tompawlak.org/how-to-generate-random-values-nodejs-javascript
			for (var i = 0; i < iterations; i++) {
				var _uuid = uuid.v4();
				sendSMS(56448900000, 56986769674, _uuid);
			}
		}
	  });
	})
	
	function randomInt (low, high) {
		return Math.floor(Math.random() * (high - low) + low);
	}
}

module.exports = Client;

var object = new Client();
