package com.bpa4j.util.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.regex.Pattern;
import com.bpa4j.core.Root;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A node representing {@link com.bpa4j.feature.FeatureTransmissionContract Feature interface} implementation.
 */
public class FeatureNode extends ClassNode<FeatureNode>{
	public static interface FeaturePhysicalNode extends ClassPhysicalNode<FeatureNode>{}
	public static class FileFeaturePhysicalNode extends FileClassPhysicalNode<FeatureNode> implements FeaturePhysicalNode{
		private static final String TEMPLATE_PATH="resources/graph/templates/feature_impl.txt";
		/**
		 * Constructs a new feature physical node with the given file and package.
		 */
		public FileFeaturePhysicalNode(File file,String packageName){
			super(file,packageName);
		}
		/**
		 * Constructs feature physical node with newly generated location.
		 */
		public FileFeaturePhysicalNode(String className,String basePackage,File projectRoot){
			super(computeFileLocation(className,basePackage,projectRoot),computePackage(basePackage));
			assert !getLocation().exists();
		}
		private static String computePackage(String basePackage){
			return basePackage+".features_impl";
		}
		private static File computeFileLocation(String className,String basePackage,File projectRoot){
			String packagePath=computePackage(basePackage).replace('.','/');
			File file=new File(projectRoot,packagePath+"/"+className+".java");
			Pattern reg=Pattern.compile("\\d$");
			while(file.exists())file=new File(reg.matcher(file.getName()).replaceFirst(r->String.valueOf(Integer.parseInt(r.group(0))+1)));
			return file;
		}
		@Override
		public void persist(NodeModel<FeatureNode> model){
			if(getLocation().exists()) throw new IllegalStateException("Physical representation already exists: "+getLocation().getAbsolutePath());
			try{
				if(getLocation().getParentFile()!=null) getLocation().getParentFile().mkdirs();
				String className=getLocation().getName().replace(".java","");
				String s="public class "+className+" {}";
				Files.writeString(getLocation().toPath(),s);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public FeatureModel load(){
			if(!getLocation().exists())throw new UncheckedIOException(new FileNotFoundException());
			String name=getLocation().getName();
			assert name.endsWith(".java");
			name=name.substring(0,name.length()-5);
			return new FeatureModel(name);
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
	public static class FeatureModel extends ClassModel<FeatureNode>{
		public FeatureModel(String name){
			super(name);
			//No feature implementation data is kept for now.
		}
	}
	/**
	 * Reading constructor.
	 */
	public FeatureNode(FeaturePhysicalNode physicalNode){
		super(physicalNode);
		if(!physicalNode.exists())throw new IllegalArgumentException("Empty physical node cannot be loaded.");
	}
	/**
	 * Writing constructor.
	 */
	public FeatureNode(FeaturePhysicalNode physicalNode,String name){
		super(physicalNode);
		if(physicalNode.exists())throw new IllegalArgumentException("Physical node"+physicalNode+" is not empty.");
		model=new FeatureModel(name);
		physicalNode.persist(model);
	}
}
