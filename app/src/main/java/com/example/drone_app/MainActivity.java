//Written By Josk3261@student.su.se

package com.example.drone_app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String POSITION_X = "position_X";
    private static final String POSITION_Y = "position_Y";
    private static final String AVAILABLE = "Busy";
    private static final String FINSIHX = "Finish x";
    private static final String FINSIHY = "Finish y";
    private static final String STARTPOSY = "startPos y";
    private static final String STARTPOSX = "startPos x";
    private static final Position PASSPOSITION = new Position("304");
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference drone2Listener = database.getReference("drone2");
    private final DatabaseReference request = database.getReference("request");
    private TextView xTextView, yTextView, statusTextView;
    private ArrayList<Position> path = new ArrayList<>();
    private final ArrayList<Boolean> movedToPositions = new ArrayList<>();
    private String droneXPosition;
    private DatabaseReference moveDroneRef;
    private String droneYPosition;
    private String startPosY;
    private String startPosX;
    private boolean moveDrone = false;
    private String finishPosX;
    private String finishPosy;
    private int pathPos = 1;
    private Position targetPos;


    private void moveDrone() {          //the first moveDrone function creates a path from the drones start position to the finish position
        String moveDir = null;

        if (path.isEmpty()) { //If the path is empty it needs to be created
            createPath();   //Create the path if it's empty
        }
        //Get move Direction
        moveDir = getMoveDirr(moveDir);
        if (moveDir == null) {
            return;
        }
        moveDrone(moveDir); //Call second moveDrone method when we know the direction
    }

    private String getMoveDirr(String moveDir) {
        for (Boolean bool : movedToPositions) { //creates a for loop that runs the movedToPositions (Boolean array).
            if (!bool) {                        //if the current boolean is false then that's the next target.
                moveDir = chooseMoveDir(path.get(pathPos));     //Pathpos is = current target location chooseMoveDirr decides which direction to move next. move to line 150 to read about that method
                targetPos = path.get(pathPos);       //Sett the target position
                break;
            }
        }
        return moveDir;
    }

    private void createPath() {
        path = getDronePath();  //Move to line 167 to view this method.
        if (movedToPositions.isEmpty()) {   //since an arraylist with positions can't be checked if they been reached we need to create an arraylist with the same amount of values in booleans.
            for (int i = 0; i < path.size(); i++) { //This makes it possible to check if a positions has been reached.
                movedToPositions.add(i == 0);   //The first positions is the drones start position. This one is already reached and should therefore be true from start.
            }
        }
    }

    private void moveDrone(String moveDir) {    //Second moveDrone method. Here we are going to set the server reference and the actual value to change to.
        moveDroneRef = drone2Listener;  //drone2listener is the exact reference to the server for drone2. Later on we extend with for example drone2/positionX so that we can set the correct value.
        int changedValue;
        int x = 0;
        String value = null;
        switch (moveDir) {      // This switch "loop" decides case based on the direction we got in the first moveDrone method.
            case "+x":       //Move drone positiv X angle
                moveDroneRef = moveDroneRef.child(POSITION_X);  //Extends the reference
                x += 1;     //Adds postive 1 to the dronesXPosition.
                changedValue = Integer.parseInt(droneXPosition) + x;    //combines the current value with positive 1 for the new value
                value = "X";                                    //creates a value x. (will be explained later)
                break;
            case "-x":       //Move drone negativ X angle
                moveDroneRef = moveDroneRef.child(POSITION_X);
                x -= 1;
                changedValue = Integer.parseInt(droneXPosition) + x;
                value = "X";

                break;
            case "+y":       //Move drone positiv Y angle
                moveDroneRef = moveDroneRef.child(POSITION_Y);
                x += 1;
                changedValue = Integer.parseInt(droneYPosition) + x;
                value = "Y";
                break;
            case "-y":       //Move drone negativ Y angle
                moveDroneRef = moveDroneRef.child(POSITION_Y);
                x -= 1;
                changedValue = Integer.parseInt(droneYPosition) + x;
                value = "Y";
                break;
            default:
                changedValue = 0;
        }
        moveDrone(changedValue, value);//now we know the moveDirection, t
        // he new value of the drone in that axis and the databasereferance to the exakt value.
        // Then we call the last method to execute our move.
    }

    private void moveDrone(int changedValue, String value) {
        moveDroneRef.setValue(String.valueOf(changedValue));        //We set the drones new value in the database.

        if (value.equalsIgnoreCase("X")) {          //this makes sure both values are always updated with the latest value.
            droneXPosition = String.valueOf(changedValue);
        } else {
            droneYPosition = String.valueOf(changedValue);
        }

        Position dronePosition = new Position(droneXPosition + droneYPosition); //creates a position called drone position.
        int i = 0;
        for (Position currentTargetPosition : path) {   //Loops through the path array to see what positions are done
            if (currentTargetPosition.equals(dronePosition) && i != 0) {    //If currentTargetPosition is the same as drone position and "i" isn't 0. if the integer "i" is
                // zero that means that the positions we are comparing is the drone start position which cannot be allowed.
                movedToPositions.set(pathPos, true);                        //if "i" isn't zero then we made it to an actual target which means we want to set the correct value in our boolean array
                //to true and also add a pathPos++ so that we update our taget next iteration.
                pathPos++;
            }
            if (i == pathPos) {//if i == pathPos it's better to break the loop since if we target the
                break;          // request position we don't need to check if the finish position is done.
            }
            i++;
        }
        if (checkIfAllPathsAreDone()) { // at last we check if all paths are done because if they are we need to do somethings
            request.child("active").setValue("inActive"); // We set the request to inActive
            moveDrone = false;                              //moveDrone to false
            drone2Listener.child(AVAILABLE).setValue("n");  //and the drone to available
            path.clear();
            movedToPositions.clear();
            pathPos = 1;
        }
    }

    private Boolean checkIfAllPathsAreDone() {  //This method loops through bool array movedToPositionif it founds a false value it returns false otherwise it returns true.
        for (Boolean pathsDone : movedToPositions) {
            if (!pathsDone) return false;
        }
        return true;
    }

    private String chooseMoveDir(Position currentPosTarget) {       // This method decides in which direction the drone should move next.
        int xDiff = currentPosTarget.getX() - Integer.parseInt(droneXPosition); //Count the differances in X,Y axis.
        int yDiff = currentPosTarget.getY() - Integer.parseInt(droneYPosition);

        if (xDiff < yDiff && xDiff < 0)     //if the x value is less than Y and less then zero move -x direction
            return "-x";
        else if (xDiff < yDiff && xDiff > 0)         //if the x value is less than Y but bigger then zero move +x direction
            return "+x";
        else if (xDiff > yDiff && yDiff < 0)         //if the x value is greater than Y but less then zero move -y direction
            return "-y";
        else if (xDiff == yDiff)           //if the x value is equal to Y move -x
            return "-x";
        else {
            if (xDiff == 0 && yDiff < 0)        //If x== 0 and Y is less than 0 move -y
                return "-y";
            else if (xDiff == 0 && yDiff > 0)   //If x== 0 and Y is greater than 0 move +y
                return "+y";
            else if (yDiff == 0 && xDiff < 0)   //If y== 0 and x is greater than 0 move -x
                return "-x";
            else if (yDiff == 0 && xDiff > 0)   //If y== 0 and x is greater than 0 move +x
                return "+x";
            else
                return "+y";        // if nothing else matches just move +y
        }
    }

    private ArrayList<Position> getDronePath() {        // this method creates the drone path.
        Position requestPosition = null;            //it creates a request position which is null at first
        Position finishPosition = null;             //it creates a request position which is null at first

        Position droneStartPosition = new Position(droneXPosition + droneYPosition);
        path.add(droneStartPosition);       //the drones start position is added.

        //Check drone start position to request. If drone need to pass PASSPOSITION it needs to be added to route.
        //PASSPOSITION is a position the drone need to pass if it wants to move between some areas. This makes sure it doesn't fly where it can't.
        if (startPosY != null || startPosX != null) {
            requestPosition = new Position(startPosX + startPosY);  //Store values in the request position.
            if (comparePosition(droneStartPosition, requestPosition)) {  //here we compare if the drone needs to pass this passposition if it does it adds the pass position to the path.
                path.add(PASSPOSITION);
            }
            path.add(requestPosition);  //here we finally add the request position to the path array
        }

        //Check drone request finish position. If drone need to pass PASSPOSTION it needs to be added to route
        // we repeat the procedure above to create a path to the finish position.
        if (finishPosX != null || finishPosy != null) {
            finishPosition = new Position(finishPosX + finishPosy);
            if (comparePosition(requestPosition, finishPosition)) {
                path.add(PASSPOSITION);
            }
        }
        path.add(finishPosition);
        return path;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //This are the method that are runned directly to instantiate the view, and set the listeners.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xTextView = findViewById(R.id.XValue);
        yTextView = findViewById(R.id.YValue);
        statusTextView = findViewById(R.id.status);
        getDronePosition();
        getRequest();
    }

    private void getRequest() {
        request.addValueEventListener(new ValueEventListener() {    //This method gets the request from the server. Ones it's changed on the server it collects the values.
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String active = snapshot.child("active").getValue().toString();
                if (active.equalsIgnoreCase("active")) {
                    finishPosX = snapshot.child(FINSIHX).getValue().toString();
                    finishPosy = snapshot.child(FINSIHY).getValue().toString();
                    finishPosy = convertYToCorrectFormat(finishPosy);
                    startPosY = snapshot.child(STARTPOSY).getValue().toString();
                    startPosY = convertYToCorrectFormat(startPosY);
                    startPosX = snapshot.child(STARTPOSX).getValue().toString();
                    drone2Listener.child(AVAILABLE).setValue("busy");
                    if (!moveDrone) {               //if moveDrone is false it makes the first move for the drone. sets the moveDrone to true and calls the method moveDrone the first time.
                        moveDrone = true;
                        moveDrone();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String convertYToCorrectFormat(String finishY) {//since the y value can be row 2 or row 14 the server may return a single value or a double but we want it always to be two values
        String yValue = finishY;                //even if it's a 02. This method makes sure of that.
        if (finishY.length() == 1) {
            yValue = "0" + finishY;
        }
        return yValue;
    }

    private void getDronePosition() {   //this is the drone listener.
        drone2Listener.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                droneXPosition = snapshot.child(POSITION_X).getValue().toString(); // collects the important values.
                droneYPosition = snapshot.child(POSITION_Y).getValue().toString();
                droneYPosition = convertYToCorrectFormat(droneYPosition);
                if (moveDrone) {                    //first move will be done by the request listener but we want to drone to move the second and the third time it self.
                    try {                           // So the first server makes the drone move the first time. The values are changed on the server and this method calls because the values are changed
                        Thread.sleep(1000); //moveDrone is true because we sat it to true in requestlistener.
                    } catch (InterruptedException e) {//We wait 1000millis and then calls moveDrone() to simulate the time it takes for the drone to move
                        e.printStackTrace();
                    }
                    moveDrone();
                }
                xTextView.setText(droneXPosition);
                yTextView.setText(droneYPosition);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private Boolean comparePosition(Position droneStart, Position requestStart) {   //this method compare the positions if the drone needs to pass the passposition or not.
        if (droneStart.getX() > 4 && requestStart.getX() < 4) {
            return droneStart.getY() < 5 && requestStart.getY() > 5;
        }
        return false;
    }
}