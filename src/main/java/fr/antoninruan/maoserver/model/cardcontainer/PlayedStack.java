package fr.antoninruan.maoserver.model.cardcontainer;

import fr.antoninruan.maoserver.model.Card;

public class PlayedStack extends CardContainer {

    public Card pickLastCard() {
        if(keys.isEmpty())
            return null;

        Card card = keys.get(keys.size() - 1);
        keys.remove(card);
        return card;
    }

}
