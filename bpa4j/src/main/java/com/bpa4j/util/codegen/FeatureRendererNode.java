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

public class FeatureRendererNode extends ClassNode<FeatureRendererNode>implements InternalNode<FeatureRendererNode>{
	public static class FeatureRendererModel extends ClassModel<FeatureRendererNode>{
		@Getter
		private String featureName;
		public FeatureRendererModel(String name,String featureName){
			super(name);
			this.featureName=featureName;
		}
	}
	public static interface FeatureRendererPhysicalNode extends ClassPhysicalNode<FeatureRendererNode>{
		FeatureRendererModel load();
	}
	public static class FileFeatureRendererPhysicalNode extends FileClassPhysicalNode<FeatureRendererNode>implements FeatureRendererPhysicalNode{
		private static final String TEMPLATE_PATH="resources/graph/templates/feature_renderer.txt";
		public FileFeatureRendererPhysicalNode(File file,String packageName){
			super(file,packageName);
		}
		@Override
		public void persist(NodeModel<FeatureRendererNode> model){
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
		public FeatureRendererModel load(){
			if(!getLocation().exists())throw new UncheckedIOException(new FileNotFoundException());
			try{
				CompilationUnit cu=StaticJavaParser.parse(getLocation());

				TypeDeclaration<?> featureConfig=cu.getTypes().getFirst().get();
				ClassOrInterfaceType rendererClass=featureConfig.asClassOrInterfaceDeclaration().getExtendedTypes(0);
				ClassOrInterfaceType typeArgument=rendererClass.getTypeArguments().get().getFirst().get().asClassOrInterfaceType();

				return new FeatureRendererModel(featureConfig.getName().asString(),typeArgument.getName().asString());
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
	public FeatureRendererNode(ClassPhysicalNode<FeatureRendererNode> physicalNode){
		super(physicalNode);
	}
}
