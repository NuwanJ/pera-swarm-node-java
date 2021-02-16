package swarm.robot.sensors;

import org.json.simple.parser.ParseException;
import swarm.mqtt.RobotMqttClient;

import java.util.HashMap;

import org.json.simple.JSONObject;
import swarm.mqtt.MqttMsg;
import swarm.robot.exception.SensorException;
import swarm.robot.Robot;
import swarm.robot.types.RGBColorType;

public class ColorSensor extends AbstractSensor {

    private final static int MQTT_TIMEOUT = 1000;

    private enum mqttTopic {COLOR_IN, COLOR_LOOK}

    private HashMap<mqttTopic, String> topicsSub = new HashMap<mqttTopic, String>();

    private boolean col_lock = false;

    RGBColorType color;

    public ColorSensor(Robot robot, RobotMqttClient m) {
        super(robot, m);
        subscribe(mqttTopic.COLOR_IN, "sensor/color/" + robotId);
        subscribe(mqttTopic.COLOR_LOOK, "sensor/color/" + robotId + "/?");
    }

    private void subscribe(mqttTopic key, String topic) {
        topicsSub.put(key, topic);      // Put to the queue
        robotMqttClient.subscribe(topic);   // Subscribe through MqttHandler
    }

    @Override
    public void handleSubscription(Robot robot, MqttMsg m) {
        // sensor/color/
        String topic = m.topic;
        String msg = m.message;

        if (topic.equals(topicsSub.get(mqttTopic.COLOR_IN))) {
            // sensor/color/{id}
            // TODO: Only for testing
            System.out.println("Color Input>" + msg);

            String[] arrSplit = msg.split(" ");
            color.setColor(Integer.parseInt(arrSplit[0]), Integer.parseInt(arrSplit[1]), Integer.parseInt(arrSplit[2]));

            col_lock = false;
            // robot.sensorInterrupt(color.toString());

        } else if (topic.equals(topicsSub.get(mqttTopic.COLOR_LOOK))) {
            // TODO: What we need to do in here ?
            // sensor/color/{id}/?
            System.out.println("Received: " + topic + "> " + color.toString());

        } else {
            System.out.println("Received (unknown): " + topic + "> " + color.toString());
        }

    }

    public RGBColorType getColor() throws Exception {
        // Publish to sensor/color/ -> {id: this.id}
        // Listen to sensor/color/{robotId} -> color
        // return the reading

        // Prepare the message
        JSONObject msg = new JSONObject();
        msg.put("id", robotId);
        msg.put("reality", "M"); // inform the requesting reality
        //System.out.println(msg.toJSONString());

        // Acquire the color sensor lock
        col_lock = true;

        robotMqttClient.publish("sensor/color", msg.toJSONString());
        robot.delay(250);

        long stratTime = System.currentTimeMillis();
        boolean timeout = false;

        while (col_lock && !timeout) {
            try {
                robot.handleSubscribeQueue();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            System.out.print(".");
            robot.delay(100);
            timeout = (System.currentTimeMillis() - stratTime > MQTT_TIMEOUT);
        }

        if (timeout) {
            throw new SensorException("Color sensor timeout");
        }

        System.out.println(color.toString());

        return color;
    }

    public void sendColor(int r, int g, int b, int ambient) {
        // Only for test, virtual robots will not invoke this.

        JSONObject obj = new JSONObject();
        obj.put("id", robotId);
        obj.put("R", r);
        obj.put("G", g);
        obj.put("B", b);
        obj.put("ambient", ambient);

        robotMqttClient.publish("sensor/color/", obj.toJSONString());
    }
}