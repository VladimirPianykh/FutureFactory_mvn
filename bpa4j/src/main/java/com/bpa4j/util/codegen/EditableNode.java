package com.bpa4j.util.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bpa4j.core.Editable;
import com.bpa4j.editor.EditorEntry;
import com.bpa4j.Wrapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("PMD.ExceptionAsFlowControl")
/**
 * Most methods are AI-generated.
 */
public class EditableNode extends ClassNode<EditableNode>{
	public static class Property{
		/**
		 * Property type value object.
		 */
		public static class PropertyType{
			public static final PropertyType STRING=new PropertyType("String");
			public static final PropertyType INT=new PropertyType("int");
			public static final PropertyType DOUBLE=new PropertyType("double");
			public static final PropertyType BOOL=new PropertyType("boolean");
			public static final PropertyType DATE=new PropertyType("LocalDate");
			public static final PropertyType DATETIME=new PropertyType("LocalDateTime");

			private final String name;
			public PropertyType(String typeName){
				this.name=typeName;
			}
			public String toString(){
				return name;
			}
			public String name(){
				return name;
			}
			public static PropertyType[] values(){
				return new PropertyType[]{STRING,INT,DOUBLE,BOOL,DATE,DATETIME};
			}
			public boolean equals(Object obj){
				return (obj instanceof PropertyType t)&&t.name==name;
			}
		}
		@Getter
		private PropertyType type;
		@Getter
		private String name;
		public Property(String name,PropertyType type){
			this.name=name.trim();
			this.type=type;
		}
		public String getCode(String prompt){
			return type==null?"\t//FIXME: add property \""+name+"\"\n":String.format("""
						@EditorEntry(translation="%s")
						public %s %s;
					""",name,type.toString(),prompt);
		}
		public String toString(){
			return name+" ("+(type==null?"???":type.toString())+")";
		}
		protected void setType(PropertyType type){
			this.type=type;
		}
		protected void setName(String name){
			this.name=name;
		}
	}
	public static class FileEditablePhysicalNode extends FileClassPhysicalNode<EditableNode>{
		/**
		 * Constructs a new editable physical node with the given file and package.
		 */
		public FileEditablePhysicalNode(File file,String packageName){
			super(file,packageName);
		}
		/**
		 * Finds the package and file itself.
		 */
		public FileEditablePhysicalNode(String className,String objectName,String basePackage,File projectRoot,Property...properties){
			super(computeFileLocation(className,basePackage,projectRoot),computePackage(basePackage));
			// Throw if file already exists. FIXME Can be enhanced with incremental naming.
			if(getLocation().exists()) throw new IllegalStateException("File already exists: "+getLocation().getAbsolutePath());
		}
		private static String computePackage(String basePackage){
			return basePackage+".editables.registered";
		}
		private static File computeFileLocation(String className,String basePackage,File projectRoot){
			String packagePath=computePackage(basePackage).replace('.','/');
			return new File(projectRoot,"src/main/java/"+packagePath+"/"+className+".java");
		}
		@Override
		public ClassModel<EditableNode> load(){
			try{
				String objectName=null;
				List<Property> properties=new ArrayList<>();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<ClassOrInterfaceDeclaration> clazz=cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(c->c.getExtendedTypes().stream().anyMatch(extType->extType instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)extType).getNameAsString().equals("Editable"))).findFirst();

