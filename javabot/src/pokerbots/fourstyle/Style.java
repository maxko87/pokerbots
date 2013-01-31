package pokerbots.fourstyle;

import pokerbots.brains.GenericBrain;
import pokerbots.utils.StatAggregator.OpponentStats;

public interface Style {
	public String actionInitiator( float hisPredictedWinChance, GenericBrain b );
	public String actionReceiver( float hisPredictedWinChance, GenericBrain b );
	public String getName();
}
