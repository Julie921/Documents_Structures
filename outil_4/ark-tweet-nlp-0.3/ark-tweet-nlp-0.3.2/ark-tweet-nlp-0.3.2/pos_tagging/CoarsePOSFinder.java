package pos_tagging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CoarsePOSFinder {
	
	HashSet<String> noun = new HashSet<String>();
	HashSet<String> nounNonPronoun = new HashSet<String>();
	HashSet<String> pronoun = new HashSet<String>();
	HashSet<String> adjective = new HashSet<String>();
	HashSet<String> verb = new HashSet<String>();
	HashSet<String> adverb = new HashSet<String>();
	HashSet<String> conjunction = new HashSet<String>();
	HashSet<String> punctuation = new HashSet<String>();
	HashSet<String> determiner = new HashSet<String>();
	HashSet<String> infinitive = new HashSet<String>();
	HashSet<String> preposition = new HashSet<String>();
	HashSet<String> possessive = new HashSet<String>();
	HashSet<String> symbol = new HashSet<String>();
	HashSet<String> interjection = new HashSet<String>();
	HashSet<String> foreign = new HashSet<String>();
	HashSet<String>[] coarsePOS;
	String[] coarsePOSNames;
	
	public CoarsePOSFinder() {
		noun.add("NNP");
		noun.add("NNS");
		noun.add("NN");
		noun.add("NNPS");
		noun.add("PRP");
		noun.add("WP");
		noun.add("EX");
		
		nounNonPronoun.add("NNP");
		nounNonPronoun.add("NNS");
		nounNonPronoun.add("NN");
		nounNonPronoun.add("NNPS");
		
		pronoun.add("PRP");
		pronoun.add("WP");
		pronoun.add("EX");
		
		adjective.add("JJ");
		adjective.add("JJS");
		adjective.add("JJR");
		adjective.add("CD");
		
		verb.add("VB");
		verb.add("VBZ");
		verb.add("VBG");
		verb.add("VBD");
		verb.add("VBN");
		verb.add("VBP");
		verb.add("MD");
		
		adverb.add("RB");
		adverb.add("RBR");
		adverb.add("RP");
		adverb.add("WRB");
		adverb.add("RBS");

		conjunction.add("CC");
		conjunction.add("IN");
		
		punctuation.add(",");
		punctuation.add(".");
		punctuation.add("``");
		punctuation.add("''");
		punctuation.add(":");
		punctuation.add("$");
		punctuation.add("#");
		punctuation.add("-RRB-");
		punctuation.add("-LRB-");
		
		determiner.add("DT");
		determiner.add("PDT");
//		determiner.add("PRP");
		determiner.add("WP$");
		determiner.add("PRP$");
		determiner.add("WDT");
		
//		infinitive.add("TO");

		preposition.add("TO");
		preposition.add("IN");
		
		possessive.add("POS");

		interjection.add("UH");
		
		foreign.add("FW");
		
		symbol.add("SYM");
		symbol.add("LS");
		
		coarsePOS = new HashSet[4];
		coarsePOS[0] = nounNonPronoun;
		coarsePOS[1] = verb;
		coarsePOS[2] = adjective;
		coarsePOS[3] = adverb;
		coarsePOSNames = new String[4];
		coarsePOSNames[0] = "nounNonPronoun";
		coarsePOSNames[1] = "verb";
		coarsePOSNames[2] = "adjective";
		coarsePOSNames[3] = "adverb";
		
//		coarsePOS = new HashSet[15];
//		coarsePOS[0] = noun;
//		coarsePOS[1] = nounNonPronoun;
//		coarsePOS[2] = pronoun;
//		coarsePOS[3] = adjective;
//		coarsePOS[4] = verb;
//		coarsePOS[5] = adverb;
//		coarsePOS[6] = conjunction;
//		coarsePOS[7] = punctuation;
//		coarsePOS[8] = determiner;
//		coarsePOS[9] = infinitive;
//		coarsePOS[10] = preposition;
//		coarsePOS[11] = possessive;
//		coarsePOS[12] = symbol;
//		coarsePOS[13] = interjection;
//		coarsePOS[14] = foreign;
//		coarsePOSNames = new String[15];
//		coarsePOSNames[0] = "noun";
//		coarsePOSNames[1] = "nounNonPronoun";
//		coarsePOSNames[2] = "pronoun";
//		coarsePOSNames[3] = "adjective";
//		coarsePOSNames[4] = "verb";
//		coarsePOSNames[5] = "adverb";
//		coarsePOSNames[6] = "conjunction";
//		coarsePOSNames[7] = "punctuation";
//		coarsePOSNames[8] = "determiner";
//		coarsePOSNames[9] = "infinitive";
//		coarsePOSNames[10] = "preposition";
//		coarsePOSNames[11] = "possessive";
//		coarsePOSNames[12] = "symbol";
//		coarsePOSNames[13] = "interjection";
//		coarsePOSNames[14] = "foreign";
	}
	
	public List<String> getCoarsePOS(String pos) {
		List<String> result = new ArrayList<String>();
		for (int i=0; i<coarsePOS.length; ++i) {
			if (coarsePOS[i].contains(pos)) {
				result.add(coarsePOSNames[i]);
			}
		}
		return result;
	}
		
}
