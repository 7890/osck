//https://www.w3.org/TR/2011/WD-websockets-20110419/

//tb/160402
var ws_url="ws://127.0.0.1:8081/osck/";
var server_thread=null;

//var ws = new WebSocket(ws_url);
//http://stackoverflow.com/questions/8393092/javascript-websockets-control-initial-connection-when-does-onopen-get-bound
var ws = null;

//=============================================================================
function ws_connect()
{
	console.log("ws_connect()");
	ws = new WebSocket(ws_url);
	//setTimeout(ws_bind_events, 1000);
	ws_bind_events();
}

//=============================================================================
function ws_disconnect()
{
	console.log("ws_disconnect()");
	ws.close();
}

//=============================================================================
function ws_bind_events()
{
	console.log("ws_bind_events");
//=============================================================================
	ws.onopen = function()
	{
		console.log("websocket "+ws_url+" opened");
		connected=1;
		server_thread=new OSCServerThread(4455,10000);
		server_thread.attach("//*", "", "any_msg_callback");
		server_thread.start();
	};

//=============================================================================
	ws.onmessage = function (evt)
	{
//		console.log(evt.data);
		eval(evt.data);
	};

//=============================================================================
	ws.onclose = function()
	{
		console.log("websocket "+ws_url+" closed");
		connected=0;
	};

//=============================================================================
	ws.onerror = function(err)
	{
		console.log("websocket "+ws_url+" error: "+err);
		connected=0;
	};
}//end ws_bind_events()

//ws_connect();

//=============================================================================
var any_msg_callback=function(addr,msg)
{
	addr.pretty();
	msg.pretty();
}
