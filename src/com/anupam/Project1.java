package com.anupam;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;


import com.anupam.clock.LogicalClock;
import com.anupam.connection.ConnectionManager;
import com.anupam.msg.Message;
import com.anupam.msg.MultiCastMessage;
import com.sun.nio.sctp.SctpChannel;

/**
 * Main Aplication class.
 * Initiates a recieve thread for recieving messages
 * Initiates multiple send thread for sending messages
 * 
 * This is an implementation of a totally ordered service.
 * send allows the application to send a multicast message to a subset of processes.
 * recieve allows the application to recieve a message.
 * 
 * The config.txt file specifies the required parameters for running the application.
 * and Messages.txt is the file, which reads the messages to be send to.
 * com.anupam.Constants specifies the other constants use.
 * @author Anupam Gangotia
 *Profile::http://en.gravatar.com/gangotia
 * github::https://github.com/agangotia
 */
public class Project1 {
	
	/**
	 * applicationRunning=true, Server will listen for requests
	 * applicationRunning=false, normally happens when u quit the application
	 */
	static volatile boolean applicationRunning;
	
	/**
	 * This is the total number of nodes in topology read from config.
	 */
	static int totalNodes;
	/**
	 * This is the nodeID of current Node, passed as command line arguement
	 */
	public static volatile int nodeID;
	/**
	 * This is the map of all Nodes Present in the Topology
	 */
	private static HashMap<Integer, NodeDetails> mapNodes;
	/**
	 * This is the map of all Nodes Present in the Topology
	 */
	 static HashMap<String, NodeDetails> mapNodesByAddress;
	/**
	 * contains information about current Node read from config.
	 */
	private static NodeDetails currentNode;

	/**
	 * This is the map of SCTP Connections.Each Process will contain the connection objects
	 */
	public static ConcurrentHashMap<Integer, SctpChannel> connectionSocket;
	

	/**
	 * Lamport's logical clock
	 * initial value=0
	 * On send event : +1
	 * On recieve event : Max(currentval,valFromMessage)+1
	 */
	static LogicalClock LC;//Lamport's Logical Clock

	

	/**
	 * Queue containing Multicast messages to be send
	 */
	private static BlockingQueue<MultiCastMessage> sendQueue;
	/**
	 * Queue containing clock Replies
	 */
	private static BlockingQueue<Message> sendClockReplyQueue;
	/**
	 * Queue containing Multicast messages for max clock value
	 */
	private static BlockingQueue<MultiCastMessage> sendMaxClock;
	
	/**
	 * recvQueue, Messages recieved are buffered in the following code,
	 * till they are delivered based on skleen's algorithm.
	 */
	private static BlockingQueue<Message> recvQueue;
	
	/**
	 * mapClockReplies : In this map node stores the replies, obtained for a multicast message.
	 * Once recieved all the replies, it then flushes it.
	 */
	private static ConcurrentHashMap<String,Message> mapClockReplies;
	/**
	 * countClockReplies This is the clock replies this Node is awaiting replies.
	 * Acc to Skleen's Algo, the node picks the next entry only if countClockReplies=0.
	 */
	private static volatile int countClockReplies;

	
	//static block for static variables initialization
	static {
		sendQueue=new ArrayBlockingQueue<MultiCastMessage>(Constants.SIZESENDQ, true);
		sendClockReplyQueue=new ArrayBlockingQueue<Message>(Constants.SIZECLOCKREPLYQ, true);
		sendMaxClock=new ArrayBlockingQueue<MultiCastMessage>(Constants.SIZEMAXCLOCKQ, true);
		recvQueue=new ArrayBlockingQueue<Message>(Constants.SIZERECVQ, true);
		applicationRunning=true;
		mapClockReplies=new ConcurrentHashMap<String,Message>();
		countClockReplies=0;
		LC=new LogicalClock();
		connectionSocket=new ConcurrentHashMap<Integer,SctpChannel>();
		mapNodes=new HashMap<Integer, NodeDetails>();
		mapNodesByAddress=new HashMap<String, NodeDetails>();
	}
	
