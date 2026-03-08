package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bpa4j.util.ParseUtils;
import com.bpa4j.util.ParseUtils.StandardSkipper;
import com.bpa4j.util.codegen.ProjectGraph.ProjectNode;

public class PermissionsNode extends ProjectNode{
	public ArrayList<String>permissions;
	public PermissionsNode(File file){
		super(file);
		try{
			String s=ParseUtils.findFirstBlock(Pattern.compile("public enum \\w+ implements.*?Permission"),Files.readString(file.toPath()),'{','}',StandardSkipper.SYNTAX);
			String a=s
				.substring(0,s.indexOf(';'))
				.replaceAll("\\s+","");
			permissions=new ArrayList<>(Arrays.asList(a.isBlank()?new String[0]:a.split(",")));
			permissions.sort((a1,a2)->-1);
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	public void addPermission(String permission){
		try{
			if(permissions.contains(permission))throw new IllegalStateException(permission+" already exists.");
			while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
			StringBuilder s=new StringBuilder(Files.readString(location.toPath()));
			MatchResult r=ParseUtils.find(
				s,
				Pattern.compile("public enum \\w+ implements.*?Permission\\s*\\{(\\s*)"),
				StandardSkipper.SYNTAX
			).get();
			s.insert(r.start(1),r.group(1)+permission+",");
			Files.writeString(location.toPath(),s);
			permissions.add(permission);
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
	public void removePermission(String permission){
		try{
			if(!permissions.contains(permission))throw new IllegalStateException(permission+" does not exist.");
			while(!Files.isWritable(location.toPath()))Thread.onSpinWait();
			StringBuilder s=new StringBuilder(Files.readString(location.toPath()));
			StringBuilder result=new StringBuilder();
			Matcher m=Pattern.compile(Pattern.quote(permission)+",\\s*").matcher(s);
			m.find(ParseUtils.find(
				s,
				Pattern.compile("public enum \\w+ implements.*?Permission\\s*\\{"),
				StandardSkipper.SYNTAX
			).get().end());
			m.appendReplacement(result,"").appendTail(result);
			Files.writeString(location.toPath(),result);
			permissions.remove(permission);
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
}