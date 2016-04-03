//tb/160402

//script uses "console.log" (i.e. with firebug)

//set to 1 when websocket connection is open
var connected=0;

//=============================================================================
//OSCCallback
//=============================================================================
var OSCCallback=function(path_filter, typetag_filter, callback_function)
{
	this.path_filter = path_filter;
	this.typetag_filter = typetag_filter;
	this.callback_function = callback_function;
};

//=============================================================================
OSCCallback.prototype.call=function(address,message)
{
	return this.callback_function(address,message);
};

//=============================================================================
//OSCAddress
//=============================================================================
var OSCAddress=function(host,port)
{
	this.host = host;
	this.port = port;
};

//=============================================================================
OSCAddress.prototype.pretty=function()
{
	console.log(this.host+":"+this.port);
};

//=============================================================================
//OSCMessage
//=============================================================================
var OSCMessage=function()
{
	if(arguments.length==1) //path
	{
		this.path=arguments[0];
		this.vec_types=[];
		this.vec_args=[];
	}
	else if(arguments.length==3) //path, types, vargs
	{
		this.path=arguments[0];
		this.vec_types=[];
		this.vec_args=[];
		this.add_args(arguments[1],arguments[2]);
	}
};

//=============================================================================
OSCMessage.prototype.send=function()
{
	if(arguments.length==1) //address
	{
		var a=arguments[0];
		if(connected==1)
		{
			///ws defined outside of this file
			ws.send(this.path+";"+a.host+";"+a.port+";"+this.vec_types.join("")+";"+this.vec_args.join(";"));
		}

		return this;
	}
	else if(arguments.length==2) //host, port
	{
		return this.send(new OSCAddress(arguments[0],arguments[1]));
	}
	else
	{
		console.log("OSCMessage.prototype.send error: invalid arguments");
		return;
	}
};

//=============================================================================
OSCMessage.prototype.types=function(){return this.vec_types;};
OSCMessage.prototype.args=function(){return this.vec_args;};
OSCMessage.prototype.type=function(index){return this.vec_types[index];};
OSCMessage.prototype.arg=function(index){return this.vec_args[index];};
//OSCMessage.prototype.remove=function(index){}; ///

//=============================================================================
OSCMessage.prototype.add=function(type,val)
{
	this.vec_types.push(type);
	this.vec_args.push(val);
	return this;
};

//=============================================================================
OSCMessage.prototype.add_array=function(type,arr)
{
	var size=arr.length;
	var tt=[size];
	for(i=0;i<size;i++)
	{
		tt[i]=type;
	}
	this.vec_types=this.vec_types.concat(tt);
	this.vec_args=this.vec_args.concat(arr);
	return this;
};

//=============================================================================
OSCMessage.prototype.add_float=function(val){return this.add("f",val);};
OSCMessage.prototype.add_double=function(val){return this.add("d",val);};
OSCMessage.prototype.add_string=function(val){return this.add("s",val);};
OSCMessage.prototype.add_int32=function(val){return this.add("i",val);};
OSCMessage.prototype.add_int64=function(val){return this.add("h",val);};
OSCMessage.prototype.add_char=function(val){return this.add("c",val);};

//=============================================================================
OSCMessage.prototype.add_float_array=function(arr){return this.add_array("f",arr);};
OSCMessage.prototype.add_double_array=function(arr){return this.add_array("d",arr);};
OSCMessage.prototype.add_string_array=function(arr){return this.add_array("s",arr);};
OSCMessage.prototype.add_int32_array=function(arr){return this.add_array("i",arr);};
OSCMessage.prototype.add_int64_array=function(arr){return this.add_array("h",arr);};
OSCMessage.prototype.add_char_array=function(arr){return this.add_array("c",arr);};

//=============================================================================
OSCMessage.prototype.f=function(val){return this.add("f",val);};
OSCMessage.prototype.d=function(val){return this.add("d",val);};
OSCMessage.prototype.s=function(val){return this.add("s",val);};
OSCMessage.prototype.i=function(val){return this.add("i",val);};
OSCMessage.prototype.h=function(val){return this.add("h",val);};
OSCMessage.prototype.c=function(val){return this.add("c",val);};

