import com.ibm.mq.jms.*;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;

public class MQclass {

    String host = "localhost";        // хост, где расположен MQ-сервер
    String port = "1414";        // порт для работы с MQ-сервером
    String mqQManager = "api_example"; // менеджер очередей MQ
    String mqQChannel = "SYSTEM.DEF.SVRCONN";    // Канал для подключения к MQ-серверу
    String mqQIn = "MQ.Incoming";    // Очередь входящих сообщений
    String mqQOut = "MQ.Outgoing";    // Очередь исходящих сообщений

    MQQueueConnection mqConn;
    MQQueueConnectionFactory mqCF = new MQQueueConnectionFactory();
    MQQueueSession mqQSession;
    MQQueue mqIn;
    MQQueue mqOut;
    MQQueueSender mqSender;
    MQQueueReceiver mqReceiver;
    MessageProducer replyProd;

    public void mqSettings() {
        mqCF.setHostName(host);
        try {
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
            this.replyProd = this.mqQSession.createProducer(null);
            this.replyProd.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            mqConn.start();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    MessageListener listener = msg -> {
        if (msg instanceof TextMessage) {
            TextMessage tMsg = (TextMessage) msg;
            try { String msgText = ((TextMessage) msg).getText();
            } catch (JMSException e) { e.printStackTrace(); }
        }

    };

    public void sendAns(Message msg) {
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
