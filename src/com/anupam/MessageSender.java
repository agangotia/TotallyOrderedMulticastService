package com.anupam;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.anupam.clock.LogicalClock;
import com.anupam.fileAppend.FileAppend;
import com.anupam.msg.Message;
import com.anupam.msg.MultiCastMessage;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

/**
 * Sender Thread:
 * This thread sends messages from the send queue
 * @author Anupam Gangotia
 * Profile::http://en.gravatar.com/gangotia
 * github::https://github.com/agangotia
*/
public class MessageSender extends Thread{

	
	private BlockingQueue<MultiCastMessage> sendQueue;
	private LogicalClock LC;
	ConcurrentHashMap<String,Message> mapClockReplies;
	BlockingQueue<MultiCastMessage> sendMaxClock;
	BlockingQueue<Message> sendClockReplyQueue;
	Long unicastMessageCount;
	volatile Long multicastMessageCount; 
	
	public MessageSender(LogicalClock LC,BlockingQueue<MultiCastMessage> sendQueue,BlockingQueue<MultiCastMessage> sendMaxClock,BlockingQueue<Message> sendClockReplyQueue,ConcurrentHashMap<String,Message> mapClockReplies){
		this.LC=LC;
		this.sendQueue=sendQueue;
		this.sendMaxClock=sendMaxClock;
		this.sendClockReplyQueue=sendClockReplyQueue;
		this.mapClockReplies=mapClockReplies;
		unicastMessageCount=0l;
		multicastMessageCount=0l;
	}
	

	
	 public void run() 
	   {
		
		 while(Project1.applicationRunning){
			 /**
			  * Highest Priority is to send Clock Replies.
			  * So sending them first.
			  */
			while(!sendClockReplyQueue.isEmpty()){
				Message toSend=sendClockReplyQueue.poll();
				 //System.out.println(toSend.getRecieverAddress()+String.valueOf(toSend.getRecieverPort()));
				 int destNodeID=Project1.mapNodesByAddress.get(toSend.getRecieverAddress()+toSend.getRecieverPort()).getNodeID();
				// toSend.setMessageId(this.getUnicastMessageID(Project1.nodeID,destNodeID));
				 sendMessage(Project1.mapNodesByAddress.get(toSend.getRecieverAddress()+String.valueOf(toSend.getRecieverPort())).getNodeID(),toSend);
			}
			/**
			  * Second Highest Priority is to send MAx Clock Replies.
			  * So sending them second.
			  */
			while(!sendMaxClock.isEmpty()){
				MultiCastMessage objMulti=sendMaxClock.poll();
				for(Message msg:objMulti.getMessages()){
					//msg.setMessageId(this.getMulticastMessageIDMaxClock(Project1.nodeID));
					 sendMessage(Project1.mapNodesByAddress.get(msg.getRecieverAddress()+String.valueOf(msg.getRecieverPort())).getNodeID(),msg);					 
				 }
			}
			 while(!sendQueue.isEmpty() && Project1.getClockReplies()==0){
				 MultiCastMessage objMulti= sendQueue.poll();
				 
				 String id=this.getMulticastMessageID(Project1.nodeID);
				 LC.incrementValue();
				 for(Message msg:objMulti.getMessages()){
					 msg.setMessageId(id);
					 msg.setLogicalClockValue(LC.getValue());
					 sendMessage(Project1.mapNodesByAddress.get(msg.getRecieverAddress()+String.valueOf(msg.getRecieverPort())).getNodeID(),msg);
					 Project1.incrementClockReplies();
				 }
			 }	 
		 }
		 System.out.println("Quitting");
	   }

	 void sendMessage(int sendTo, Message msgToSend) {
		 //get the connection object from already stored connections in the map
	        SctpChannel clientSocket = Project1.connectionSocket.get(sendTo);
	        if(clientSocket==null){
	        	System.out.println("null error");
	        	return;
	        }
	        
	        try {
	        	if(!(msgToSend.getMsgType()==0)){
	        		LC.incrementValue();
		        	msgToSend.setLogicalClockValue(LC.getValue());
	        	}
	        	sendMessage2(clientSocket, msgToSend);

	        } catch (CharacterCodingException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }

	    }

	    private  void sendMessage2(SctpChannel clientSock, Message message)
	            throws CharacterCodingException,IOException {

	        ByteBuffer Buffer = ByteBuffer.allocate(10000);
	        Buffer.clear();
	        byte[] serialized = null;
	        serialized = serialize(message);
	    

	        // Reset a pointer to point to the start of buffer
	        Buffer.put(serialized);
	        Buffer.flip();

	        try {
	            // Send a message in the channel
	            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
	            clientSock.send(Buffer, messageInfo);
	            String msgPrint="*********************************************";
	            msgPrint+="\nSending Time-"+System.currentTimeMillis()+"\n"
		                +message.printMessage();
	            msgPrint+="\n LC Value-"+LC.getValue();
	            msgPrint+="\n*********************************************";
	            System.out.println(msgPrint);
	       
	            //if(message.getMsgType()==0)
	            	//FileAppend.appendText(Constants.lOGMSGSEND+Project1.nodeID+".log",msgPrint);
	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (NullPointerException e) {
	            e.printStackTrace();
	        }
	    }

	    public byte[] serialize(Object obj) throws IOException {
	        ObjectOutputStream out;
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        out = new ObjectOutputStream(bos);
	        out.writeObject(obj);
	        return bos.toByteArray();
	    }
	    
		public String getUnicastMessageID(int sender,int destination){
			this.unicastMessageCount++;
			return "CR:"+sender+":"+destination+":"+this.unicastMessageCount;
		}
		public String getMulticastMessageID(int sender){
			this.multicastMessageCount++;
			return sender+":MM:"+this.multicastMessageCount;
		}
		public String getMulticastMessageIDMaxClock(int sender){
			return sender+":MM:"+this.multicastMessageCount+":MxCL";
		}
}
