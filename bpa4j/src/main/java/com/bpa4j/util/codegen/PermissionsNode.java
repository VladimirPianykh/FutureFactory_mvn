package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/**
 * @author AI-generated
 */
public class PermissionsNode implements ProjectNode<PermissionsNode>{
	public static interface PermissionsPhysicalNode extends PhysicalNode<PermissionsNode>{
		
	}
	public static class FilePermissionsPhysicalNode implements PermissionsPhysicalNode{
		private final File file;
		public FilePermissionsPhysicalNode(File file){
			this.file=file;
		}
		@Override
		public void clear(){
			file.delete();
		}
		public void sync(NodeModel<PermissionsNode> model){
			PermissionsModel m=(PermissionsModel)model;
			try{
				while(!Files.isWritable(file.toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> permissionEnum=findPermissionEnum(cu);
				if(permissionEnum.isPresent()){
					EnumDeclaration enumDecl=permissionEnum.get();
					enumDecl.getEntries().clear();
					for(String perm:m.getPermissions())
						enumDecl.addEntry(new EnumConstantDeclaration(perm));
					Files.writeString(file.toPath(),cu.toString());
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public void persist(NodeModel<PermissionsNode> model){
			if(file.exists()) throw new IllegalStateException("Physical representation already exists: "+file.getAbsolutePath());
			// try{
			// 	if(file.getParentFile()!=null) file.getParentFile().mkdirs();
			// 	Files.writeString(file.toPath(),"public enum "+file.getName().replace(".java","")+" implements Permission {}");
			// }catch(IOException ex){
			// 	throw new UncheckedIOException(ex);
			// }
			throw new UnsupportedOperationException("Physical representation for PermissionsNode cannot be created.");
		}
		@Override
		public NodeModel<PermissionsNode> load(){
			try{
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> permissionEnum=findPermissionEnum(cu);
				if(permissionEnum.isPresent()){
					List<String> permissions=permissionEnum.get().getEntries().stream().map(EnumConstantDeclaration::getNameAsString).toList();
					return new PermissionsModel(permissions);
				}
				return new PermissionsModel(List.of());
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public boolean exists(){
			return file.exists();
		}
		private Optional<EnumDeclaration> findPermissionEnum(CompilationUnit cu){
			return cu.findAll(EnumDeclaration.class).stream().filter(enumDecl->enumDecl.getImplementedTypes().stream().anyMatch(type->type instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)type).getNameAsString().contains("Permission"))).findFirst();
		}
	}

	public static class PermissionsModel implements NodeModel<PermissionsNode>{
		private final List<String> permissions;
		public PermissionsModel(List<String> permissions){
			this.permissions=new ArrayList<>(permissions);
		}
		public List<String> getPermissions(){
			return Collections.unmodifiableList(permissions);
		}
		public void addPermission(String permission){
			if(permissions.contains(permission))
				throw new IllegalStateException(permission+" already exists.");
			permissions.add(permission);
		}
		public void removePermission(String permission){
			if(!permissions.contains(permission))
				throw new IllegalStateException(permission+" does not exist.");
			permissions.remove(permission);
		}
		public boolean hasPermission(String permission){
			return permissions.contains(permission);
		}
	}

	private final PhysicalNode<PermissionsNode> physicalNode;
	private final NodeModel<PermissionsNode> model;
	public PermissionsNode(PhysicalNode<PermissionsNode> physicalNode){
		if(!physicalNode.exists()) throw new IllegalArgumentException("Physical representation does not exist");
		this.physicalNode=physicalNode;
		this.model=physicalNode.load();
	}
	public PermissionsNode(PhysicalNode<PermissionsNode> physicalNode,List<String> permissions){
		if(physicalNode.exists()) throw new IllegalArgumentException("Physical representation already exists");
		this.model=new PermissionsModel(permissions);
		this.physicalNode=physicalNode;
		this.physicalNode.persist(model);
	}
	public PhysicalNode<PermissionsNode> getPhysicalRepresentation(){
		return physicalNode;
	}
	public NodeModel<PermissionsNode> getModel(){
		return model;
	}
	public void addPermission(String permission){
		((PermissionsModel)model).addPermission(permission);
		((FilePermissionsPhysicalNode)physicalNode).sync(model);
	}
	public void removePermission(String permission){
		((PermissionsModel)model).removePermission(permission);
		((FilePermissionsPhysicalNode)physicalNode).sync(model);
	}
	public List<String> getPermissions(){
		return((PermissionsModel)model).getPermissions();
	}
	public boolean hasPermission(String permission){
		return((PermissionsModel)model).hasPermission(permission);
	}
}