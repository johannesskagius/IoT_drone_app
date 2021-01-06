package com.example.drone_app;

import java.util.Objects;

public class Position {
    private int x;
    private int y;

    public Position(String position) {
        this.x = getXPosition(position);
        this.y = getYPosition(position);
    }

    private int getYPosition(String position) {
        String y;
        if(position.length()==3) y = position.substring(1,3);
        else y = position.substring(1,2);


        return Integer.parseInt(y);
    }

    private int getXPosition(String position) {
        String xString = position.substring(0,1);
        return Integer.parseInt(xString);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position position = (Position) o;
        return getX() == position.getX() &&
                getY() == position.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
    }
}