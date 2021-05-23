package robotImplementations;

import swarm.robot.VirtualRobot;

public class ObstacleAvoidRobot extends VirtualRobot {

    public ObstacleAvoidRobot(int id, double x, double y, double heading) {
        super(id, x, y, heading);
    }

    public void setup() {
        super.setup();
    }

    public void loop() throws Exception {
        super.loop();

        if(state==robotState.RUN) {
            double dist = distSensor.getDistance();

            if (dist < 25) {

                // Generate a random number in [-1000,1000] range
                // if even, rotate CW, otherwise rotate CCW an angle depends on the random number
                int random = -1000 + ((int) ((Math.random() * 2000)));
                int sign = (random % 2 == 0) ? 1 : -1;

                System.out.println("\t Wall detected, go back and rotate " + ((sign > 0) ? "CW" : "CCW"));

                // Go back a little
                motion.move(-100, -100, 900);

                // rotate
                int loopCount = 0; // to avoid infinity loop
                while (distSensor.getDistance() < 25 && loopCount < 5) {
                    motion.rotate(50 * sign, 1000);
                    loopCount++;
                }
                // TODO: This is a temp update to restrict the robot into arena
                // if (coordinates.getX() >= 90) coordinates.setX(85);
                // if (coordinates.getX() <= -90) coordinates.setX(-85);
                // if (coordinates.getY() >= 90) coordinates.setY(85);
                // if (coordinates.getY() <= -90) coordinates.setY(-85);

                // rotate a little
                motion.rotate(50 * sign, 500);

            } else {
                motion.move(100, 100, 1000);
            }
        }
    }
}
