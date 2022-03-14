package fr.antoninruan.maoserver.model.cardcontainer;

import fr.antoninruan.maoserver.model.Card;

import java.util.ArrayList;


public class Hand extends CardContainer {

    private int id;

    private String name;

    public Hand(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Card getCard(int i) {
        return keys.get(i);
    }


}

