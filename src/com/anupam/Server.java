package com.anupam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import com.anupam.clock.LogicalClock;
import com.anupam.fileAppend.FileAppend;
import com.anupam.msg.Message;
import com.anupam.msg.MultiCastMessage;
import com.sun.nio.sctp.SctpChannel;


/**
 * Reciever Thread:
 * This thread recieves messages:
 * BAsed on message type fills the send queues & recieve queues
 * 
* @author Anupam Gangotia
* Profile::http://en.gravatar.com/gangotia
* github::https://github.com/agangotia
*/
public class Server extends Thread {

	
	private LogicalClock LC;
	ConcurrentHashMap<String, Message> mapClockReplies;
	BlockingQueue<MultiCastMessage> sendMaxClock;
	BlockingQueue<Message> sendClockReplyQueue;
	//BlockingQueue<Message> recvQueue;
	Comparator<Message> comparator; 
    PriorityQueue<Message> delvrQueue; 


	/**
	 * The constructor for server execution.
	 * 
	 * @param port
	 *            : specifies the port number, where this service will run
	 * @throws IOException
	 */
	public Server( LogicalClock LC,
			BlockingQueue<MultiCastMessage> sendMaxClock,
			BlockingQueue<Message> sendClockReplyQueue,
			BlockingQueue<Message> recvQueue,
			ConcurrentHashMap<String, Message> mapClockReplies)
			throws IOException {
		
		this.LC = LC;
		this.sendMaxClock = sendMaxClock;
		this.sendClockReplyQueue = sendClockReplyQueue;
		//this.recvQueue = recvQueue;
		this.mapClockReplies = mapClockReplies;
		this.comparator = new MessageComparator();
        this.delvrQueue= new PriorityQueue<Message>(Constants.SIZERECVQ, comparator);
	}

	/**
	 * Once Server is up, it is continuously waiting for clients to connect.
	 * Whenever a particular client connects, it recieves the message, and sends
	 * a reply.
	 * 
	 */
	public void run() {
	        while (true && Project1.applicationRunning) {

	            ByteBuffer byteBuffer= ByteBuffer.allocate(10000);

	            try {

	                for (int id : Project1.connectionSocket.keySet()) {
	                    SctpChannel schnl = Project1.connectionSocket.get(id);

	                    byteBuffer.clear();
	                    schnl.configureBlocking(false);
	                    schnl.receive(byteBuffer, null,null);
	                    byteBuffer.flip();

	                    if (byteBuffer.remaining() > 0) {
	                        Message receivedMsg = (Message) deserialize(byteBuffer
	                                .array());
	                        LC.incrementValue(receivedMsg.getLogicalClockValue());
	                        String msgPrint="*********************************************";
	        	            msgPrint+="\nRecieved  Time-"+System.currentTimeMillis()+"\n"
	        		                +receivedMsg.printMessage();
	        	            msgPrint+="\n LC Value-"+LC.getValue();
	        	            msgPrint+="\n*********************************************";
	                        System.out.println(msgPrint);
	                        

	                        if(receivedMsg.getMsgType()==0){
	        					//FileAppend.appendText(Constants.lOGMSGRECVD+Project1.nodeID+".log", receivedMsg.getStringForRecieve()+","+LC.getValue());
	        					Message clockReply=receivedMsg.getClockReplyMessage(LC.getValue());
	        					sendClockReplyQueue.add(clockReply);
	        					//put it in queue
	        					//recvQueue.add(receivedMsg);
	        					delvrQueue.add(receivedMsg);
	                        }else if(receivedMsg.getMsgType()==1){
	                        	//FileAppend.appendText(Constants.lOGMSGRECVD+Project1.nodeID+".log", receivedMsg.getStringForRecieve()+","+LC.getValue());
	        					//clock replies
	        					mapClockReplies.put(receivedMsg.getSenderAddress()+receivedMsg.getSenderPort(), receivedMsg);
	        					
	        					Project1.decrementClockReplies();
	        					if(Project1.getClockReplies()==0){
	        						int maxClockValue=0;
	        						 ArrayList<Message> messages=new  ArrayList<Message>();
	        						 //System.out.println("Multicasting clock replies for"+mapClockReplies.size());
	        						for (Message temp : mapClockReplies.values()) {
	        						    if(Integer.parseInt(temp.getData())>maxClockValue)
	        						    	maxClockValue=Integer.parseInt(temp.getData());
	        						}
	        						System.out.println("***Max clock Value***"+maxClockValue);
	        						for (Message temp : mapClockReplies.values()) {
	        							Message msgMaxClockreply=new Message(temp.getMessageId(),temp.getRecieverAddress(),temp.getRecieverPort(),temp.getSenderAddress(),temp.getSenderPort(),2,String.valueOf(maxClockValue));
	        							messages.add(msgMaxClockreply);
	        						}
	        						mapClockReplies.clear();
	        						sendMaxClock.add(new MultiCastMessage(messages));
	        					} 
	        				}
	        				else if(receivedMsg.getMsgType()==2){
	        					//FileAppend.appendText(Constants.lOGMSGRECVD+Project1.nodeID+".log", receivedMsg.getStringForRecieve()+","+LC.getValue());
	        					//System.out.println("Final Clock Value recieved"+receivedMsg.getData());
	        					//This is the final clock value recieved
	        					 System.out.println("PacketID"+receivedMsg.getMessageId());
	        					movePacket(receivedMsg.getMessageId(),Integer.parseInt(receivedMsg.getData()));
	        					deliverPacket(receivedMsg.getMessageId());
	        				}
	                    }
	                    byteBuffer.clear();
	                }// end for
	                
	            } catch (IOException e) {
	                e.printStackTrace();
	            } catch (ArrayIndexOutOfBoundsException e) {
	            } catch (NullPointerException e) {
	                e.printStackTrace();
	            }finally {
	                
	            }

	        }
	        System.out.println("Quitting");

	}
	
