package fr.antoninruan.maoserver.model.cardcontainer;

import fr.antoninruan.maoserver.model.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CardContainer {

    protected final List<Card> keys = new ArrayList<>();

    public List<Card> getCards() {
        return keys;
    }

    public int getSize() {
        return keys.size();
    }

    public void add(Card card) {
        this.keys.add(card);
    }

    public void remove(Card card) {
        this.keys.remove(card);
    }

//    public abstract void moveCardTo(Card card, CardContainer dest);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardContainer cardContainer = (CardContainer) o;
        return keys.equals(cardContainer.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(keys.toArray());
    }

}
