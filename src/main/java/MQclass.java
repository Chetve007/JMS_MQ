import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;

public class MQclass {

    static MQQueueConnection mqConn;
    static MQQueueConnectionFactory mqCF;
    static MQQueueSession mqQSession;
    static MQQueue mqIn;
    static MQQueue mqOut;
    static MQQueueSender mqSender;
    static MQQueueReceiver mqReceiver;
    static MessageProducer replyProd;

    public static void main(String[] args) {

        Thread threadOne = new Thread(() -> {
            String host = "localhost";        // хост, где расположен MQ-сервер
            String port = "1414";        // порт для работы с MQ-сервером
            String mqQManager = "api_example"; // менеджер очередей MQ
            String mqQChannel = "SYSTEM.DEF.SVRCONN";    // Канал для подключения к MQ-серверу
            String mqQIn = "Hello";    // Очередь входящих сообщений
            String mqQOut = "Bye";    // Очередь исходящих сообщений

            try {
                mqCF = new MQQueueConnectionFactory();
                mqCF.setHostName(host);
                mqCF.setPort(Integer.parseInt(port));
                mqCF.setQueueManager(mqQManager);
                mqCF.setChannel(mqQChannel);
                mqCF.setTransportType(WMQConstants.TIME_TO_LIVE_UNLIMITED);

                mqConn = (MQQueueConnection) mqCF.createQueueConnection();
                mqQSession = (MQQueueSession) mqConn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);

                mqIn = (MQQueue) mqQSession.createQueue("SomeInQueue"); // входная
                mqOut = (MQQueue) mqQSession.createQueue("SomeOutQueue"); // выходная

                mqSender = (MQQueueSender) mqQSession.createSender(mqOut);
                mqReceiver = (MQQueueReceiver) mqQSession.createReceiver(mqIn);
                replyProd = mqQSession.createProducer(null);
                replyProd.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                mqConn.start();

                mqReceiver.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message msg) {
                        if (msg instanceof TextMessage) {
                            TextMessage tMsg = (TextMessage) msg;
                            try {
                                String msgText = ((TextMessage) msg).getText();
                                mqQSession.commit();
                                sendAnswer(msg);
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
        threadOne.start();
    }

    public static void sendAnswer(Message msg) {
        String xmlAns = "Something";
        try {
            TextMessage answer = mqQSession.createTextMessage(xmlAns); //xmlAns – ответная xml
            answer.setJMSCorrelationID(msg.getJMSMessageID());
            mqSender.send(answer);
            mqQSession.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
