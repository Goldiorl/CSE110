package edu.ucsd.cse110.client;

import java.util.Scanner;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class EnterChatRoom implements MessageListener {
	/** Username associated with the client */
	private String username;

	private static int ackMode;
	// private static String clientQueueName;
	
	//private static String clientTopicName;
	private boolean flag = true;
	private boolean transacted = false;
	
	//Create the necessary variables to connect to the server 
	private MessageProducer producer;
	private MessageProducer producer_chatroom;
	private MessageConsumer responseConsumer;
	private MessageConsumer responseConsumer_chatroom;
	private Session session;
	private Connection connection;
	private String[] ChatRoomStringList;
	private Destination producerQueue;
	private Destination consumerQueue;
	private String currentChatRoom;

	public EnterChatRoom(String username) throws JMSException{
		this.username=username;
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				ClientConstants.messageBrokerUrl);
		
		try {

			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(transacted, ClientConstants.ackMode);
			Destination adminQueue = session.createQueue(ClientConstants.consumeTopicName);
			this.producer = session.createProducer(adminQueue);
			this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			//set consumer
			consumerQueue = session.createTemporaryTopic();
			responseConsumer = session.createConsumer(consumerQueue);
			responseConsumer.setMessageListener(this);
			System.out.println("You are attemping to enter a chatroom....");
			System.out.println("Current chatrooms are:");
			System.out.println("Please enter the name of the chatroom you want to join in");
			commandChatRoom("listchatroom");
			selectChatRoom();
			//responseConsumer.close(); //unsubscribe from the temporary topic used to transmit chatroomlist
			inChatRoom();
		} catch (JMSException e) {
			// Handle the exception appropriately
		}

		
	}


	public void onMessage(Message message) {
		try {
			ChatRoomStringList = ((TextMessage)message).getText().split(" ");
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String messageText = null;
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				messageText = textMessage.getText();

				
				System.out.print("\n\"" + messageText 
						+ "\"\n\n>>");
			}
		} catch (JMSException e) {
			// Handle the exception appropriately
		}
		
	}
	
	public void commandChatRoom(String command) throws JMSException{
		txtSender(command,command);
	
    }
	

	
	public void selectChatRoom() throws JMSException{
		// Do you want to create one? Implement later and also:created chatroom never deleted
		boolean chatroomflag=false;
		String chatroomname = null;
		//System.out.println("Please enter the name of the chatroom you want to join in");
		while(!chatroomflag){
		System.out.print(">>");
		
		// Now create the actual message you want to send
		Scanner keyboard = new Scanner(System.in);
		chatroomname = keyboard.nextLine();
		for (String chatroomentry:ChatRoomStringList){
			if (chatroomname.equals(chatroomentry)) chatroomflag=true;;
			
		}
		
		if(!chatroomflag){
			System.out.println("chatroom name is case sensitive, your chatroom name is not in the list, please reselect");			
		}	
		}
		
		//send chatroom login message:
		currentChatRoom=chatroomname;
		txtSender(username+" "+currentChatRoom,"chatroomlogin");
		
		Destination consumeTopic_chatroom = session.createTopic(chatroomname);
		this.producer_chatroom = session.createProducer(consumeTopic_chatroom);
		this.producer_chatroom.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		//Set consumer	
		responseConsumer_chatroom = session.createConsumer(consumeTopic_chatroom);
		responseConsumer_chatroom.setMessageListener(this);
		return;
	    	
    }
	
	
	
	public void inChatRoom(){

		try {
						
			while (flag) {
				System.out.print(">>");
				
				// Now create the actual message you want to send
				Scanner keyboard = new Scanner(System.in);
				String message = keyboard.nextLine();
				
				if ("Command:LISTCHATROOM".equalsIgnoreCase(message)) {
					
					System.out.println("The current chatroom list");
					commandChatRoom("listchatroom");
					continue;
				}
				
				if ("Command:LISTCHATROOMUSERS".equalsIgnoreCase(message)) {
					
					System.out.println("The current chatroom userlist is:");
					txtSender(username+" "+currentChatRoom,"listchatroomusers");
					continue;
				}
				
				if ("Command:CREATECHATROOM".equalsIgnoreCase(message)) {
					System.out.println("please enter the name of the chatroom you want to create");
					System.out.print(">>");
					String chatroomname = keyboard.nextLine();
					txtSender("createchatroom"+" "+chatroomname,"createchatroom");
					continue;
				}
				
				if ("Command:quitchatroom".equalsIgnoreCase(message)) {
					
					System.out.println("Client quits current chatroom,return to default"
							+ " interface");
					txtSender(username+" "+currentChatRoom,"chatroomlogout");
					connection.close();
					return;
				}
				
				message="["+username+"]"+":"+message;
				TextMessage txtMessage = session.createTextMessage();
				txtMessage.setText(message);


				this.producer_chatroom.send(txtMessage);
			}
		
		} catch (JMSException e) {
			e.printStackTrace();
		}
	
	


	}
	
	public void txtSender(String content, String correlationid) throws JMSException{
		TextMessage txtMessage = session.createTextMessage();
		txtMessage.setText(content);
		txtMessage.setJMSReplyTo(consumerQueue);
		String correlationId = correlationid;
		txtMessage.setJMSCorrelationID(correlationId);
		this.producer.send(txtMessage);
	}
	
	public static void main(String[] args) throws JMSException{
		new EnterChatRoom("testname");
	}
	
}
