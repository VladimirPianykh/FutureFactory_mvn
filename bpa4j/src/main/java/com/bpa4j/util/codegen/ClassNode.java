package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.SimpleName;

import lombok.Getter;
import lombok.Setter;

/**
 * @author AI-generated
 */
public abstract class ClassNode<T extends ClassNode<T>> implements ProjectNode<T>{
	public static abstract class FileClassPhysicalNode<V extends ClassNode<V>> implements ClassPhysicalNode<V>{
		private File file;
		private String packageName;
		public FileClassPhysicalNode(File file,String packageName){
			if(!file.getName().endsWith(".java"))throw new IllegalArgumentException("Only java files are permitted.");
			if(!file.getParentFile().toPath().endsWith(packageName.replace('.','\\')))throw new IllegalArgumentException("Bad package name.");
			this.file=file;
			this.packageName=packageName;
		}
		@Override
		public void clear(){
			file.delete();
		}
		@Override
		public boolean exists(){
			return file.exists();
		}
		public void rename(ProjectGraph project,String newName){
			try{
				String oldName=file.getName().substring(0,file.getName().lastIndexOf('.'));
				File f=new File(file.getParent()+"/"+newName+".java");
				if(f.exists()) return;
				file.renameTo(f);
				file=f;

				Files.walkFileTree(project.projectFolder.toPath(),new SimpleFileVisitor<Path>(){
					public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException{
						if(file.toString().endsWith(".java")){
							while(!Files.isWritable(file))
								Thread.onSpinWait();
							CompilationUnit cu=StaticJavaParser.parse(file);

							// Найти все использования имени класса
							cu.findAll(SimpleName.class).forEach(typeExpr->{
								if(typeExpr.getIdentifier().replaceAll(".*\\.","").equals(oldName)){
									typeExpr.setIdentifier(newName);
								}
							});

							Files.writeString(file,cu.toString());
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public File getLocation(){
			return file;
		}
		public String getPackageName(){
			return packageName;
		}
	}
	public static interface ClassPhysicalNode<V extends ClassNode<V>>extends PhysicalNode<V>{
		void rename(ProjectGraph project,String newName);
		@Override
		ClassModel<V> load();
	}
	public static class ClassModel<V extends ClassNode<V>> implements NodeModel<V>{
		@Getter
		@Setter
		private String name;
		public ClassModel(String name){
			this.name=name;
		}
	}

	protected final ClassPhysicalNode<T> physicalNode;
	protected ClassModel<T> model;

	/**
	 * Creates a new ClassNode from with the given physical node.
	 * </p>
	 * If PhysicalNode is associated with real node, then loads model from it.
	 * Otherwise, it's caller's responsibility to call {@link PhysicalNode#persist(NodeModel)}.
	 * Thus, this is both writing and reading constructor.
	 */
	public ClassNode(ClassPhysicalNode<T> physicalNode){
		this.physicalNode=physicalNode;
		if(physicalNode.exists())this.model=physicalNode.load();
	}

	@Override
	public ClassPhysicalNode<T> getPhysicalRepresentation(){
		return physicalNode;
	}

	@Override
	public ClassModel<T> getModel(){
		return model;
	}

	public synchronized void changeNameIn(ProjectGraph project,String name){
		// String oldName=((ClassModel<T>)model).getName();
		((ClassModel<T>)model).setName(name);
		((ClassPhysicalNode<T>)physicalNode).rename(project,name);
	}
	public String getName(){
		return ((ClassModel<T>)model).getName();
	}
}
