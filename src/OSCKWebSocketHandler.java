import java.lang.Thread;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.util.Vector;
import java.util.Date;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import com.illposed.osc.*;

//http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/websocket/server/WebSocketHandler.html

//tb/160403

//=============================================================================
//=============================================================================
@WebSocket
public class OSCKWebSocketHandler
{
	private DatagramSocket ds;
	private OSCPortIn portIn; //receive

	private String csv_separator=";";
	private int heartbeat_impulse_millis=0;

	private Session sess=null;

	private boolean debug=false;

//websocket callback methods
//=============================================================================
	@OnWebSocketConnect
	public void onConnect(Session session)
	{
		sess=session;
		p("Connect: " + session.getRemoteAddress().getAddress());

		final RemoteEndpoint ep=session.getRemote();
		Thread t = new Thread()
		{
			public void run()
			{
				while(1==1)
				{
					try{
						if(heartbeat_impulse_millis>0)
						{
							ep.sendString("OSCHeartBeat()");
							Thread.sleep(heartbeat_impulse_millis);
						}
						else
						{
							Thread.sleep(1000);
						}
					}catch(Exception e)
					{
						e.printStackTrace();
						return;
					}
				}
			}//end run()
		};//end Thread t
		t.start();
	}//end onConnect()

//=============================================================================
	@OnWebSocketMessage
	public void onMessage(String message)
	{
		d("Message: " + message);

		//only accept messages starting with "/"
		if(!message.startsWith("/"))
		{
			return;
		}

		//internal command, not to send out
		if(message.startsWith("/osck"))
		{
			try
			{
				OSCMessage msg=parseOSCMessageWithAddressFromCSVString(message);
				String path=msg.getAddress();
				java.util.List<Object> args=msg.getArguments();
				int argsSize=args.size();

				//check for internal commands / messages
				if(path.equals("/osck/server_thread/new") && argsSize==2) //port, heartbeat_millis
				{
					d("NEW");
					ds=new DatagramSocket(Integer.parseInt(""+args.get(0)));
					portIn=new OSCPortIn(ds);
					heartbeat_impulse_millis=Integer.parseInt(""+args.get(1));
				}
				else if(path.equals("/osck/server_thread/attach_handler")  && argsSize==3) //path_filter, type_filter, callback_name
				{
					d("ATTACH");
					portIn.addListener((String)args.get(0)
						,new MessageListener((String)args.get(1)
							,(String)args.get(2)
						)
					);
				}
				else if(path.equals("/osck/server_thread/start") && argsSize==0)
				{
					d("START");
					if(portIn!=null)
					{
						portIn.startListening();
					}
				}
				else if(path.equals("/osck/heart_beat") && argsSize==0)
				{
					d("HEARTBEAT");
				}
				else
				{///
				}
				return;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return;
			}
		}//end if message.startsWith("/osck")

		try
		{
			OSCAddress addr=parseAddressFromCSVString(message);
			OSCMessage msg=parseOSCMessageWithAddressFromCSVString(message);

			////could maybe reuse port
			////p(Base64.decode("dHJ5IiB0aGlzIiZcc3VwZXIgZHVwZXI="));

			OSCPortOut portOut=null;
			if(portIn!=null && ds!=null)
			{
				//send from server thread port using datagram socket
				portOut=new OSCPortOut(InetAddress.getByName(addr.host), addr.port, ds);
				portOut.send(msg);
			}
			else
			{
				//send from random port
				portOut=new OSCPortOut(InetAddress.getByName(addr.host), addr.port); 
				portOut.send(msg);
				portOut.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}//end onMessage

//=============================================================================
	@OnWebSocketClose
	public void onClose(int statusCode, String reason)
	{
		p("Close: statusCode=" + statusCode + ", reason=" + reason);
		shutdown();
	}

//=============================================================================
	@OnWebSocketError
	public void onError(Throwable t)
	{
		e("Error: " + t.getMessage());
	}

//end of websocket callback methods 
//=============================================================================

//========================================================================
class OSCAddress
{
	String host="";
	int port=0;
	public OSCAddress(String host,int port)
	{
		this.host=host;
		this.port=port;
	}
	public String toString()
	{
		return this.host+";"+this.port;
	}
}

//========================================================================
//========================================================================
class MessageListener implements OSCListener
{
	private String typetag_filter=null;
	private String callback_name=null;

	public MessageListener(){}
	public MessageListener(String typetag_filter)
	{
		this.typetag_filter=typetag_filter;
	}
	public MessageListener(String typetag_filter, String callback_name)
	{
		this.typetag_filter=typetag_filter;
		this.callback_name=callback_name;
	}

//========================================================================
	public void accept(OSCMessage msg)
	{
		if(typetag_filter!=null)
		{
			///
		}
		d("message received (non-browser)");
		if(callback_name!=null && callback_name!=null)
		{
			d("calling back JS method '"+callback_name+"'");
			if(sess!=null)
			{
				try
				{
					RemoteEndpoint ep=sess.getRemote();
					String path=msg.getAddress();
					java.util.List<Object> args=msg.getArguments();
					int argsSize=args.size();

					//create a JS string that will call the callback method with
					//objects created in the JS space
					StringBuffer sb=new StringBuffer();
					sb.append(callback_name+"(new OSCAddress('"
						+InetAddress.getByName(msg.getRemoteHost())+"',"+msg.getRemotePort()
						+"),new OSCMessage('"+path+"'");

					String types=msg.getTypetagString();
					if(args.size()>0){sb.append(",'"+types+"',[");}
					for(int i=0;i<args.size();i++)
					{
						if(i>0){sb.append(",");}
						char c=types.charAt(i);
						if(c=='i' || c=='f' || c=='h' || c=='d')
						{
							sb.append(args.get(i));
						}
						else if(c=='s' || c=='c')
						{
							sb.append(new String("'"+args.get(i)+"'"));
						}
						///....
					}
					if(args.size()>0){sb.append("]");}
					sb.append("))"); //end new OSCMessage, end callback
					d("sending to browser: "+sb.toString());
					ep.sendString(sb.toString());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}//end if(sess!=null)
		}//end (callback_name!=null)
	}//end accpept()

//========================================================================
	public void acceptMessage(Date time,OSCMessage msg) 
	{
		accept(msg);
	}
}//end inner class MessageListener

//========================================================================
	public OSCAddress parseAddressFromCSVString(String oscstring) throws Exception
	{
		// !!! csv_separator / ';' can't be part of string content
		String[] args=oscstring.split(csv_separator);

		//parse args: path, host, port, typetags, args...
		//minimum #args: 3
		//if typetag contained: min #args 5
		if(args.length<3 || args.length==4)
		{
			//                                    0      1      2         3        4 ...
			throw new Exception("Error: syntax: <path>;<host>;<port>;(<typetags>;<args>;...)");
		}

		try
		{
			int port=Integer.parseInt(args[2]);
			return new OSCAddress(args[1],port);
		}
		catch(Exception e)
		{
			throw new Exception("Something went wrong parsing the message\n"+e.getMessage());
		}
	}

///	public OSCMessage parseOSCMessageCSVString(String oscstring) throws Exception
//========================================================================
	public OSCMessage parseOSCMessageWithAddressFromCSVString(String oscstring) throws Exception
	{
		// !!! csv_separator / ';' can't be part of string content
		String[] args=oscstring.split(csv_separator);

		//parse args: path, host, port, typetags, args...
		//minimum #args: 3
		//if typetag contained: min #args 5
		if(args.length<3 || args.length==4)
		{
			//                                    0      1      2         3        4 ...
			throw new Exception("Error: syntax: <path>;<host>;<port>;(<typetags>;<args>;...)");
		}

		try
		{
			String path=args[0];
			String typetags="";
			Vector msg_args=new Vector();
		
			if(args.length>4)
			{
				typetags=args[3].trim();

				if(typetags.length()!=args.length-4)
				{
					throw new Exception("Error: typetags length does not match # of args");
				}

				for(int i=0;i<(args.length-4);i++)
				{
					String type=typetags.substring(i,i+1);
					if(type.equals("s"))
					{
						msg_args.add(args[(i+4)]);
					}
					else if(type.equals("i"))
					{
						msg_args.add(new Integer( Integer.parseInt( args[(i+4)] ) ));
					}
					else if(type.equals("f"))
					{
						msg_args.add(new Float( Float.parseFloat( args[(i+4)] ) ));
					}
					else
					{
						e("Type '"+type+"' not supported! (just 's', 'i' and 'f' for now).");
					}
				}
			}//end if args length >2

			//create message, cast args to types according to typetag
			OSCMessage msg_out=null;
			if(!msg_args.isEmpty())
			{
				msg_out=new OSCMessage(path,msg_args);
			}
			else
			{
				msg_out=new OSCMessage(path);
			}
			return msg_out;
		}
		catch(Exception e)
		{
			throw new Exception("Something went wrong parsing the message\n"+e.getMessage());
		}
	}//end parseOSCMessageFromCSVString()

//=============================================================================
	private void p(String s)
	{
		System.out.println(s);
	}

//=============================================================================
	private void d(String s)
	{
		if(debug)
		{
			System.out.println(s);
		}
	}

//=============================================================================
	private void e(String s)
	{
		System.err.println(s);
	}

//=============================================================================
	private void shutdown()
	{
		try
		{
			if(portIn!=null)
			{
				portIn.stopListening();
				portIn.close();
			}
			if(ds!=null)
			{
				ds.disconnect();
				ds.close();
			}
		}catch(Exception e){}
	}

}//end class OSCKWebSocketHandler
//EOF
