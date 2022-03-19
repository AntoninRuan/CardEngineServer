package fr.antoninruan.maoserver.utils;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Emote {

    private static List<Emote> emotes = new ArrayList<>();

    private String name;

    public static List<Emote> getEmotes() {
        return emotes;
    }

    public Emote(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("name", this.name);
        return object;
    }

}
