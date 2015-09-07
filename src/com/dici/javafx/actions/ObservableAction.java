package com.dici.javafx.actions;

interface ObservableAction extends Action {
	default void updateState(StateObserver observer) { };
}
