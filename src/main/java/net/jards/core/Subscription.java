package net.jards.core;

public interface Subscription {

	public void stop();
	public boolean isReady();
	public int getId();
}

/*
* ready?
*
* */