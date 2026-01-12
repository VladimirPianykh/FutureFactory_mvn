package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.Getter;

/**
 * @author AI-generated
 */
public class RolesNode implements ProjectNode<RolesNode>{
	public static class RoleRepresentation{
		private String name;
		private Set<String> permissions;
		private Set<String> features;
		public RoleRepresentation(String name,Set<String> permissions,Set<String> features){
			this.name=name;
			this.permissions=permissions;
			this.features=features;
		}
		public Set<String> getFeatures(){
			return features;
		}
		public Set<String> getPermissions(){
			return permissions;
		}
		public String getName(){
			return name;
		}
	}
	public static interface RolesPhysicalNode extends PhysicalNode<RolesNode>{
		void addPermission(String roleName,String permission);
		void removePermission(String roleName,String permission);
		void addRole(String name);
		void removeRole(String name);
		@Override
		RolesModel load();
	}
	public static class FileRolesPhysicalNode implements RolesPhysicalNode{
		private final File file;
		private final PermissionsNode permissionsNode; // Dependency for loading
		public FileRolesPhysicalNode(File file,PermissionsNode permissionsNode){
			this.file=file;
			this.permissionsNode=permissionsNode;
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
		public RolesModel load(){
			ArrayList<RoleRepresentation> roles=new ArrayList<>();
			try{
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> roleEnum=findRoleEnum(cu);

				if(roleEnum.isPresent()){
					EnumDeclaration enumDecl=roleEnum.get();
					for(EnumConstantDeclaration constant:enumDecl.getEntries()){
						String name=constant.getNameAsString();
						Set<String> permissions;
						Set<String> features;

						if(constant.getArguments().size()>=2){
							if(constant.getArgument(0) instanceof LambdaExpr permissionsLambda)
								permissions=parsePermissionsFromLambda(permissionsLambda,permissionsNode);
							else permissions=new TreeSet<>();
							if(constant.getArgument(1) instanceof LambdaExpr featuresLambda)
								features=parseFeaturesFromLambda(featuresLambda);
							else features=new TreeSet<>();
						}else{
							permissions=new TreeSet<>();
							features=new TreeSet<>();
						}
						roles.add(new RoleRepresentation(name,permissions,features));
					}
				}
				return new RolesModel(roles);
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		@Override
		public void persist(NodeModel<RolesNode> model){
			// if(file.exists()){ throw new IllegalStateException("Physical representation already exists: "+file.getAbsolutePath()); }
			// try{
			// 	if(file.getParentFile()!=null) file.getParentFile().mkdirs();
			// 	// Basic implementation - generating empty enum or based on model logic
			// 	// For now, creating the skeleton to satisfy contract
			// 	String className=file.getName().replace(".java","");
			// 	StringBuilder sb=new StringBuilder();
			// 	sb.append("public enum ").append(className).append(" implements Role {\n");
			// 	// TODO: Serialize roles from model
			// 	sb.append(";\n");
			// 	sb.append("}");
			// 	Files.writeString(file.toPath(),sb.toString());
			// }catch(IOException ex){
			// 	throw new UncheckedIOException(ex);
			// }
			throw new UnsupportedOperationException("Physical representation for RolesNode cannot be created.");
		}

		public void addPermission(String roleName,String permission){
			try{
				while(!Files.isWritable(file.toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> roleEnum=findRoleEnum(cu);

				if(roleEnum.isPresent()){
					Optional<EnumConstantDeclaration> roleConstant=roleEnum.get().getEntries().stream().filter(constant->constant.getNameAsString().equals(roleName)).findFirst();
					if(roleConstant.isPresent()){
						EnumConstantDeclaration constant=roleConstant.get();
						if(constant.getArguments().size()>=1&&constant.getArguments().get(0) instanceof LambdaExpr){
							LambdaExpr permissionsLambda=(LambdaExpr)constant.getArguments().get(0);
							if(permissionsLambda.getBody() instanceof ExpressionStmt){
								ExpressionStmt stmt=(ExpressionStmt)permissionsLambda.getBody();
								if(stmt.getExpression() instanceof ArrayCreationExpr){
									ArrayCreationExpr array=(ArrayCreationExpr)stmt.getExpression();
									array.getInitializer().ifPresent(init->{
										init.getValues().add(new NameExpr(permission));
									});
								}
							}
						}
						Files.writeString(file.toPath(),cu.toString());
					}
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public void removePermission(String roleName,String permission){
			try{
				while(!Files.isWritable(file.toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> roleEnum=findRoleEnum(cu);

				if(roleEnum.isPresent()){
					Optional<EnumConstantDeclaration> roleConstant=roleEnum.get().getEntries().stream().filter(constant->constant.getNameAsString().equals(roleName)).findFirst();
					if(roleConstant.isPresent()){
						EnumConstantDeclaration constant=roleConstant.get();
						if(constant.getArguments().size()>=1&&constant.getArguments().get(0) instanceof LambdaExpr){
							LambdaExpr permissionsLambda=(LambdaExpr)constant.getArguments().get(0);
							if(permissionsLambda.getBody() instanceof ExpressionStmt){
								ExpressionStmt stmt=(ExpressionStmt)permissionsLambda.getBody();
								if(stmt.getExpression() instanceof ArrayCreationExpr){
									ArrayCreationExpr array=(ArrayCreationExpr)stmt.getExpression();
									array.getInitializer().ifPresent(init->{
										init.getValues().removeIf(element->element instanceof NameExpr&&((NameExpr)element).getNameAsString().equals(permission));
									});
									Files.writeString(file.toPath(),cu.toString());
								}else if(stmt.getExpression() instanceof MethodCallExpr){
									// Support for permissions = Permission.values()
									MethodCallExpr methodCall=(MethodCallExpr)stmt.getExpression();
									if(methodCall.getNameAsString().equals("values")&&methodCall.getScope().isPresent()&&methodCall.getScope().get() instanceof NameExpr&&((NameExpr)methodCall.getScope().get()).getNameAsString().contains("Permission")){
										// Replace Permission.values() with explicit array of all permissions minus the one to remove
										// Requires PermissionsNode to provide all known permissions
										List<String> allPermissions=permissionsNode!=null?permissionsNode.getPermissions():List.of();
										List<String> newPermissions=allPermissions.stream().filter(p->!p.equals(permission)).toList();
										if(!newPermissions.isEmpty()){
											ArrayCreationExpr newArray=new ArrayCreationExpr();
											newArray.setElementType(methodCall.getScope().get().toString());
											ArrayInitializerExpr initializer=new ArrayInitializerExpr();
											for(String permName:newPermissions){
												initializer.getValues().add(new NameExpr(permName));
											}
											newArray.setInitializer(initializer);
											stmt.setExpression(newArray);
										}else{
											// No permissions left, set to empty array
											ArrayCreationExpr emptyArray=new ArrayCreationExpr();
											emptyArray.setElementType(methodCall.getScope().get().toString());
											emptyArray.setInitializer(new ArrayInitializerExpr());
											stmt.setExpression(emptyArray);
										}
										Files.writeString(file.toPath(),cu.toString());
									}
								}
							}
						}
					}
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public void addRole(String name){
			try{
				while(!Files.isWritable(file.toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(file);
				Optional<EnumDeclaration> roleEnum=findRoleEnum(cu);
				if(roleEnum.isPresent()){
					EnumConstantDeclaration newConstant=new EnumConstantDeclaration(name);
					roleEnum.get().addEntry(newConstant);
					Files.writeString(file.toPath(),cu.toString());
				}
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}
		public void removeRole(String name){
			try{
				while(!Files.isWritable(file.toPath()))
					Thread.onSpinWait();
				CompilationUnit cu=StaticJavaParser.parse(file);
				findRoleEnum(cu).ifPresent(roleEnum->{
					roleEnum.getEntries().removeIf(constant->constant.getNameAsString().equals(name));
					try{
						Files.writeString(file.toPath(),cu.toString());
					}catch(IOException e){
						throw new UncheckedIOException(e);
					}
				});
			}catch(IOException ex){
				throw new UncheckedIOException(ex);
			}
		}

		private Optional<EnumDeclaration> findRoleEnum(CompilationUnit cu){
			return cu.findAll(EnumDeclaration.class).stream().filter(enumDecl->enumDecl.getImplementedTypes().stream().anyMatch(type->type instanceof ClassOrInterfaceType&&((ClassOrInterfaceType)type).getNameAsString().contains("Role"))).findFirst();
		}
		private Set<String> parsePermissionsFromLambda(LambdaExpr lambda,PermissionsNode p){
			Set<String> permissions=new TreeSet<>();
			if(lambda.getBody() instanceof ExpressionStmt stmt){
				if(stmt.getExpression() instanceof MethodCallExpr){
					MethodCallExpr methodCall=(MethodCallExpr)stmt.getExpression();
					if(methodCall.getNameAsString().equals("values")&&methodCall.getScope().isPresent()&&methodCall.getScope().get() instanceof NameExpr){
						NameExpr scope=(NameExpr)methodCall.getScope().get();
						if(scope.getNameAsString().contains("Permission")) return new TreeSet<>(p.getPermissions());
					}
				}
				if(stmt.getExpression() instanceof ArrayCreationExpr){
					ArrayCreationExpr array=(ArrayCreationExpr)stmt.getExpression();
					array.getInitializer().ifPresent(init->{
						for(var element:init.getValues()){
							if(element instanceof NameExpr){
								permissions.add(((NameExpr)element).getNameAsString());
							}
						}
					});
				}
			}
			return permissions;
		}
		private Set<String>parseFeaturesFromLambda(LambdaExpr lambda){
			return new TreeSet<>();
			// TODO: #6 parse features for RolesNode
		}
	}
	public static class RolesModel implements NodeModel<RolesNode>{
		@Getter
		private final List<RoleRepresentation> roles;
		public RolesModel(List<RoleRepresentation> roles){
			this.roles=roles;
		}
	}
	private final RolesPhysicalNode physicalNode;
	private final RolesModel model;
	public RolesNode(RolesPhysicalNode physicalNode){
		if(!physicalNode.exists()) throw new IllegalArgumentException("Physical representation does not exist");
		this.physicalNode=physicalNode;
		this.model=physicalNode.load();
	}
	public RolesNode(RolesPhysicalNode physicalNode,List<RoleRepresentation> roles){
		if(physicalNode.exists()) throw new IllegalArgumentException("Physical representation already exists");
		this.model=new RolesModel(roles);
		this.physicalNode=physicalNode;
		this.physicalNode.persist(model);
	}
	@Override
	public PhysicalNode<RolesNode> getPhysicalRepresentation(){
		return physicalNode;
	}
	@Override
	public NodeModel<RolesNode> getModel(){
		return model;
	}
	public void addPermission(String roleName,String permission){
		for(RoleRepresentation r:getRoles()){
			if(r.name.equals(roleName)){
				if(r.permissions.contains(permission)) throw new IllegalStateException(r.name+" already has permission "+permission+".");
				r.permissions.add(permission); // Model update
				physicalNode.addPermission(roleName,permission); // Physical update
				return;
			}
		}
		throw new IllegalArgumentException("There is no role "+roleName+".");
	}
	public void removePermission(String roleName,String permission){
		for(RoleRepresentation r:model.getRoles()){
			if(r.name.equals(roleName)){
				if(r.permissions.contains(permission)){
					r.permissions.remove(permission); // Model update
					physicalNode.removePermission(roleName,permission); // Physical update
					return;
				}else{
					throw new IllegalStateException(r.name+" does not have permission "+permission);
				}
			}
		}
		throw new IllegalArgumentException("There is no role "+roleName);
	}
	public RoleRepresentation addRole(String name,String...permissions){
		RoleRepresentation r=new RoleRepresentation(name,new TreeSet<>(Arrays.asList(permissions)),null);
		model.getRoles().add(r); // Model update
		physicalNode.addRole(name); // Physical update
		// Note: Original addRole didn't add permissions in file creation? 
		// "EnumConstantDeclaration newConstant=new EnumConstantDeclaration(name);" -> acts like simple name.
		// The permissions are passed to RoleRepresentation but not written to file?
		// Replicating original behavior.
		return r;
	}
	public void removeRole(String name){
		model.getRoles().removeIf(r->r.name.equals(name));
		physicalNode.removeRole(name);
	}
	public List<RoleRepresentation>getRoles(){
		return model.getRoles();
	}
}
