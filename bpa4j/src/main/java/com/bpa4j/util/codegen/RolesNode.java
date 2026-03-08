package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bpa4j.util.ParseUtils;
import com.bpa4j.util.ParseUtils.StandardSkipper;
import com.bpa4j.util.codegen.ProjectGraph.ProjectNode;

public class RolesNode extends ProjectNode{
	public static class RoleRepresentation{
		public String name;
		public Set<String>permissions;
		public Set<String>features;
		public RoleRepresentation(String name,Set<String>permissions,Set<String>features){
			this.name=name;
			this.permissions=permissions;
			this.features=features;
		}
	}
	public ArrayList<RoleRepresentation>roles=new ArrayList<>();
	public RolesNode(File file,PermissionsNode p){
		super(file);
		try{
			String roleClass=ParseUtils.findFirstBlock(
				Pattern.compile("public enum \\w+ implements.*?Role"),
				Files.readString(file.toPath()), 
				'{','}',
				StandardSkipper.SYNTAX
			);
			TreeMap<String,String>a=ParseUtils.findAllBlocks(
				Pattern.compile("(\\w+)"),
				roleClass.substring(0,roleClass.indexOf(';')),
				'(',')',
				StandardSkipper.SYNTAX
			);
			Pattern lambdaPattern=Pattern.compile("\\(\\)\\s*->\\s*");
			for(Entry<String,String>entry:a.entrySet()){ //for each role
				String name=entry.getKey();
				int d=ParseUtils.find(entry.getValue(),Pattern.compile(","),StandardSkipper.OUTERSCOPE).get().start();
				String permissions=entry.getValue().substring(0,d);
				Matcher m=lambdaPattern.matcher(permissions);
				if(m.find())permissions=permissions.substring(m.end());
				else permissions="";
				String featuresStr=entry.getValue().substring(d+1);
				m=lambdaPattern.matcher(featuresStr);
				if(m.find())featuresStr=featuresStr.substring(m.end());
				else featuresStr="";
				//TODO: parse features
				if(Pattern.compile("\\s*\\w+\\.values\\s*\\(\\).*",Pattern.DOTALL).matcher(permissions).matches()){
					roles.add(new RoleRepresentation(name,new TreeSet<>(p.permissions),null));
				}else{
					String s=ParseUtils.findFirstBlock(Pattern.compile("new Permission\\s*\\[\\]"),permissions,'{','}');
					roles.add(new RoleRepresentation(name,s==null?null:s.isBlank()?new TreeSet<>():new TreeSet<>(Arrays.asList(s.replaceAll("\\s+","").split(","))),null));
				}
			}
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	public void addPermission(String roleName,String permission){
		for(RoleRepresentation r:roles)if(r.name.equals(roleName))try{
			if(r.permissions.contains(permission))throw new IllegalStateException(r.name+" already has permission "+permission+".");
			r.permissions.add(permission);
			while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
			StringBuilder s=new StringBuilder(Files.readString(location.toPath()));
			int index=ParseUtils.find(
				s,
				Pattern.compile(Pattern.quote(roleName)+"\\s*\\("),
				StandardSkipper.SYNTAX,
				ParseUtils.find(
					s,
					ParseUtils.createSubClassPattern("Role"),
					StandardSkipper.SYNTAX
				).get().start()
			).get().start();
			Matcher m=Pattern.compile("\\(\\)\\s*->\\s*new Permission\\s*\\[\\]\\s*\\{(\\s*)(.)").matcher(s);
			m.find(index);
			s.insert(m.end(1),permission+(m.group(2).equals("}")?"":",")+m.group(1));
			Files.writeString(location.toPath(),s);
			return;
		}catch(IOException ex){throw new UncheckedIOException(ex);}
		throw new IllegalArgumentException("There is no role "+roleName+".");
	}
	public void removePermission(String roleName,String permission){
		for(RoleRepresentation r:roles)if(r.name.equals(roleName))try{
			if(r.permissions.contains(permission)){
				r.permissions.remove(permission);
				while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
				String s=Files.readString(location.toPath());
				int index=ParseUtils.find(
					s,
					Pattern.compile(Pattern.quote(roleName)+"\\s*\\("),
					StandardSkipper.SYNTAX,
					ParseUtils.find(
						s,
						ParseUtils.createSubClassPattern("Role"),
						StandardSkipper.SYNTAX
					).get().start()
				).get().start();
				Matcher m=Pattern.compile("(\\s*,?\\s*)"+Pattern.quote(permission)+"(\\s*,?\\s*)").matcher(s);
				m.find(index);
				Files.writeString(location.toPath(),m.replaceFirst(result->{
					if(result.start()<index)return result.group();
					return result.group(1).isBlank()?result.group(1):result.group(2);
				}));
			}else throw new IllegalStateException(r.name+" does not have permission "+permission);
			return;
		}catch(IOException ex){throw new UncheckedIOException(ex);}
		throw new IllegalArgumentException("There is no role "+roleName);
	}
	public RoleRepresentation addRole(String name,String...permissions){
		try{
			RoleRepresentation r=new RoleRepresentation(name,new TreeSet<>(Arrays.asList(permissions)),null);
			while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
			StringBuilder s=new StringBuilder(Files.readString(location.toPath()));
			MatchResult m=ParseUtils.find(s,Pattern.compile("public enum \\w+ implements.*?Role\\s*\\{(\\s*)"),StandardSkipper.SYNTAX).get();
			s.insert(
				m.start(1),
				String.format("""
						%s(
							()->new Permission[]{%s},
							()->new Feature[]{}
						),
				""",m.group(1)+name,Stream.of(permissions).collect(Collectors.joining(",")))
			);
			//TODO: add role
			Files.writeString(location.toPath(),s);
			roles.add(r);
			return r;
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	public void removeRole(String name){
		try{
			while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
			StringBuilder s=new StringBuilder(Files.readString(location.toPath()));
			MatchResult m=ParseUtils.find(
				s,
				Pattern.compile(name+"\\s*\\("),
				StandardSkipper.SYNTAX,
				ParseUtils.find(s,Pattern.compile("public enum \\w+ implements.*?Role\\s*\\{(\\s*)"),StandardSkipper.SYNTAX).get().end()
			).get();
			s=new StringBuilder(ParseUtils.replaceAll(
				s,
				Pattern.compile(".+",Pattern.DOTALL),
				"",
				new ParseUtils.InnerScopeSkipper('(',')'),
				m.end()
			));
			StringBuilder result=new StringBuilder();
			Matcher matcher=Pattern.compile(".*?,\\s*").matcher(s).region(m.start(),s.length());
			matcher.find();
			matcher.appendReplacement(result,"").appendTail(result);
			Files.writeString(location.toPath(),result);
			roles.removeIf(r->r.name.equals(name));
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
}