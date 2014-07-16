package com.anupam;
/**
* @author Anupam Gangotia
* Profile::http://en.gravatar.com/gangotia
* github::https://github.com/agangotia
*/
public interface Constants {
	String SEPARATOR="//";
	
	String MESSAGESFILE="data"+SEPARATOR+"multicast.txt";
	String CONFIGFILE="data"+SEPARATOR+"topology.txt";
	
	String lOGMSGRECVD="log"+SEPARATOR+"logmsgrecvd";
	//String lOGMSGSEND="log"+SEPARATOR+"logmsgsend";
	
	int SIZESENDQ=100;
	int SIZECLOCKREPLYQ=100;
	int SIZEMAXCLOCKQ=1;
	int SIZERECVQ=100;
	
	/**
	 * If AUTOMATICMODE = true, processes sends the messages automatically
	 * if AUTOMATICMODE = false, a menu is displayed to send the message manually
	 */
	boolean AUTOMATICMODE=true;
	Long AMNODEALIVEDURATION=15000L;//Total time node will stay alive, before dying
	Long DELAY=1000L;//delay in message send event
	
}
