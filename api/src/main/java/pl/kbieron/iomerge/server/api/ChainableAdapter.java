package pl.kbieron.iomerge.server.api;

/**
 * @see Chainable
 */
public abstract class ChainableAdapter<T> implements Chainable<T> {

	protected T nextInChain;

	@Override
	public void chain(T nextInChain) {
		this.nextInChain = nextInChain;
	}
}