	/**
	 * This is the main method. Start point of execution for My Application.
	 */
	public static void main(String[] args) {
		if(args.length!=1){
			
			System.out.println("Inappropriate arguement passed, please pass only 1 arguement");
			return;
		}
		
		if(!readConfig(Constants.CONFIGFILE,Integer.parseInt(args[0]))){
			return ;
		}
		
		if(!ConnectionManager.createConnections(currentNode,connectionSocket, mapNodes))
			return;
		
		Thread serverThread;
		Thread sendThread;
		try {
			serverThread = new Server(LC,sendMaxClock,sendClockReplyQueue,recvQueue,mapClockReplies);
			serverThread.start();
			sendThread = new MessageSender(LC,sendQueue,sendMaxClock,sendClockReplyQueue,mapClockReplies);
			sendThread.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		logApplicationConfDetails();
		displayMenu();
		try {
			if(serverThread!=null)
				serverThread.join();
			if(sendThread!=null)
				sendThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}

	/**
	 * Function :displayMenu()
	 * displays the menu for each application
	 */
	private static void displayMenu(){
		boolean terminateLoop = false;
		Long initClock=System.currentTimeMillis();
		Long exitClock=initClock+Constants.AMNODEALIVEDURATION;
		if(Constants.AUTOMATICMODE){
			while(System.currentTimeMillis()<exitClock){
				MultiCastMessage results = readMessage(Constants.MESSAGESFILE);
				if(results!=null)
					sendMulticastMessage(results);
				try {
					Thread.sleep(Constants.DELAY);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			applicationRunning=false;
			return;
		}else{
			while (!terminateLoop) {
				System.out.println("1.Send a message");
				System.out.println("2.Test the service");
				System.out.println("3.Terminate and exit");

				Scanner in = new Scanner(System.in);
				int a = in.nextInt();
				//in.close();
				switch (a) {
				case 1:
					MultiCastMessage results = readMessage(Constants.MESSAGESFILE);
					if(results!=null)
						sendMulticastMessage(results);
					break;
				case 2:
					break;
				case 3:
					System.out.println("Exiting");
					
					applicationRunning=false;
					terminateLoop = true;
					break;
				default:
					break;
				}
			}
		}
		return;
	}



	/**
	 * Reads the message from a text file, and returns them in an arraylist of
	 * String[3]. e.g. <[localhost],[port],[message1]>
	 * <[localhost],[port],[message2]>
	 * 
	 * @param fileName
	 * @return
	 */
	public static MultiCastMessage readMessage(String fileName) {
		ArrayList<Message> results = new ArrayList<Message>();
		BufferedReader bReader = null;
		try {

			bReader = new BufferedReader(new FileReader(fileName));
			String line = bReader.readLine();
			boolean firstLine=true;
			while (line != null) {
				if(firstLine){
					firstLine=false;
				}else{
					StringTokenizer st = new StringTokenizer(line, ",");
					int senderNodeID=Integer.parseInt((String) st.nextElement());
					if(senderNodeID==nodeID){
						String[] message = new String[3];
						if (st.countTokens() == 3) {
							message[0] = (String) st.nextElement();
							message[1] = (String) st.nextElement();
							message[2] = (String) st.nextElement();
							Message msgObj=new Message("",currentNode.getAddress(),currentNode.getPortNumber(),message[0],Integer.parseInt(message[1]),0,message[2]);
							results.add(msgObj);
						} else {
							System.out.println("Error in reading line");
						}
					}
				}	
				line = bReader.readLine();
				if(line!=null && line.length()==0)
					break;
			}
			MultiCastMessage objMulti=new MultiCastMessage(results);
			return objMulti;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bReader != null)
					bReader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
		return null;
	}

	/*
	 * Reads the config parameters from the config file
	 */
	public static boolean readConfig(String fileName,int nodeid) {
		System.out.println("Reading config for"+nodeid);
		nodeID=nodeid;
		BufferedReader bReader = null;
		int nodesCount=0;
		try {
			bReader = new BufferedReader(new FileReader(fileName));
			String line = bReader.readLine();
			boolean firstLine=true;
			while(line!=null){
				if(firstLine){
					firstLine=false;
				}else{
					StringTokenizer st = new StringTokenizer(line, ",");
					int nodeID=Integer.parseInt((String) st.nextElement());
					String address=(String)st.nextElement();
					int portNumber=Integer.parseInt((String) st.nextElement());	
					NodeDetails nodeObj=new NodeDetails(nodeID, portNumber, address);
					mapNodes.put(nodeID,nodeObj);
					mapNodesByAddress.put(address+String.valueOf(portNumber),nodeObj);
					nodesCount++;
				}
				line = bReader.readLine();
				if(line!=null && line.length()==0)
					break;
				}
			
			totalNodes=nodesCount;
			//System.out.println("Total Nodes"+totalNodes);
			//All the Node info has been filled
			if(mapNodes.containsKey(nodeID))
				currentNode=mapNodes.get(nodeID);
			else{
				
				System.out.println("*********************************************************");
				System.out.println("Please Supply the correct Process ID"+nodeid);
				System.out.println("*********************************************************");
				System.out.println("Exiting");
				return false;
			}
			}catch (IOException e) {
			e.printStackTrace();
			System.out.println("*********************************************************");
			System.out.println("Exception in reading config"+e.toString());
			return false;
		} finally {
			try {
				if (bReader != null)
					bReader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
		return true;
	}

	/**
	 * Sends the messages To 
	 * 
	 * @param messages
	 */
	public static void sendMulticastMessage(MultiCastMessage message) {
	
		sendQueue.add(message);

	}

	
	public static synchronized void incrementClockReplies(){
		countClockReplies++;
	}
	public static synchronized void decrementClockReplies(){
		countClockReplies--;
	}
	public static synchronized int getClockReplies(){
		return countClockReplies;
	}
	
	
	
	public static void logApplicationConfDetails(){
		
		System.out.println("*********************************************************");
		System.out.println("APPLICATION is UP "+currentNode.getAddress());
		System.out.println("SENDER is UP ");
		System.out.println("RECIEVER is UP "+currentNode.getPortNumber());
		System.out.println("SIZE : SEND QUEUE :"+Constants.SIZESENDQ);
		System.out.println("SIZE : RECIEVE QUEUE :"+Constants.SIZERECVQ);
		System.out.println("SIZE : SEND CONTROL CLOCKS QUEUE :"+Constants.SIZECLOCKREPLYQ);
		System.out.println("SIZE : SEND CONTROL MAX CLOCK QUEUE :"+Constants.SIZEMAXCLOCKQ);
		System.out.println("LAMPORT CLOCK INITIALIZED :"+LC.getValue());
		System.out.println("*********************************************************");
	}
	
}