//=============================================================================
OSCMessage.prototype.farr=function(arr){return this.add_array("f",arr);};
OSCMessage.prototype.darr=function(arr){return this.add_array("d",arr);};
OSCMessage.prototype.sarr=function(arr){return this.add_array("s",arr);};
OSCMessage.prototype.iarr=function(arr){return this.add_array("i",arr);};
OSCMessage.prototype.harr=function(arr){return this.add_array("h",arr);};
OSCMessage.prototype.carr=function(arr){return this.add_array("c",arr);};

//=============================================================================
OSCMessage.prototype.add_args=function(types,vargs)
{
	//concat to existing arrays
	this.vec_types=this.vec_types.concat(types.split(""));
//	console.log(this.vec_types);
	this.vec_args=this.vec_args.concat(vargs);
//	console.log(this.vec_args);
	return this;
};

//=============================================================================
OSCMessage.prototype.pretty=function()
{
	if(arguments.length==0) //whole message
	{
		console.log(this.path);
		for(var i=0;i<this.vec_types.length;++i)
		{
			this.pretty(i);
		}
		return this;
	}
	else if(arguments.length==1) //index
	{
		var index=arguments[0];
		console.log(" ["+index+"]("+this.vec_types[index]+") "+this.vec_args[index]);
	}

	return this;
};

//=============================================================================
//OSCServerThread
//=============================================================================
var OSCServerThread=function(port,heartbeat_millis)
{
	this.port=port;
	this.heartbeat_millis=heartbeat_millis;
	this.vec_handlers=[];
	this.generic_callback=null;
	osck_server_thread_new(this.port,this.heartbeat_millis);
	console.log("OSC server thread created on port: "+this.port+" hearbeat millis: "+this.heartbeat_millis);
};

//=============================================================================
OSCServerThread.prototype.attach=function(path_filter, typetag_filter, callback_function)
{
	this.vec_handlers.push(new OSCCallback(path_filter, typetag_filter, callback_function));
	osck_server_thread_attach_handler(path_filter, typetag_filter, callback_function);
	console.log("callback attached: "+callback_function);
};

//=============================================================================
OSCServerThread.prototype.attach_generic=function(callback_function)
{
	this.generic_callback=callback_function;
	///send here
	console.log("generic callback attached");
};

//=============================================================================
OSCServerThread.prototype.start=function()
{
	osck_server_thread_start();
	console.log("OSC server started");
};

//=============================================================================
OSCServerThread.prototype.send=function()
{
	if(arguments.length==2) //address,message
	{
		arguments[1].send(address);
	}
	else if(arguments.length==3) //host,port,message
	{
		arguments[2].send(arguments[0],arguments[1]);
	}
	else if(arguments.length==5) //host,port,path,types,vargs
	{
		new OSCMessage(arguments[2]).add_args(arguments[3],arguments[4]).send(arguments[0],arguments[1]);
	}
};

//=============================================================================
var OSCHeartBeat=function()
{
	//server calls this function periodically (optional)
	//send back something to keep connection alive
	if(connected==1)
	{
		ws.send("/osck/heart_beat;dummy;0");
	}
};

//=============================================================================
var osck_server_thread_new=function(port,heartbeat_millis)
{
	if(connected==1)
	{
		ws.send("/osck/server_thread/new;dummy;0;ii;"+port+";"+heartbeat_millis);
	}
}

//=============================================================================
var osck_server_thread_attach_handler=function(path_filter, typetag_filter, callback_function)
{
	if(connected==1)
	{
		ws.send("/osck/server_thread/attach_handler;dummy;0;sss;"+path_filter+";"+typetag_filter+";"+callback_function);
	}
}

//=============================================================================
var osck_server_thread_start=function()
{
//      if(ws.readyState==1) //OPEN
	if(connected==1)
	{
		ws.send("/osck/server_thread/start;dummy;0;");
	}
}

//EOF
