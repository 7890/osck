//http://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string

//works with jdk1.6
//basically just a wrapper to a hidden feature

import java.io.UnsupportedEncodingException;    
import javax.xml.bind.DatatypeConverter;

public class Base64
{
	public static String encode(String str)
	{
		return DatatypeConverter.printBase64Binary(str.getBytes());
	}
	public static String decode(String str)
	{
		return new String(DatatypeConverter.parseBase64Binary(str));
	}

	public static void main(String[] args) throws UnsupportedEncodingException
	{
		String str = "this'&&%*@<\\\\>ä";
		System.out.println("original value is \t" + str);
		//encode data using BASE64
		String encoded = Base64.encode(str);
		System.out.println("encoded value is \t" + encoded);
		//decode data 
		String decoded = Base64.decode(encoded);
		System.out.println("decoded value is \t" + decoded);
	}
}
