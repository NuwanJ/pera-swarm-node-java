package robotImplementations;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import swarm.robot.VirtualRobot;

public class ExperimentRobot extends VirtualRobot {

    long startTime;

    long[] roundDelay = new long[20];
    int responseCount = 0;

    public ExperimentRobot(int id, double x, double y, double heading) {
        super(id, x, y, heading);
    }

    public void setup() {
        super.setup();

        neoPixel.changeColor(0, 0, 0);
        coordinates.publishCoordinate();

        // Initiate delay registers
        for (int i = 0; i < 20; i++) {
            roundDelay[i] = 0;
        }
    }

    @Override
    public void loop() throws Exception {
        super.loop();
        // Anything specially check in continuously

//        if (state == robotState.RUN) {
//            if (responseCount == 9) {
//                System.out.println("All responses received successfully !");
//
//                for (int i = 0; i < 20; i++) {
//                    if (roundDelay[i] != 0) {
//                        JSONObject logMsg = new JSONObject();
//                        logMsg.put("id", i);
//                        logMsg.put("msg", roundDelay[i]);
//                        robotMqttClient.publish("robot/log", logMsg.toJSONString());
//                    }
//                }
//                reset();
//            }
//        }
    }

    @Override
    public void interrupt() {

    }

    @Override
    public void communicationInterrupt(String msg) {
        // System.out.println("communicationInterrupt on " + id + " with msg:" + msg);

        String[] s = msg.split(" ");
        int originId = Integer.parseInt(s[0]);

        if (s.length == 2 && originId != id) {
            // This is a message from the origin, lets reply
            // Format: <Origin> REQ

            // Send an ACK message to the originator
            simpleComm.sendMessage(s[0] + " " + id + " ACK");
            neoPixel.changeColor(0, 0, 128);

        } else if (s.length == 3 && originId == id) {
            // This is a reply ACK, <Origin> <Responder> ACK

            // lets log the message
            int responder = Integer.parseInt(s[1]);
            roundDelay[responder] = System.currentTimeMillis() - startTime;
            responseCount++;
            System.out.println(responseCount + " > " + responder + "=" + roundDelay[responder] + " from " + responder);

        }
    }

    public void start() {
        super.start();
        System.out.println("Algorithm started on robot " + id);
        // Things to do when start action
        neoPixel.changeColor(0, 128, 0);

        // Record the start time and send a REQ message to all nearby robots
        responseCount = 0;
        startTime = System.currentTimeMillis();
        simpleComm.sendMessage(id + " REQ");

        while(state == robotState.RUN){
            try {
                handleSubscribeQueue();
                if (responseCount == 9) {
                    System.out.println("All responses received successfully !");

                    for (int i = 0; i < 20; i++) {
                        if (roundDelay[i] != 0) {
                            JSONObject logMsg = new JSONObject();
                            logMsg.put("id", i);
                            logMsg.put("msg", roundDelay[i]);
                            robotMqttClient.publish("robot/log", logMsg.toJSONString());
                        }
                    }
                    reset();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reset(){
        super.reset();
        neoPixel.changeColor(0, 0, 0);
    }

}
