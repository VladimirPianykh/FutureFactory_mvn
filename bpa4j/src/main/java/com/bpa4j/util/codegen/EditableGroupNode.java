package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author AI-generated
 * FIXME implement
 */
public class EditableGroupNode implements ProjectNode<EditableGroupNode>{
	public static class EditableGroupPhysicalNode implements PhysicalNode<EditableGroupNode>{
		private final File file;
		public EditableGroupPhysicalNode(File file){
			this.file=file;
		}
		@Override
		public void clear(){
			file.delete();
		}
		@Override
		public boolean exists(){
			return file.exists();
		}
		@Override
		public void persist(NodeModel<EditableGroupNode> model){
			if(file.exists()){ throw new IllegalStateException("Physical representation already exists: "+file.getAbsolutePath()); }
			try{
				if(file.getParentFile()!=null) file.getParentFile().mkdirs();
				String className=file.getName().replace(".java","");
				String s="public class "+className+" {}";
				Files.writeString(file.toPath(),s);
			}catch(IOException ex){
				throw new java.io.UncheckedIOException(ex);
			}
		}
		@Override
		public NodeModel<EditableGroupNode> load(){
			return new EditableGroupModel();
		}
	}
	
	public static class EditableGroupModel implements NodeModel<EditableGroupNode>{
	}
	
	private final PhysicalNode<EditableGroupNode> physicalNode;
	private final NodeModel<EditableGroupNode> model;

	public EditableGroupNode(PhysicalNode<EditableGroupNode> physicalNode){
		if(!physicalNode.exists()) throw new IllegalArgumentException("Physical representation does not exist");
		this.physicalNode=physicalNode;
		this.model=physicalNode.load();
	}
	public EditableGroupNode(PhysicalNode<EditableGroupNode> physicalNode,EditableGroupModel model){
		if(physicalNode.exists()) throw new IllegalArgumentException("Physical representation already exists");
		this.model=model;
		this.physicalNode=physicalNode;
		this.physicalNode.persist(model);
	}

	@Override
	public PhysicalNode<EditableGroupNode> getPhysicalRepresentation(){
		return physicalNode;
	}
	@Override
	public NodeModel<EditableGroupNode> getModel(){
		return model;
	}
}
