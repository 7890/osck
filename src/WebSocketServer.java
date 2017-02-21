import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

//http://download.eclipse.org/jetty/9.3.7.v20160115/apidocs/org/eclipse/jetty/websocket/api/package-summary.html

//tb/160403

//========================================================================
//========================================================================
public class WebSocketServer
{
	//this file is loaded if found in current directory
	private String propertiesFileUri="WebSocketServer.properties";

	//===configurable parameters (here: default values)
	public int port=8080;
	public String context="/osck";
	public String handler_classname="OSCKWebSocketHandler";
	//===end configurable parameters

//========================================================================
	public static void main(String[] args) throws Exception
	{
		WebSocketServer ws=new WebSocketServer(args);
	}

//========================================================================
	public WebSocketServer(String[] args) throws Exception
	{
		if(args.length>0 && (args[0].equals("-h") || args[0].equals("--help")))
		{
			System.out.println("WebSocketServer Help");
			System.out.println("Arguments: (properties file to use)");
			System.out.println("If no argument given, default file '"+propertiesFileUri+"' will be used.");
			System.exit(0);
		}
		else if(args.length>0)
		{
			propertiesFileUri=args[0];
		}

		if(!loadProps(propertiesFileUri))
		{
			System.err.println("could not load properties");
		}

		Server server = new Server(port);
		//currently just one handler supported
		WebSocketHandler wsHandler = new WebSocketHandler()
		{
			@Override
			public void configure(WebSocketServletFactory factory)
			{
				try
				{
					Class<?> c = Class.forName(handler_classname);
					factory.register(c);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					System.err.println("Could not load handler class: "+handler_classname);
					System.exit(1);
				}
			}
		};
		ContextHandler ch = new ContextHandler();
		ch.setContextPath(context);
		ch.setHandler(wsHandler);
		HandlerCollection hc = new HandlerCollection();
		hc.addHandler(ch);
		server.setHandler(hc);
		//server.addHandler(ch);
		//server.addHandler(wsHandler);
		server.start();
		server.join();
	}

//========================================================================
	public boolean loadProps(String configfile_uri)
	{
		propertiesFileUri=configfile_uri;
		return LProps.load(propertiesFileUri,this);
	}
}//end class WebSocketServer
//EOF
