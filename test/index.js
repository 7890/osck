//https://www.w3.org/TR/2011/WD-websockets-20110419/

//tb/160402
var ws_url="ws://127.0.0.1:8080/osck/";
var server_thread=null;

var ws = new WebSocket(ws_url);

//=============================================================================
var any_msg_callback=function(addr,msg)
{
	addr.pretty();
	msg.pretty();
}

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
//	console.log(evt.data);
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