				if(clazz.isPresent()){
					String className=clazz.get().getNameAsString();
					// Извлечь objectName из конструктора
					Optional<ConstructorDeclaration> constructor=cu.findAll(ConstructorDeclaration.class).stream().filter(c->c.getNameAsString().equals(className)).findFirst();
					if(constructor.isPresent()){
						ConstructorDeclaration constr=constructor.get();
						Optional<ExplicitConstructorInvocationStmt> superCall=constr.getBody().getStatements().stream().filter(stmt->stmt.isExplicitConstructorInvocationStmt()).map(stmt->stmt.asExplicitConstructorInvocationStmt()).findFirst();

						if(superCall.isPresent()&&superCall.get().getArguments().size()>0){
							Expression argExpr=superCall.get().getArguments().get(0);
							if(argExpr.isStringLiteralExpr()){
								StringLiteralExpr arg=argExpr.asStringLiteralExpr();
								String superArg=arg.asString();
								if(superArg.toLowerCase().startsWith("нов")){
									superArg=superArg.substring(superArg.indexOf(' '));
								}
								objectName=superArg.replaceAll("[!@#$%&*]","").trim();
								if(objectName.isEmpty()) objectName=null;
							}
						}
					}
					// Извлечь поля с аннотацией @EditorEntry
					cu.findAll(FieldDeclaration.class).stream().filter(field->field.getAnnotationByName("EditorEntry").isPresent()).forEach(field->{
						Optional<StringLiteralExpr> translation=field.getAnnotationByName("EditorEntry").flatMap(ann->ann.asNormalAnnotationExpr().getPairs().stream().filter(pair->pair.getNameAsString().equals("translation")).findFirst().map(pair->pair.getValue().asStringLiteralExpr()));

						if(translation.isPresent()){
							String fieldName=translation.get().asString();
							String fieldType=field.getElementType().toString();

							Property.PropertyType propType=switch(fieldType){
								case "LocalDate" -> Property.PropertyType.DATE;
								case "LocalDateTime" -> Property.PropertyType.DATETIME;
								case "String" -> Property.PropertyType.STRING;
								case "int" -> Property.PropertyType.INT;
								case "double" -> Property.PropertyType.DOUBLE;
								case "boolean" -> Property.PropertyType.BOOL;
								default -> null;
							};

							properties.add(new Property(fieldName,propType));
						}
					});
					// Найти fixme-комментарии для свойств
					cu.findAll(LineComment.class).stream().filter(comment->comment.getContent().contains("FIXME: add property")).forEach(comment->{
						Matcher m=Pattern.compile("FIXME:\\s*add property\\s*\"(.*?)\"").matcher(comment.getContent());
						if(m.find()){
							properties.add(new Property(m.group(1),null));
						}
					});
					return new EditableModel(className,objectName,properties);
				}else throw new IOException();
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public void persist(NodeModel<EditableNode> model){
			// Per contract: throws IllegalStateException if already associated with physical representation
			if(getLocation().exists()) throw new IllegalStateException("Physical representation already exists: "+getLocation().getAbsolutePath());
			EditableModel m=(EditableModel)model;
			try{
				if(getLocation().getParentFile()!=null) getLocation().getParentFile().mkdirs();
				Wrapper<Integer> index=new Wrapper<>(0);
				String s=String.format("""
						package %s;

						import %s;
						import %s;

						public class %s extends Editable{
						"""+m.getProperties().stream().map(p->p.getCode("var"+(++index.var))).collect(Collectors.joining("\n"))+"""
							public %s(){
								super("Нов %s");
							}
						}
						""",getPackageName(),Editable.class.getName(),EditorEntry.class.getName(),m.getName(),m.getName(),m.getObjectName()==null?"":m.getObjectName());
				Files.writeString(getLocation().toPath(),s);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		// Specialized methods for fine-grained updates
		public void changePropertyType(Property p,Property.PropertyType type){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());

				if(p.getType()==null){
					// fixme case
					Optional<LineComment> todoComment=cu.findAll(LineComment.class).stream().filter(comment->comment.getContent().contains("FIXME: add property \""+p.getName()+"\"")).findFirst();
					if(todoComment.isPresent()){
						todoComment.get().remove();
						Optional<ClassOrInterfaceDeclaration> clazz=cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(c->c.getExtendedTypes().stream().anyMatch(extType->extType instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)extType).getNameAsString().equals("Editable"))).findFirst();
						if(clazz.isPresent()) clazz.get().addField(type.toString(),p.getName(),Keyword.PUBLIC);
					}
				}else{
					// Existing case
					Optional<FieldDeclaration> field=findFieldByTranslation(cu,p.getName());
					if(field.isPresent()){
						field.get().setAllTypes(StaticJavaParser.parseType(type.toString()));
					}
				}
				Files.writeString(getLocation().toPath(),cu.toString());
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		public void changePropertyName(String oldName,String newName){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<FieldDeclaration> field=findFieldByTranslation(cu,oldName);
				if(field.isPresent()){
					field.get().getAnnotationByName("EditorEntry").flatMap(ann->ann.asNormalAnnotationExpr().getPairs().stream().filter(pair->pair.getNameAsString().equals("translation")).findFirst()).ifPresent(pair->pair.setValue(new StringLiteralExpr(newName)));
					Files.writeString(getLocation().toPath(),cu.toString());
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		public void addProperty(Property property,String varName){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<ClassOrInterfaceDeclaration> clazz=cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(c->c.getExtendedTypes().stream().anyMatch(extType->extType instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)extType).getNameAsString().equals("Editable"))).findFirst();
				if(clazz.isPresent()){
					clazz.get().addField(property.getType().toString(),varName,Keyword.PUBLIC); // Simplified logic
					Files.writeString(getLocation().toPath(),cu.toString(),StandardOpenOption.CREATE);
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		public void addProperties(List<Property> properties){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<ClassOrInterfaceDeclaration> clazz=cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(c->c.getExtendedTypes().stream().anyMatch(extType->extType instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)extType).getNameAsString().equals("Editable"))).findFirst();
				if(clazz.isPresent()){
					for(int i=0;i<properties.size();++i){
						Property prop=properties.get(i);
						FieldDeclaration newField=new FieldDeclaration();
						newField.setAllTypes(StaticJavaParser.parseType(prop.getType().toString()));
						newField.addVariable(new VariableDeclarator(StaticJavaParser.parseType(prop.getType().toString()),"iVar"+(i+1)));
						clazz.get().addMember(newField);
					}
					Files.writeString(getLocation().toPath(),cu.toString(),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		public void removeProperty(String propertyName){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<FieldDeclaration> field=findFieldByTranslation(cu,propertyName);
				if(field.isPresent()){
					field.get().remove();
					Files.writeString(getLocation().toPath(),cu.toString());
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		public void changeObjectName(String objectName,String className){
			try{
				while(!Files.isWritable(getLocation().toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(getLocation());
				Optional<ConstructorDeclaration> constructor=cu.findAll(ConstructorDeclaration.class).stream().filter(c->c.getNameAsString().equals(className)).findFirst();
				if(constructor.isPresent()){
					ConstructorDeclaration constr=constructor.get();
					Optional<MethodCallExpr> superCall=constr.getBody().getStatements().stream().filter(stmt->stmt.isExpressionStmt()&&stmt.asExpressionStmt().getExpression() instanceof MethodCallExpr).map(stmt->stmt.asExpressionStmt().getExpression().asMethodCallExpr()).filter(call->call.getNameAsString().equals("super")).findFirst();
					if(superCall.isPresent()&&superCall.get().getArguments().size()>0){
						superCall.get().getArguments().set(0,new StringLiteralExpr(objectName==null?"":objectName));
					}
				}
				Files.writeString(getLocation().toPath(),cu.toString());
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		private Optional<FieldDeclaration> findFieldByTranslation(CompilationUnit cu,String name){
			return cu.findAll(FieldDeclaration.class).stream().filter(f->f.getAnnotationByName("EditorEntry").isPresent()).filter(f->{
				Optional<StringLiteralExpr> translation=f.getAnnotationByName("EditorEntry").flatMap(ann->ann.asNormalAnnotationExpr().getPairs().stream().filter(pair->pair.getNameAsString().equals("translation")).findFirst().map(pair->pair.getValue().asStringLiteralExpr()));
				return translation.isPresent()&&translation.get().asString().equals(name);
			}).findFirst();
		}
	}
	public static class EditableModel extends ClassModel<EditableNode>{
		@Getter
		@Setter
		private String objectName;
		@Getter
		private final List<Property> properties;
		public EditableModel(String name,String objectName,List<Property> properties){
			super(name);
			this.objectName=objectName;
			this.properties=properties==null?new ArrayList<>():new ArrayList<>(properties);
		}
	}
	/**
	 * Reading constructor.
	 * @param physicalNode
	 */
	public EditableNode(ClassPhysicalNode<EditableNode> physicalNode){
		super(physicalNode);
		if(!physicalNode.exists()) throw new IllegalArgumentException("The node "+physicalNode+" is not empty.");
	}

	/**
	 * Writing constructor.
	 */
	public EditableNode(ClassPhysicalNode<EditableNode> physicalNode,String className,String objectName,Property...properties){
		super(physicalNode);
		if(physicalNode.exists()) throw new IllegalArgumentException("Physical representation already exists");
		this.model=new EditableModel(className,objectName,List.of(properties));
		this.physicalNode.persist(model);
	}
	public static File findFile(String name,File parent) throws IOException{
		Wrapper<File> w=new Wrapper<File>(null);
		Files.walkFileTree(parent.toPath(),new SimpleFileVisitor<Path>(){
			public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException{
				if(file.toString().equals(name+".java")){
					w.var=file.toFile();
					return FileVisitResult.TERMINATE;
				}
				return FileVisitResult.CONTINUE;
			}
		});
		if(w.var==null) throw new FileNotFoundException();
		return w.var;
	}

	// Delegation Methods
	public void addProperty(Property property,String varName){
		((EditableModel)model).getProperties().add(property);
		((FileEditablePhysicalNode)physicalNode).addProperty(property,varName);
	}
	public void addProperties(Property...properties){
		((EditableModel)model).getProperties().addAll(Arrays.asList(properties));
		((FileEditablePhysicalNode)physicalNode).addProperties(Arrays.asList(properties));
	}
	public void removeProperty(Property property){
		((EditableModel)model).getProperties().remove(property);
		((FileEditablePhysicalNode)physicalNode).removeProperty(property.getName());
	}
	public void changePropertyName(Property property,String newName){
		String oldName=property.getName();
		property.setName(newName); // Update model (property instance is in list)
		((FileEditablePhysicalNode)physicalNode).changePropertyName(oldName,newName);
	}
	public void changePropertyType(Property property,Property.PropertyType newType){
		property.setType(newType);
		((FileEditablePhysicalNode)physicalNode).changePropertyType(property,newType);
	}
	public void changeObjectName(String objectName){
		((EditableModel)model).setObjectName(objectName);
		((FileEditablePhysicalNode)physicalNode).changeObjectName(objectName,((EditableModel)model).getName());
	}

	public List<Property> getProperties(){
		return Collections.unmodifiableList(((EditableModel)model).getProperties());
	}
	public String getObjectName(){
		return ((EditableModel)model).getObjectName();
	}
}
