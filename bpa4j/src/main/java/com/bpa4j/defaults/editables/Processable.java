package com.bpa4j.defaults.editables;

import java.io.Serializable;

import com.bpa4j.core.User;
import com.bpa4j.SerializableFunction;
import com.bpa4j.core.Data.Editable;
import com.bpa4j.core.User.Permission;

public abstract class Processable extends Editable{
	public static class Stage implements Serializable{
		public String name;
		public Permission approver,rejecter;
		public int rejectionIndex;
		public SerializableFunction<Processable,Boolean>checker;
		public Stage(String name,Permission approver,SerializableFunction<Processable,Boolean>checker,Permission rejecter,int rejectionIndex){
			this.name=name;
			this.approver=approver;
			this.checker=checker;
			this.rejecter=rejecter;
			this.rejectionIndex=rejectionIndex;
		}
		public Stage(String name,Permission approver,Permission rejecter,int rejectionIndex){this(name,approver,null,rejecter,rejectionIndex);}
		public Stage(String name,Permission approver,SerializableFunction<Processable,Boolean>checker){this(name,approver,checker,null,0);}
		public Stage(String name,Permission approver){this(name,approver,null,0);}
	}
	public Stage[]stages;
	private int stageIndex;
	public int getStageIndex() {
		return stageIndex;
	}
	public Processable(String name,Stage...stages){super(name);this.stages=stages.clone();}
	public Stage getStage(){return stages[stageIndex];}
	public boolean isLastStage(){return stageIndex==stages.length-1;}
	public boolean approve(String comment){
		if(isLastStage())throw new IllegalStateException("Cannot approve an object with the last stage.");
		if(getStage().checker==null||getStage().checker.apply(this)){
			records.add(new ActionRecord('+'+getStage().name,User.getActiveUser()));
			++stageIndex;
			if(!comment.isBlank())records.add(new ActionRecord('>'+User.getActiveUser().login+':'+comment,User.getActiveUser()));
			return true;
		}else return false;
	}
	public void reject(String comment){
		stageIndex=getStage().rejectionIndex;
		records.add(new ActionRecord('-'+getStage().name,User.getActiveUser()));
		if(!comment.isBlank())records.add(new ActionRecord('>'+User.getActiveUser().login+':'+comment,User.getActiveUser()));
	}
}
