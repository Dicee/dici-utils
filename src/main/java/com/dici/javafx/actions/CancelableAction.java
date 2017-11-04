package com.dici.javafx.actions;

public abstract class CancelableAction extends AbstractAction {	
	@Override
	public final void updateState(StateObserver observer) { observer.handleReversibleStateChange(this); }	
	public abstract void cancel();
}
