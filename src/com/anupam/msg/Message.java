package com.anupam.msg;

import java.io.Serializable;

/**
 * Class Message
 * 3 Messsage Types:
 * 	msgType=0:first message
 *  msgType=1:clock reply
 *  msgType=2:max clock reply
 *  These message types are based on Skleen algorithm
 * @author Anupam Gangotia
 * Profile::http://en.gravatar.com/gangotia
 * github::https://github.com/agangotia
*/
public class Message implements Serializable{

	private static final long serialVersionUID = 1L;
	private String messageId;
	private String senderAddress;
	private int senderPort;
	

	private String recieverAddress;
	private int recieverPort;
	private int msgType;
	private String data;
	
	private int logicalClockValue;
	private boolean deliverable;
	

	

	public Message(String msgId,String senderAddress,int senderPort, String recieverAddress,int recvPort,int type,String data){
		this.messageId=msgId;
		this.senderAddress=senderAddress;
		this.senderPort=senderPort;
		this.recieverAddress=recieverAddress;
		this.recieverPort=recvPort;
		this.msgType=type;
		this.data=data;
		this.deliverable=false;
	}
	
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}

	public String getRecieverAddress() {
		return recieverAddress;
	}

	public void setRecieverAddress(String recieverAddress) {
		this.recieverAddress = recieverAddress;
	}

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
	public int getSenderPort() {
		return senderPort;
	}

	public void setSenderPort(int senderPort) {
		this.senderPort = senderPort;
	}

	public int getRecieverPort() {
		return recieverPort;
	}

	public void setRecieverPort(int recieverPort) {
		this.recieverPort = recieverPort;
	}
	
	public int getLogicalClockValue() {
		return logicalClockValue;
	}

	public void setLogicalClockValue(int logicalClockValue) {
		this.logicalClockValue = logicalClockValue;
	}
	
	public String getStringForSend(){
		return this.recieverAddress+","+this.recieverPort+","+this.data;
	}
	public String getStringForRecieve(){
		return this.senderAddress+","+this.senderPort+","+this.data;
	}
	
	public Message getClockReplyMessage(int time){
		Message msgClockreply=new Message(this.messageId,this.recieverAddress,this.recieverPort,this.senderAddress,this.senderPort,1,String.valueOf(time));
		return msgClockreply;
	}
	
	public String printMessage(){
		return "MID-"+this.messageId +
				"\nType-"+this.msgType+
				"\nSender-"+this.senderAddress+":"+this.senderPort+
				"\nReciever-"+this.recieverAddress+":"+this.recieverPort+
				"\nContent-"+this.data+
				"\nPiggybankClockValue-"+this.logicalClockValue;
	}
	public boolean isDeliverable() {
		return deliverable;
	}

	public void setDeliverable(boolean deliverable) {
		this.deliverable = deliverable;
	}

}
