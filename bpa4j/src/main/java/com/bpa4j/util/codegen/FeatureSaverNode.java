package com.bpa4j.util.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import com.bpa4j.core.Root;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.Getter;

public class FeatureSaverNode extends ClassNode<FeatureSaverNode> implements InternalNode<FeatureSaverNode>{
	public static class FeatureSaverModel extends ClassModel<FeatureSaverNode>{
		@Getter
		private String featureName;
		public FeatureSaverModel(String name,String featureName){
			super(name);
			this.featureName=featureName;
		}
	}
	public static interface FeatureSaverPhysicalNode extends ClassPhysicalNode<FeatureSaverNode>{
		FeatureSaverModel load();
	}
	public static class FileFeatureSaverPhysicalNode extends FileClassPhysicalNode<FeatureSaverNode> implements FeatureSaverPhysicalNode{
		private static final String TEMPLATE_PATH="resources/graph/templates/feature_saver.txt";
		public FileFeatureSaverPhysicalNode(File file,String packageName){
			super(file,packageName);
		}
		@Override
		public void persist(NodeModel<FeatureSaverNode> model){
			if(getLocation().exists()) throw new IllegalStateException("Physical representation already exists: "+getLocation().getAbsolutePath());
			try{
				if(getLocation().getParentFile()!=null) getLocation().getParentFile().mkdirs();
				String className=getLocation().getName().replace(".java","");
				String s=getClassString(className,getPackageName());
				Files.writeString(getLocation().toPath(),s);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public FeatureSaverModel load(){
			if(!getLocation().exists()) throw new UncheckedIOException(new FileNotFoundException());
			try{
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				TypeDeclaration<?> featureConfig=cu.getTypes().getFirst().get();
				ClassOrInterfaceType saverClass=featureConfig.asClassOrInterfaceDeclaration().getExtendedTypes(0);
				ClassOrInterfaceType typeArgument=saverClass.getTypeArguments().get().getFirst().get().asClassOrInterfaceType();
				return new FeatureSaverModel(featureConfig.getName().asString(),typeArgument.getName().asString());
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public String getClassString(String className,String pkg){
			String base;
			try{
				base=new String(Root.getResourceAsStream(TEMPLATE_PATH).readAllBytes());
				return String.format(base,className,pkg);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
	}
	public FeatureSaverNode(ClassPhysicalNode<FeatureSaverNode> physicalNode){
		super(physicalNode);
	}
}
