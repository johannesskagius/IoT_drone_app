package com.example.drone_app;

import android.os.Bundle;
import android.os.Handler;
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
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference drone2Listener = database.getReference("drone2");
    private DatabaseReference request = database.getReference("request");
    private TextView xTextView, yTextView, statusTextView;
    private ArrayList<Position> path = new ArrayList<>();
    private ArrayList<Boolean> movedToPositions = new ArrayList<>();
    private Handler mHandler = new Handler();
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
    private Handler handler = new Handler();
    private Runnable waitAndCallMoveDrone = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, 500);
            moveDrone();
        }
    };

    private void moveDrone() {          //A formidable solution.
        String moveDir = null;
        if (path.isEmpty())
            path = getDronePath();
        if (movedToPositions.isEmpty()) {
            for (int i = 0; i < path.size(); i++) {
                movedToPositions.add(i == 0);
            }
        }
        //Get move Direction
        for (Boolean bool : movedToPositions) {
            if (!bool) {
                moveDir = chooseMoveDir(path.get(pathPos));     //Pathpos is = current target location
                targetPos = path.get(pathPos);
                break;
            }
        }
        if (moveDir == null) {
            return;
        }
        moveDrone(moveDir);
    }

    private void moveDrone(String moveDir) {
        moveDroneRef = drone2Listener;
        int changedValue;
        int x = 0;
        String value = null;
        switch (moveDir) {
            case "+x":       //Move drone positiv X angle
                moveDroneRef = moveDroneRef.child(POSITION_X);
                x += 1;
                changedValue = Integer.parseInt(droneXPosition) + x;
                value = "X";
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
        DatabaseReference ref = moveDroneRef;
        moveDrone(ref, changedValue, value);
    }

    private void moveDrone(DatabaseReference ref, int changedValue, String value) {
        ref.setValue(String.valueOf(changedValue));

        if (value.equalsIgnoreCase("X")) {
            droneXPosition = String.valueOf(changedValue);
        } else {
            droneYPosition = String.valueOf(changedValue);
        }


        Position dronePosition = new Position(droneXPosition + droneYPosition);
        int i = 0;
        for (Position currentTargetPosition : path) {
            if (currentTargetPosition.equals(dronePosition) && i != 0) {
                movedToPositions.set(pathPos, true);
                pathPos++;
            }
            if (i == pathPos) break;
            i++;
        }
        if(checkIfAllPathsAreDone()){
            request.child("active").setValue("Not active");
            moveDrone = false;
            drone2Listener.child(AVAILABLE).setValue("n");
        }
    }

    private Boolean checkIfAllPathsAreDone() {
        for (Boolean pathsDone : movedToPositions) {
            if (!pathsDone) return false;
        }
        return true;
    }

    private String chooseMoveDir(Position currentPosTarget) {
        int xDiff = currentPosTarget.getX() - Integer.parseInt(droneXPosition);
        int yDiff = currentPosTarget.getY() - Integer.parseInt(droneYPosition);

        if (xDiff < yDiff && xDiff < 0)
            return "-x";
        else if (xDiff < yDiff && xDiff > 0)
            return "+x";
        else if (xDiff > yDiff && yDiff < 0)
            return "-y";
        else if (xDiff == yDiff)
            return "-x";
        else {
            if (xDiff == 0 && yDiff < 0)
                return "-y";
            else if (xDiff == 0 && yDiff > 0)
                return "+y";
            else if (yDiff == 0 && xDiff < 0)
                return "-x";
            else if (yDiff == 0 && xDiff > 0)
                return "+x";
            else
                return "+y";
        }
    }

    private ArrayList<Position> getDronePath() {
        Position requestPosition = null;
        Position finishPosition = null;

        Position droneStartPosition = new Position(droneXPosition + droneYPosition);
        path.add(droneStartPosition);

        //Check drone start position to request. If drone need to pass PASSPOSITION it needs to be added to route
        if (startPosY != null || startPosX != null) {
            requestPosition = new Position(startPosX + startPosY);
            if (compareDroneStartToRequest(droneStartPosition, requestPosition)) {
                path.add(PASSPOSITION);
            }
            path.add(requestPosition);
        }

        //Check drone request finish position. If drone need to pass PASSPOSTION it needs to be added to route
        if (finishPosX != null || finishPosy != null) {
            finishPosition = new Position(finishPosX + finishPosy);
            if (compareDroneStartToRequest(requestPosition, finishPosition)) {
                path.add(PASSPOSITION);
            }
        }
        path.add(finishPosition);

        return path;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xTextView = findViewById(R.id.XValue);
        yTextView = findViewById(R.id.YValue);
        statusTextView = findViewById(R.id.status);
        getDronePosition();
        getRequest();
    }

    private void getRequest() {
        request.addValueEventListener(new ValueEventListener() {
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
                    if (!moveDrone) {
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

    private String convertYToCorrectFormat(String finishY) {
        String yValue = finishY;
        if (finishY.length() == 1) {
            yValue = "0" + finishY;
        }
        return yValue;
    }

    private void getDronePosition() {
        drone2Listener.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                droneXPosition = snapshot.child(POSITION_X).getValue().toString();
                droneYPosition = snapshot.child(POSITION_Y).getValue().toString();
                droneYPosition = convertYToCorrectFormat(droneYPosition);
                if (moveDrone) {
                    //handler.post(waitAndCallMoveDrone);
                    moveDrone();
                }

                xTextView.setText(droneXPosition);
                yTextView.setText(droneYPosition);
                // statusTextView.setText(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private Boolean compareDroneStartToRequest(Position droneStart, Position requestStart) {
        if (droneStart.getX() > 4 && requestStart.getX() < 4) {
            if (droneStart.getY() < 5 && requestStart.getY() > 5) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private Boolean allPathsDone() {
        if (movedToPositions.size() == 0)
            return true;
        for (Boolean bool : movedToPositions) {
            if (!bool) return false;
        }
        return true;
    }
}