	public void movePacket(String packetId, int val){
		
		Iterator<Message> it=delvrQueue.iterator();
		Message traverse=null;
		while(it.hasNext()){
			traverse=(Message)it.next();
			if(traverse.getMessageId().equals(packetId)){
				//System.out.println("PacketID Found");
				//traverse.setDeliverable(true);
				break;
			}
				
		}
		if(traverse!=null){
			delvrQueue.remove(traverse);
			traverse.setDeliverable(true);
			traverse.setLogicalClockValue(val);
			delvrQueue.add(traverse);	
		}
		
		//System.out.println("PacketID Not Found");
	}
	
	public void deliverPacket(String id){
			if(delvrQueue.size()>0){
				Message objrecvMsg=delvrQueue.peek();
				System.out.println("Original Value"+objrecvMsg.getLogicalClockValue());
				if(objrecvMsg.isDeliverable()==true){
					delvrQueue.poll();
					//then only deliver
					System.out.println("Message Delivered"+objrecvMsg.getData());
					
					String msgPrint="*********************************************";
    	            msgPrint+="\nRecieved  Time-"+System.currentTimeMillis()+"\n"
    		                +objrecvMsg.printMessage();
    	            msgPrint+="\n LC Value-"+LC.getValue();
    	            msgPrint+="\n*********************************************";
                    System.out.println(msgPrint);
					FileAppend.appendText(Constants.lOGMSGRECVD+Project1.nodeID+".log", objrecvMsg.getMessageId());
					
				}
			}
	}
	
	/*
	public void deliverPacket(int timeVal){
		boolean check=true;
		while(check){
			if(recvQueue.size()>0){
				Message objrecvMsg=recvQueue.peek();
				System.out.println("Original Value"+objrecvMsg.getLogicalClockValue());
				if(objrecvMsg.getLogicalClockValue()<=timeVal){
					//then only deliver
					System.out.println("Message Delivered"+objrecvMsg.getData());
					
					String msgPrint="*********************************************";
    	            msgPrint+="\nRecieved  Time-"+System.currentTimeMillis()+"\n"
    		                +objrecvMsg.printMessage();
    	            msgPrint+="\n LC Value-"+LC.getValue();
    	            msgPrint+="\n*********************************************";
                    System.out.println(msgPrint);
					FileAppend.appendText(Constants.lOGMSGRECVD+Project1.nodeID+".log", objrecvMsg.getMessageId());
					recvQueue.poll();
				}else{
					check=false;
				}
			}else{
				//System.out.println("Recieve Queue is empty");
				//log.error("Recieve Queue is empty");
				check=false;
			}
		}
		
	}*/


	public static Object deserialize(byte[] obj) {
		
		ByteArrayInputStream bos = new ByteArrayInputStream(obj);
		
		try {
			ObjectInputStream in = new ObjectInputStream(bos);
			return in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.print("Error in deserialization");
		}
		return null;
	}

}

class MessageComparator implements Comparator<Message>
{
    @Override
    public int compare(Message a, Message b)
    {
        return a.getLogicalClockValue()-b.getLogicalClockValue();
    }
}
