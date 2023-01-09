package edu.berkeley.nlp.mt;

import java.io.File;
import java.io.PrintWriter;

import fig.basic.IOUtils;
import fig.basic.StrUtils;

/**
 * TODO Add tag reading
 * 
 * @author Adam Pauls
 */
public class SentencePairWriter {

	private String basename;

	private String englishExtension;

	private String foreignExtension;

	private String englishTreeExtension;

	private String foreignTreeExtension;

	private String alignmentExtension;

	
	private PrintWriter englishOut;

	private PrintWriter foreignOut;

	private PrintWriter englishTreeOut;

	private PrintWriter foreignTreeOut;

	private PrintWriter alignmentOut;

	boolean wroteAlignment = false;

	boolean wroteForeignTree = false;

	boolean wroteEnglishTree = false;

	/**
	 * Basename should contain whatever path information is relevant, e.g.
	 * /stuff/morestuff/train
	 * 
	 * @param basename
	 * @param englishExtension
	 * @param foreignExtension
	 * @param englishTreeExtension
	 * @param foreignTreeExtension
	 * @param alignmentExtension
	 */
	public SentencePairWriter(String basename, String englishExtension, String foreignExtension, String englishTreeExtension, String foreignTreeExtension,
		String alignmentExtension)
	{
		this.basename = basename;
		this.englishExtension = englishExtension;
		this.foreignExtension = foreignExtension;
		this.englishTreeExtension = englishTreeExtension;
		this.foreignTreeExtension = foreignTreeExtension;
		this.alignmentExtension = alignmentExtension;
	}

	public void open()
	{
		englishOut = IOUtils.openOutHard(basename + "." + englishExtension);
		foreignOut = IOUtils.openOutHard(basename + "." + foreignExtension);
		englishTreeOut = IOUtils.openOutHard(basename + "." + englishTreeExtension);
		foreignTreeOut = IOUtils.openOutHard(basename + "." + foreignTreeExtension);
		alignmentOut = IOUtils.openOutHard(basename + "." + alignmentExtension);

	}

	public void writeSentencePair(SentencePair sp)
	{
		englishOut.println(StrUtils.join(sp.getEnglishWords(), " "));
		foreignOut.println(StrUtils.join(sp.getForeignWords(), " "));
		if (sp.getEnglishTree() != null)
		{
			wroteEnglishTree = true;
			englishTreeOut.println(sp.getEnglishTree().toString());
		}
		if (sp.getForeignTree() != null)
		{
			wroteForeignTree = true;
			foreignTreeOut.println(sp.getForeignTree().toString());
		}
		if (sp.getAlignment() != null)
		{
			wroteAlignment = true;
			alignmentOut.println(sp.getAlignment().outputHard());
		}
	}

	public void close()
	{
		englishOut.close();
		foreignOut.close();
		englishTreeOut.close();
		if (!wroteEnglishTree) new File(basename + "." + englishTreeExtension).delete();
		foreignTreeOut.close();
		if (!wroteForeignTree) new File(basename + "." + foreignTreeExtension).delete();
		alignmentOut.close();
		if (!wroteAlignment) new File(basename + "." + alignmentExtension).delete();
	}

}
