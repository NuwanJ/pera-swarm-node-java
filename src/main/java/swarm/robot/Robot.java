package swarm.robot;

import org.json.simple.parser.ParseException;
import swarm.Interfaces.IRobotState;
import swarm.configs.MQTTSettings;
import swarm.mqtt.RobotMqttClient;
import swarm.mqtt.MqttMsg;
import swarm.robot.communication.DirectedCommunication;
import swarm.robot.communication.SimpleCommunication;
import swarm.robot.helpers.Coordinate;
import swarm.robot.helpers.MotionController;
import swarm.robot.helpers.RobotMQTT;
import swarm.robot.indicator.NeoPixel;
import swarm.robot.sensors.ColorSensor;
import swarm.robot.sensors.DistanceSensor;
import swarm.robot.sensors.ProximitySensor;

public abstract class Robot implements Runnable, IRobotState {

    // Sensors -----------------------------------------------------------
    public DistanceSensor distSensor;
    public ProximitySensor proximitySensor;
    public ColorSensor colorSensor;

    // Communication -----------------------------------------------------
    public SimpleCommunication simpleComm;
    public DirectedCommunication directedComm;

    // Output ------------------------------------------------------------
    public NeoPixel neoPixel;

    // Helper and Controller objects ------------------------------------
    public MotionController motion;
    public RobotMQTT robotMQTT;
    public Coordinate coordinates;
    public RobotMqttClient robotMqttClient;

    // Variables ---------------------------------------------------------
    protected int id;
    protected char reality;
    protected robotState state = robotState.WAIT;

    public Robot(int id, double x, double y, double heading, char reality) {

        this.id = id;
        this.reality = reality;

        robotMqttClient = new RobotMqttClient(MQTTSettings.server, MQTTSettings.port, MQTTSettings.userName, MQTTSettings.password, MQTTSettings.channel);

        coordinates = new Coordinate(id, x, y, heading, robotMqttClient);
        robotMQTT = new RobotMQTT(id, robotMqttClient, reality);

        // Request to create a robot instance at Simulator
        robotMQTT.robotCreate(coordinates.getX(), coordinates.getY(), coordinates.getHeading());

        delay(1000);
        this.motion = new MotionController(coordinates);
    }

    public void setup() {
        // Setup each module
        distSensor = new DistanceSensor(this, robotMqttClient);
        proximitySensor = new ProximitySensor(this, robotMqttClient);
        colorSensor = new ColorSensor(this, robotMqttClient);

        neoPixel = new NeoPixel(this, robotMqttClient);

        simpleComm = new SimpleCommunication(id, robotMqttClient);
        directedComm = new DirectedCommunication(id, robotMqttClient);

        coordinates.setCoordinate(coordinates.getX(), coordinates.getY(), coordinates.getHeading());
        coordinates.publishCoordinate();

    }

    // Robot getters and setters -----------------------------------------
    public int getId() {
        return this.id;
    }

    // -------------------------------------------------------------------
    @Override
    public void run() {
        setup();

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                loop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // DO NOT REMOVE OR EDIT THIS DELAY
            // TODO: implement as delay = 1000 - timeTaken(loop())
            delay(1000);

            try {
                handleSubscribeQueue();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // MQTT related methods ----------------------------------------------
    public void handleSubscribeQueue() throws ParseException {
        // Handle the messages in incoming queue

        // Important Update
        while(!robotMqttClient.inQueue.isEmpty()){
            //for (MqttMsg item : robotMqttClient.inQueue) {
            MqttMsg m = robotMqttClient.inQueue.poll();

            if (m != null) {
                switch (m.topicGroups[0]) {
                    case "robot":
                        // Robot messages
                        // System.out.println("robot message received");
                        robotMQTT.handleSubscription(this, m);
                        break;

                    case "sensor":
                        // Sensor messages

                        if (m.topicGroups[1].equals("distance")) {
                            // System.out.println("distance sensor message received");
                            distSensor.handleSubscription(this, m);
                        } else if (m.topicGroups[1].equals("color")) {
                            // System.out.println("color sensor message received");
                            colorSensor.handleSubscription(this, m);
                        } else if (m.topicGroups[1].equals("proximity")) {
                            // System.out.println("proximity sensor message received");
                            proximitySensor.handleSubscription(this, m);
                        }

                        break;
                    case "localization":
                        // Localization messages
                        // System.out.println("localization message received");

                        if (m.topic.equals("localization/update/?")) {
                            coordinates.handleSubscription(this, m);
                        }

                        break;
                    case "comm":
                        // Communication messages
                        // System.out.println("communication message received");
                        if (m.topicGroups[2].equals("simple")) {
                            simpleComm.handleSubscription(this, m);
                        } else {
                            directedComm.handleSubscription(this, m);
                        }
                        break;

                    case "output":
                        if ("NeoPixel".equals(m.topicGroups[1])) {
                            neoPixel.handleSubscription(this, m);
                        }
                        break;
                }
            }
        }
    }

    private void handlePublishQueue() {
        // Publish messages which are collected in the outgoing queue
        while(!robotMqttClient.outQueue.isEmpty()){
            // for (MqttMsg item : robotMqttClient.outQueue) {
            MqttMsg m = robotMqttClient.outQueue.poll();
            assert m != null;
            robotMqttClient.publish(m.topic, m.message, m.QoS);
        }
    }

    // State Change methods, implement from IRobotState -------------------
    @Override
    public void start() {
        System.out.println("pattern start on " + id);
        state = robotState.RUN;
    }

    @Override
    public void stop() {
        System.out.println("pattern stop on " + id);
        state = robotState.WAIT;
    }

    @Override
    public void reset() {
        System.out.println("pattern reset on " + id);
        state = robotState.BEGIN;
    }

    // Utility Methods ---------------------------------------------------
    public void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

}