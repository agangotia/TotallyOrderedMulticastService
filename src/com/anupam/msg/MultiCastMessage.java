package com.anupam.msg;

import java.util.ArrayList;

/**
 * Class MultiCastMessage
 * Consists of several unicast messages
 * @author Anupam Gangotia
 * Profile::http://en.gravatar.com/gangotia
 * github::https://github.com/agangotia
*/
public class MultiCastMessage {
	private ArrayList<Message> messages;
	
	

	public MultiCastMessage(){
		messages=new ArrayList<Message>();
	}
	
	public MultiCastMessage(ArrayList<Message> msgs){
		this.messages=msgs;
	}
	
	public ArrayList<Message> getMessages() {
		return messages;
	}

	public void setMessages(ArrayList<Message> messages) {
		this.messages = messages;
	}
}
