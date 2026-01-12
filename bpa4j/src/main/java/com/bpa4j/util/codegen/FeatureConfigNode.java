package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.regex.Pattern;
import com.bpa4j.core.Root;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.Getter;

/**
 * A node representing {@code Feature} registration and/or configuration.
 *
 * @author AI-generated
 */
public class FeatureConfigNode extends ClassNode<FeatureConfigNode>{
	public static interface FeatureConfigPhysicalNode extends ClassPhysicalNode<FeatureConfigNode>{
		@Override
		FeatureConfigModel load();
	}
	public static class FileFeatureConfigPhysicalNode extends FileClassPhysicalNode<FeatureConfigNode> implements FeatureConfigPhysicalNode{
		private static final String TEMPLATE_PATH="resources/graph/templates/feature_manager.txt";
		public FileFeatureConfigPhysicalNode(File file,String packageName){
			super(file,packageName);
		}
		public FileFeatureConfigPhysicalNode(String className,String basePackage,File projectRoot){
			super(computeFileLocation(className,basePackage,projectRoot),computePackage(basePackage));
			assert !getLocation().exists();
		}
		private static String computePackage(String basePackage){
			return basePackage+".features";
		}
		private static File computeFileLocation(String className,String basePackage,File projectRoot){
			String packagePath=computePackage(basePackage).replace('.','/');
			File file=new File(projectRoot,packagePath+"/"+className+".java");
			Pattern reg=Pattern.compile("\\d$");
			while(file.exists())
				file=new File(reg.matcher(file.getName()).replaceFirst(r->String.valueOf(Integer.parseInt(r.group(0))+1)));
			return file;
		}
		@Override
		public void persist(NodeModel<FeatureConfigNode> model){
			if(getLocation().exists()) throw new IllegalStateException("Physical representation already exists: "+getLocation().getAbsolutePath());
			try{
				if(getLocation().getParentFile()!=null) getLocation().getParentFile().mkdirs();
				assert getLocation().getName().endsWith(".java");
				String className=getLocation().getName().substring(0,getLocation().getName().length()-5);
				String s=getClassString(className,getPackageName());
				Files.writeString(getLocation().toPath(),s);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public FeatureConfigModel load(){
			try{
				CompilationUnit cu=StaticJavaParser.parse(getLocation());

				TypeDeclaration<?> featureConfig=cu.getTypes().getFirst().get();
				ClassOrInterfaceType managerClass=featureConfig.asClassOrInterfaceDeclaration().getExtendedTypes(0);
				ClassOrInterfaceType typeArgument=managerClass.getTypeArguments().get().getFirst().get().asClassOrInterfaceType();

				return new FeatureConfigModel(featureConfig.getName().asString(),typeArgument.getName().asString());
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public String getClassString(String className,String pkg){
			try{
				String base=new String(Root.getResourceAsStream(TEMPLATE_PATH).readAllBytes());
				return String.format(base,className,pkg);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
	}
	public static class FeatureConfigModel extends ClassModel<FeatureConfigNode>{
		@Getter
		private final String featureName;
		public FeatureConfigModel(String name,String featureName){
			super(name);
			this.featureName=featureName;
		}
	}
	/**
	 * Reading constructor.
	 */
	public FeatureConfigNode(FeatureConfigPhysicalNode physicalNode){
		super(physicalNode);
		if(!physicalNode.exists())throw new IllegalArgumentException("Empty physical node cannot be loaded.");
	}
	/**
	 * Writing constructor.
	 * @param physicalNode - storage
	 * @param featureName - name of the feature implemeneted
	 */
	public FeatureConfigNode(FeatureConfigPhysicalNode physicalNode,String name,String featureName,String packageName){
		super(physicalNode);
		if(physicalNode.exists())throw new IllegalArgumentException("The physical node must be empty.");
		model=new FeatureConfigModel(name,featureName);
		physicalNode.persist(model);
	}
	public String getFeatureName(){
		return ((FeatureConfigModel)getModel()).getFeatureName();
	}
}
