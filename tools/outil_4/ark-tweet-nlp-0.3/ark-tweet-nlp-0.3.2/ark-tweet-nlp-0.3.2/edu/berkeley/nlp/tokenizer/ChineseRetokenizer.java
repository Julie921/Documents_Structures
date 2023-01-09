package edu.berkeley.nlp.tokenizer;

import java.util.LinkedList;
import java.util.List;

public class ChineseRetokenizer implements LineTokenizer {
	public List<String> tokenizeLine(String line) {
		String replaced = replaceChars(line);
		String[] tokens = replaced.split(" ");
		boolean rightDoubleQuote = false;
		boolean rightSingleQuote = false;
		LinkedList<String> newTokens = new LinkedList<String>();
		for (int i = 0; i<tokens.length; i++) 
		{
			String tok = tokens[i];
			if (tok.equals("\"")) {
				newTokens.add(rightDoubleQuote ? "\u201d" : "\u201c");
				rightDoubleQuote = !rightDoubleQuote;
			}
			else if (tok.equals("'")) {
				newTokens.add(rightSingleQuote ? "\u2019" : "\u2018");
				rightSingleQuote = !rightSingleQuote;
			} else if (tok.equals(".")) {
				if (i == tokens.length - 1) {
					newTokens.add("\u3002");
				} else {
					newTokens.add("\uff0c");
				}
			}
			else {
				newTokens.add(tok);
			}
		}
		
		return newTokens;
	}

	private String replaceChars(String line) {
		String s = line;
		s = s.replace('(', '\uff08');
		s = s.replace(')', '\uff09');
		s = s.replace('{', '\u3008');
		s = s.replace('}', '\u3009');
		s = s.replace(',', '\u3001');
		s = s.replace('-', '\u2014');
		s = s.replace('?', '\uff1f');
		s = s.replace('!', '\uff01');
		s = s.replace(':', '\uff1a');
		s = s.replace(';', '\uff1b');
		return s;
	}

}
