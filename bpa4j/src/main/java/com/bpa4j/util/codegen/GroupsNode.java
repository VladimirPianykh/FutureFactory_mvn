package com.bpa4j.util.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bpa4j.util.codegen.ProjectGraph.ProjectNode;

public class GroupsNode extends ProjectNode{
	public GroupsNode(File file){
		super(file);
		try{
			StringBuilder s=new StringBuilder(Files.readString(file.toPath()));
			Matcher m=Pattern.compile("EditableGroup \\w+\\s*=\\s*new EditableGroup\\(.*?(.*?,.*?,.*?)?\\)").matcher(s);
			m.find();
			s.insert(m.end(),"");
			//TODO: complete GroupsNode
		}catch(IOException ex){throw new UncheckedIOException(ex);}
	}
}