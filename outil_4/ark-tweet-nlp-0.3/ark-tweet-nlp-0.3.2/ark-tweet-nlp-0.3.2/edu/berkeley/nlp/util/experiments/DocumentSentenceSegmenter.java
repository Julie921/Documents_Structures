package edu.berkeley.nlp.util.experiments;

import edu.berkeley.nlp.tokenizer.PTBTokenizer;
import edu.berkeley.nlp.treebank.PennTreebankLanguagePack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class DocumentSentenceSegmenter {

  public List<List<String>> getSentences(File file) {
    StringBuilder data = new StringBuilder();
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        data.append(line + "\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
    return getSentences(data.toString());
  }

  private boolean stillInQuote(List<String> sent) {
    String openQuote = "``";
    String closeQuote = "''";
    int lastOpenIndex = sent.lastIndexOf(openQuote);
    int lastCloseQuote = sent.lastIndexOf(closeQuote);
    if (lastOpenIndex < 0) return false;
    if (lastCloseQuote < 0) return true;
    return lastCloseQuote <= lastOpenIndex;
  }

  public boolean isFinalPunc(String tok) {
    if (puncToks.contains(tok)) {
      return true;
    }
    // Ellipsses
    if (tok.matches("\\.(\\s\\.)+")) {
      return true;
    }
    return false;
  }

  private List<String> puncToks = Arrays.asList(new PennTreebankLanguagePack().sentenceFinalPunctuationWords());


  public List<List<String>> getSentences(List<String> allToks) {
    List<List<String>> sents = new ArrayList<List<String>>();
    List<String> curSent = new ArrayList<String>();
    for (int i=0; i < allToks.size(); ++i) {
      String tok = allToks.get(i);
      curSent.add(tok);
      boolean isEnding = isFinalPunc(tok);
      boolean nextIsClose = i+1 < allToks.size() && allToks.get(i+1).equals("''");
      boolean prevIsEnding = i > 0 && isFinalPunc(allToks.get(i-1));
      boolean isClose = tok.equals("''");
      if ((isEnding && !nextIsClose) || (prevIsEnding && isClose)) {
        sents.add(curSent);
        curSent = new ArrayList<String>();
      }
    }
    if (!curSent.isEmpty()) {
      sents.add(curSent);
    }
    return sents;
  }

  public List<List<String>> getSentences(String docText) {
    PTBTokenizer toker = new PTBTokenizer(new StringReader(docText), false);
    List<String> allToks = toker.tokenize();
    return getSentences(allToks);
  }

  public DocumentSentenceSegmenter() {

  }

  public static void main(String[] args) {
    String s = "`` But we have to attack the deficit . . . '' John Said.";
    List<List<String>> sents = new DocumentSentenceSegmenter().getSentences(s);
    for (List<String> sent : sents) {
      System.out.println(sent);
    }
  }


}
